package org.ruoyi.ipd.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdRolePermissionCatalog;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.DeleteAuditService;
import org.ruoyi.ipd.service.DeletionRequestService;
import org.ruoyi.ipd.service.StateMachineGuard;
import org.ruoyi.ipd.service.SystemConfigService;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-MED-3: Withdraw 独立权限码 + 防侧信道单元测试（@Tag("dev") 必须）。
 *
 * <p>覆盖核心场景：
 * <ol>
 *   <li>申请不存在 → 统一 404 NOT_FOUND「资源不存在」（防"不存在"侧信道）</li>
 *   <li>非本人申请撤返 → 统一 404 NOT_FOUND（防"非本人"侧信道）</li>
 *   <li>已终态申请撤返 → 统一 404 NOT_FOUND（防"已终态"侧信道）</li>
 *   <li>超 24h 时限撤返 → 统一 404 NOT_FOUND（防"超时限"侧信道）</li>
 *   <li>本人申请 24h 内撤返 → 200 成功 + WITHDRAWN 状态</li>
 * </ol>
 *
 * <p>附加：WITHDRAW 权限码登记校验（catalog 含 4 内部角色）+ Controller 注解切换校验
 * （@SaCheckPermission 引用新码而非 SUBMIT）+ 错误响应 body 字节级一致校验。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("SEC-MED-3: Withdraw 独立权限码 + 防侧信道")
class Sec03WithdrawSideChannelTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long REQUEST_ID = 42L;

    private static final IpdActor ACTOR_REQUESTER =
        new IpdActor(REQUESTER_ID, "市场PM甲", "MARKET_PM", null);
    private static final IpdActor ACTOR_OTHER =
        new IpdActor(OTHER_USER_ID, "无关市场PM", "MARKET_PM", null);
    private static final IpdActor ACTOR_NULL_ID =
        new IpdActor(null, "匿名", "MARKET_PM", null);

    @Mock
    private DeletionRequestMapper deletionRequestMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private DeleteAuditService deleteAuditService;
    @Mock
    private StateMachineGuard stateMachineGuard;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private GateMapper gateMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private PersonMapper personMapper;

    private DeletionRequestService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new DeletionRequestService(
            deletionRequestMapper, systemConfigService, auditLogService, deleteAuditService,
            projectMemberMapper, projectMapper, gateMapper, productMapper, personMapper);
        service.setStateMachineGuard(stateMachineGuard);
    }

    private DeletionRequest savedRequest(Long id, String status, Long requesterId, Date createTime) {
        DeletionRequest r = DeletionRequest.builder()
            .id(id).entityType("projects").entityId(100L).reason("测试删除")
            .requesterId(requesterId).status(status).build();
        r.setCreateTime(createTime);
        return r;
    }

    // ==================== 5 项核心场景 ====================

    @Test
    @DisplayName("1) 申请不存在 → 统一 404 NOT_FOUND「资源不存在」（防不存在侧信道）")
    void withdrawNotExistingReturnsUniformNotFound() {
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(null);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("2) 非本人申请撤返 → 统一 404 NOT_FOUND（防\"非本人\"侧信道）")
    void withdrawOtherOwnerReturnsUniformNotFound() {
        DeletionRequest owned = savedRequest(REQUEST_ID, DeletionRequestService.ST_LEADER_REVIEW,
            OTHER_USER_ID, new Date());
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(owned);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
    }

    @Test
    @DisplayName("3) 已终态申请撤返 → 统一 404 NOT_FOUND（防\"已终态\"侧信道）")
    void withdrawTerminalStateReturnsUniformNotFound() {
        DeletionRequest deleted = savedRequest(REQUEST_ID, DeletionRequestService.ST_DELETED,
            REQUESTER_ID, new Date());
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(deleted);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);

        DeletionRequest rejected = savedRequest(REQUEST_ID, DeletionRequestService.ST_REJECTED,
            REQUESTER_ID, new Date());
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(rejected);
        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);

        DeletionRequest alreadyWithdrawn = savedRequest(REQUEST_ID, DeletionRequestService.ST_WITHDRAWN,
            REQUESTER_ID, new Date());
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(alreadyWithdrawn);
        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("4) 超 24h 时限撤返 → 统一 404 NOT_FOUND（防\"超时限\"侧信道）")
    void withdrawOverDeadlineReturnsUniformNotFound() {
        Date old = new Date(System.currentTimeMillis() - 25 * 3600_000L);
        DeletionRequest oldRequest = savedRequest(REQUEST_ID, DeletionRequestService.ST_LEADER_REVIEW,
            REQUESTER_ID, old);
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(oldRequest);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
    }

    @Test
    @DisplayName("5) 本人申请 24h 内撤返 → 200 成功 + WITHDRAWN 状态 + 审计写入")
    void withdrawOwnerWithinDeadlineSucceeds() {
        DeletionRequest recent = savedRequest(REQUEST_ID, DeletionRequestService.ST_LEADER_REVIEW,
            REQUESTER_ID, new Date());
        when(deletionRequestMapper.selectById(REQUEST_ID)).thenReturn(recent);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        DeletionRequest after = service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, REQUEST_ID);

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_WITHDRAWN);
        verify(deletionRequestMapper).updateById(any(DeletionRequest.class));
        verify(auditLogService).append(any());
    }

    // ==================== 错误响应 body 字节级一致 ====================

    @Test
    @DisplayName("6) 错误响应 body 字节级相同：4 类失败路径抛出的 NOT_FOUND 完全一致")
    void allFailurePathsProduceIdenticalNotFoundException() {
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        when(deletionRequestMapper.selectById(1001L)).thenReturn(null);
        IpdBusinessException exNotExist = capture(() ->
            service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, 1001L));

        DeletionRequest notOwned = savedRequest(1002L, DeletionRequestService.ST_LEADER_REVIEW,
            OTHER_USER_ID, new Date());
        when(deletionRequestMapper.selectById(1002L)).thenReturn(notOwned);
        IpdBusinessException exNotOwner = capture(() ->
            service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, 1002L));

        DeletionRequest deleted = savedRequest(1003L, DeletionRequestService.ST_DELETED,
            REQUESTER_ID, new Date());
        when(deletionRequestMapper.selectById(1003L)).thenReturn(deleted);
        IpdBusinessException exTerminal = capture(() ->
            service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, 1003L));

        Date old = new Date(System.currentTimeMillis() - 25 * 3600_000L);
        DeletionRequest overdue = savedRequest(1004L, DeletionRequestService.ST_LEADER_REVIEW,
            REQUESTER_ID, old);
        when(deletionRequestMapper.selectById(1004L)).thenReturn(overdue);
        IpdBusinessException exOverdue = capture(() ->
            service.withdrawIfExistsOrNotFound(ACTOR_REQUESTER, 1004L));

        for (IpdBusinessException ex : new IpdBusinessException[] {
            exNotExist, exNotOwner, exTerminal, exOverdue }) {
            assertThat(ex.getErrorCode()).isEqualTo(ApiV1ErrorCode.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("资源不存在");
            assertThat(ex.getErrorCode().getHttpStatus()).isEqualTo(404);
        }
    }

    @Test
    @DisplayName("7) actor.id=null → UNAUTHORIZED（前置守卫，独立于 NOT_FOUND 侧信道）")
    void nullActorIdReturnsUnauthorized() {
        // actor 校验在 selectById 之前 fail-fast，故不需 stub mapper/config
        assertThatThrownBy(() -> service.withdrawIfExistsOrNotFound(ACTOR_NULL_ID, REQUEST_ID))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        verify(deletionRequestMapper, never()).selectById(any());
    }

    // ==================== 权限码 + Controller 注解校验 ====================

    @Test
    @DisplayName("8) WITHDRAW 权限码常量已定义且与 SUBMIT 解耦")
    void withdrawPermissionCodeDefined() {
        assertThat(IpdPermissionCode.OPERATION_DELETION_REQUEST_WITHDRAW)
            .isEqualTo("ipd:deletion-request:withdraw");
        assertThat(IpdPermissionCode.OPERATION_DELETION_REQUEST_WITHDRAW)
            .isNotEqualTo(IpdPermissionCode.OPERATION_DELETION_REQUEST_SUBMIT);
    }

    @Test
    @DisplayName("9) 4 个内部角色都登记 WITHDRAW 权限（申请人角色范围）")
    void withdrawRegisteredForAllInternalRoles() {
        Set<String> expectedRoles = Set.of("SUPER_ADMIN", "GROUP_LEADER", "MARKET_PM", "RD_PM");
        Set<String> registeredRoles = new HashSet<>();
        for (String role : expectedRoles) {
            if (IpdRolePermissionCatalog.has(role, IpdPermissionCode.OPERATION_DELETION_REQUEST_WITHDRAW)) {
                registeredRoles.add(role);
            }
        }
        assertThat(registeredRoles)
            .as("WITHDRAW 权限须对所有可发起删除申请的内部角色登记")
            .containsExactlyInAnyOrderElementsOf(expectedRoles);
    }

    @Test
    @DisplayName("10) Controller withdraw 注解引用新 WITHDRAW 码而非 SUBMIT")
    void controllerWithdrawAnnotationUsesWithdrawCode() throws Exception {
        Method withdraw = DeletionRequestController.class.getDeclaredMethod(
            "withdraw", Long.class);

        cn.dev33.satoken.annotation.SaCheckPermission annotation =
            withdraw.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class);
        assertThat(annotation)
            .as("withdraw 方法必须保留 @SaCheckPermission 注解")
            .isNotNull();
        // Sa-Token 的 @SaCheckPermission.value() 返回 String[]，取首元素比对
        assertThat(annotation.value())
            .as("withdraw 注解必须用独立 WITHDRAW 权限码，不能复用 SUBMIT")
            .containsExactly(IpdPermissionCode.OPERATION_DELETION_REQUEST_WITHDRAW);
        assertThat(annotation.value())
            .doesNotContain(IpdPermissionCode.OPERATION_DELETION_REQUEST_SUBMIT);
    }

    private static IpdBusinessException capture(RunnableWithThrow r) {
        try {
            r.run();
        } catch (IpdBusinessException ex) {
            return ex;
        } catch (Throwable t) {
            throw new AssertionError("expected IpdBusinessException but got " + t.getClass(), t);
        }
        throw new AssertionError("expected IpdBusinessException but no exception thrown");
    }

    @FunctionalInterface
    private interface RunnableWithThrow {
        void run() throws Exception;
    }
}
