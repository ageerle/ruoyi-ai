package org.ruoyi.ipd.service;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.BidController;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.domain.BidResponse;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.BidResponseMapper;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdRolePermissionCatalog;
import org.ruoyi.ipd.security.IpdPermissionException;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * adminAssign 双层授权闭环（commit dc850833 + e1d6f90f 家族）场景级回归。
 *
 * <p>三层校验链：注解层 @SaCheckPermission（OPERATION_BID_INVITATION_ADMIN_ASSIGN，Catalog 仅
 * SUPER_ADMIN 持有）→ controller 方法内 {@code requireAdmin()} 兜底 → service 状态门禁
 * （仅 EXPIRED 且挂起超 30 日、targetPersonId 必填）+ 审计落库 + 通知对等。
 *
 * <p>覆盖（SEC 审查报告 A4c3f5fd 后续要求）：
 * <ul>
 *   <li>非超管（GROUP_LEADER）经 controller 链 → 403 语义（IpdPermissionException httpStatus=403）且 service 零触达</li>
 *   <li>超管经 controller 链通过并以会话身份落库委托</li>
 *   <li>注解码与 Catalog 登记反漂移（防 dc850833 修复过的「码错位」回归）</li>
 *   <li>service 四道门禁：EXPIRED 状态 / 30 日年龄 / targetPersonId 必填 / 不存在 NOT_FOUND</li>
 *   <li>超管放行主路径：SELECTED 落库 + admin_assign 审计（合法 JSON 载荷）</li>
 *   <li>通知对等：中标 BID_WON + 其他 PENDING 应标 BID_LOST（排除中标者与空 rdPmId）</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("adminAssign 双层授权场景回归（403 语义/超管委托/注解码反漂移/服务门禁/审计/通知对等）")
class BidAdminAssignSecurityScenarioTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long AGE_31_DAYS = 31L * 24 * 60 * 60 * 1000;
    private static final long AGE_29_DAYS = 29L * 24 * 60 * 60 * 1000;

    @Mock
    private BidInvitationMapper bidInvitationMapper;
    @Mock
    private BidResponseMapper bidResponseMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BidInvitationService serviceMock;
    @Mock
    private BidResponseService bidResponseService;
    @Mock
    private IpdAuthSession session;
    @Mock
    private IpdAuthService authService;

    private BidInvitationService realService;
    private IpdPermission permission;
    private BidController controller;

    @BeforeEach
    void setUp() {
        realService = new BidInvitationService(bidInvitationMapper, bidResponseMapper, auditLogService, notificationService);
        permission = new IpdPermission(session, authService);
        controller = new BidController(serviceMock, bidResponseService, permission, session);
    }

    /** 以指定角色/组属构造会话身份（SEC-01 边界：角色只来自 IpdAuthSession.currentPerson）。 */
    private void loginAs(String personType, Long id, Long groupId) {
        Person person = Person.builder()
            .id(id).name("U-" + id).personType(personType).groupId(groupId)
            .accountStatus("ACTIVE").employmentStatus("ACTIVE").delFlag("0")
            .build();
        lenient().when(session.currentPerson()).thenReturn(person);
        lenient().when(authService.scopeOf(person)).thenReturn(IpdAuthService.Scope.FULL);
    }

    private BidInvitation expiredInvitation(long ageMillis) {
        BidInvitation inv = new BidInvitation();
        inv.setId(1L);
        inv.setStatus("EXPIRED");
        inv.setTitle("G1 网关招标单");
        inv.setExpireAt(new Date(System.currentTimeMillis() - ageMillis));
        return inv;
    }

    @Test
    @DisplayName("GROUP_LEADER 走 controller 链：403 语义拒绝（IpdPermissionException httpStatus=403）且 service 零触达")
    void adminAssign_groupLeader_viaController_403_andServiceUntouched() {
        loginAs("GROUP_LEADER", 555L, 10L);

        assertThatThrownBy(() -> controller.adminAssign(1L, 200L))
            .isInstanceOf(IpdPermissionException.class)
            .satisfies(e -> {
                IpdPermissionException pe = (IpdPermissionException) e;
                assertThat(pe.getHttpStatus()).as("HTTP 403 语义").isEqualTo(403);
                assertThat(pe.getErrorCode()).as("业务码 FORBIDDEN").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
                assertThat(pe.getMessage()).as("措辞即业务码名").isEqualTo("FORBIDDEN");
            });

        verify(serviceMock, never()).adminAssign(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("MARKET_PM 走 controller 链：同样 403（ADMIN_WRITE 码不向 PM 角色外溢）")
    void adminAssign_marketPm_viaController_403() {
        loginAs("MARKET_PM", 556L, 10L);

        assertThatThrownBy(() -> controller.adminAssign(1L, 200L))
            .isInstanceOf(IpdPermissionException.class)
            .satisfies(e -> assertThat(((IpdPermissionException) e).getHttpStatus()).isEqualTo(403));
        verify(serviceMock, never()).adminAssign(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("超管走 controller 链：requireAdmin 放行并以会话身份委托 service（adminId 不可伪造）")
    void adminAssign_superAdmin_viaController_delegatesWithSessionIdentity() {
        loginAs("SUPER_ADMIN", 999L, 99L);
        BidInvitation inv = expiredInvitation(AGE_31_DAYS);
        when(serviceMock.adminAssign(1L, 200L, 999L)).thenReturn(inv);

        var resp = controller.adminAssign(1L, 200L);

        assertThat(resp.getCode()).as("v1 统一响应 code=0").isZero();
        assertThat(resp.getData()).isSameAs(inv);
        verify(serviceMock).adminAssign(1L, 200L, 999L);
    }

    @Test
    @DisplayName("反漂移：adminAssign 注解码等于 OPERATION_BID_INVITATION_ADMIN_ASSIGN 且 Catalog 仅 SUPER_ADMIN 持有")
    void annotation_pinsAdminAssignCode_registeredOnlyForSuperAdmin() throws Exception {
        java.lang.reflect.Method m = BidController.class.getMethod("adminAssign", Long.class, Long.class);
        SaCheckPermission sa = m.getAnnotation(SaCheckPermission.class);
        assertThat(sa).as("adminAssign 必须保留注解层守卫").isNotNull();
        assertThat(sa.value()).as("注解码即 admin-assign 专属码")
            .containsExactly(IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN);

        assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN))
            .as("SUPER_ADMIN 持有该码").isTrue();
        assertThat(IpdRolePermissionCatalog.has("GROUP_LEADER", IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN))
            .as("GROUP_LEADER 不持有该码").isFalse();
        assertThat(IpdRolePermissionCatalog.has("MARKET_PM", IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN))
            .as("MARKET_PM 不持有该码").isFalse();
        assertThat(IpdRolePermissionCatalog.has("RD_PM", IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN))
            .as("RD_PM 不持有该码").isFalse();
    }

    @Test
    @DisplayName("service 门禁：非 EXPIRED 状态拒绝（STATE_CONFLICT，措辞含『仅适用于 EXPIRED』）且零审计")
    void serviceGate_notExpired_stateConflict() {
        BidInvitation inv = expiredInvitation(AGE_31_DAYS);
        inv.setStatus("OPEN");
        when(bidInvitationMapper.selectByIdForUpdate(1L)).thenReturn(inv);

        assertThatThrownBy(() -> realService.adminAssign(1L, 200L, 999L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅适用于 EXPIRED")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(bidInvitationMapper, never()).updateById(any(BidInvitation.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("service 门禁：EXPIRED 但挂起不足 30 日拒绝（expireAt 锚点）")
    void serviceGate_under30Days_stateConflict() {
        when(bidInvitationMapper.selectByIdForUpdate(1L)).thenReturn(expiredInvitation(AGE_29_DAYS));

        assertThatThrownBy(() -> realService.adminAssign(1L, 200L, 999L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("不足 30 日")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        verify(bidInvitationMapper, never()).updateById(any(BidInvitation.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("service 门禁：targetPersonId 缺失抛 PARAM_INVALID（服务端权威，防空指针中标）")
    void serviceGate_nullTarget_paramInvalid() {
        when(bidInvitationMapper.selectByIdForUpdate(1L)).thenReturn(expiredInvitation(AGE_31_DAYS));

        assertThatThrownBy(() -> realService.adminAssign(1L, null, 999L))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("targetPersonId")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        verify(bidInvitationMapper, never()).updateById(any(BidInvitation.class));
    }

    @Test
    @DisplayName("service 门禁：招标单不存在抛 NOT_FOUND")
    void serviceGate_missingInvitation_notFound() {
        when(bidInvitationMapper.selectByIdForUpdate(404L)).thenReturn(null);

        assertThatThrownBy(() -> realService.adminAssign(404L, 200L, 999L))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("超管放行主路径：EXPIRED 超期单转 SELECTED 落库 + admin_assign 审计（合法 JSON、operatorId 不可伪造）")
    void superAdminPath_selectsAndAudits() throws Exception {
        BidInvitation inv = expiredInvitation(AGE_31_DAYS);
        when(bidInvitationMapper.selectByIdForUpdate(1L)).thenReturn(inv);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);

        BidInvitation result = realService.adminAssign(1L, 200L, 999L);

        assertThat(result.getStatus()).as("状态推进到 SELECTED").isEqualTo("SELECTED");
        assertThat(result.getUpdateTime()).as("审计锚点 updateTime 刷新").isNotNull();
        verify(bidInvitationMapper).updateById(any(BidInvitation.class));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(captor.capture());
        AuditLog audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo("admin_assign");
        assertThat(audit.getEntityType()).isEqualTo("bid_invitation");
        assertThat(audit.getEntityId()).isEqualTo(1L);
        assertThat(audit.getOperatorId()).as("operatorId 来自会话身份").isEqualTo(999L);
        JsonNode node = JSON.readTree(audit.getAfterData());
        assertThat(node.path("targetPersonId").asLong()).as("审计载荷记录中标人").isEqualTo(200L);
        assertThat(audit.getReason()).isEqualTo("G1 网关招标单");
    }

    @Test
    @DisplayName("通知对等：中标者 BID_WON + 其他 PENDING 应标 BID_LOST；中标者本人与空 rdPmId 被排除")
    void notificationParity_winnerWon_losersLost_exclusions() {
        BidInvitation inv = expiredInvitation(AGE_31_DAYS);
        when(bidInvitationMapper.selectByIdForUpdate(1L)).thenReturn(inv);
        when(bidInvitationMapper.updateById(any(BidInvitation.class))).thenReturn(1);
        BidResponse loserA = BidResponse.builder().id(11L).invitationId(1L).rdPmId(301L).status("PENDING").build();
        BidResponse nullPmRow = BidResponse.builder().id(13L).invitationId(1L).rdPmId(null).status("PENDING").build();
        // mapper mock 返回已按 SQL 语义过滤后的行集（status=PENDING 且 rdPmId != target；
        // LambdaQueryWrapper 的 ne 条目在 DB 层生效），Java 侧循环负责跳过空 rdPmId
        when(bidResponseMapper.selectList(any())).thenReturn(List.of(loserA, nullPmRow));

        realService.adminAssign(1L, 200L, 999L);

        verify(notificationService, times(2)).publish(anyLong(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());
        verify(notificationService).publish(eq(200L), eq("BID_WON"), eq("ACTION"), eq("bid_invitation"), eq(1L), anyString(), anyString(), anyString());
        verify(notificationService).publish(eq(301L), eq("BID_LOST"), eq("ACTION"), eq("bid_invitation"), eq(1L), anyString(), anyString(), anyString());
        verify(notificationService, never()).publish(eq(200L), eq("BID_LOST"), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());
    }
}
