package org.ruoyi.ipd.service;

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
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.support.NoopTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SEC-REV-HANDOVER-03：HandoverService.accept 三层守卫 + 业务前置校验专项测试。
 *
 * <p>R-NEW-S-3 收口（2026-09-07）：HandoverService.accept 已加三层守卫——
 * <ol>
 *   <li>{@code IpdIdorGuard.requireAuthenticated}：actor 必填（防 service 内部 null 调用）</li>
 *   <li>{@code IpdIdorGuard.requireProjectTenantMatch}：项目与会话租户一致
 *       （单企业部署恒真；无 Sa-Token 上下文时 currentTenantId() 返回 null，单测场景放行）</li>
 *   <li>{@code assertRecipientEligible}：接手人在建单时即校验过的三条谓词在 accept 时重跑
 *       （防 DRAFT 期内时移失效——接手人离职/被冻结/转岗）</li>
 * </ol>
 *
 * <p>另：accept 自身前置校验——handoverId 必填、记录存在、状态=DRAFT、recipient.id() == toPersonId
 * （不为空即拒，避免被三层守卫绕过后无意义落到 doAccept）。
 *
 * <p>W4-Security 决策 1：跨租户守卫在纯 JVM 单测无 Sa-Token 上下文时 currentTenantId() 返回 null，
 * 守卫恒真放行——本类不重复测试（集成层 / 多租户开启后才有意义）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("S-3 HandoverService.accept 三层守卫 + 业务前置校验专项")
class HandoverAcceptGuardTest {

    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private HandoverMapper handoverMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ProjectMemberService projectMemberService;
    @Mock
    private NotificationService notificationService;

    private HandoverService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new HandoverService(memberMapper, personMapper, projectMapper,
            handoverMapper, auditLogService, projectMemberService, NoopTransactionManager.INSTANCE,
            null /* IpdAuthSession —— accept 不使用 */, notificationService);
    }

    /** 默认 DRAFT 移交记录：id=100，project=1，from=10，to=20，role=MARKET_PM。 */
    private HandoverRecord draftRecord() {
        return HandoverRecord.builder()
            .id(100L)
            .projectId(1L)
            .fromPersonId(10L)
            .toPersonId(20L)
            .handoverRole("MARKET_PM")
            .handoverType("PROJECT")
            .status("DRAFT")
            .build();
    }

    private Person personActive(long id, String type) {
        return Person.builder()
            .id(id)
            .name("P-" + id)
            .personType(type)
            .accountStatus("ACTIVE")
            .employmentStatus("ACTIVE")
            .build();
    }

    /**
     * S3-1：accept(actor=null) ⇒ {@code IpdIdorGuard.requireAuthenticated} ⇒
     * IpdBusinessException(UNAUTHORIZED, "未登录")。守卫先于任何 DB 读 / 业务校验。
     */
    @Test
    @DisplayName("S3-1 accept_nullRecipient_throwsIpdBusinessUnauthorized")
    void accept_nullRecipient_throwsIpdBusinessUnauthorized() {
        assertThatThrownBy(() -> service.accept(100L, null, null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);

        verify(handoverMapper, never()).selectById(anyLong());
        verify(projectMemberService, never()).bindMember(anyLong(), anyLong(), anyString(), any(), any());
        verify(auditLogService, never()).append(any());
    }

    /**
     * S3-2：accept 时 recipient 已 RESIGNED（建单后 15 天内接手人离职）⇒
     * {@code assertRecipientEligible} 抛 ServiceException("接手人已离职/禁用，不可承接")。
     * 防 DRAFT 期内时移失效——建单合法但接手期失效。
     */
    @Test
    @DisplayName("S3-2 accept_resignedRecipient_throwsServiceException")
    void accept_resignedRecipient_throwsServiceException() {
        HandoverRecord rec = draftRecord();
        when(handoverMapper.selectById(100L)).thenReturn(rec);
        when(projectMapper.selectById(1L)).thenReturn(Project.builder().id(1L).name("P1").mainGroupId(10L).build());

        Person resigned = personActive(20L, "MARKET_PM");
        resigned.setEmploymentStatus("RESIGNED");
        when(personMapper.selectById(20L)).thenReturn(resigned);

        IpdActor recipient = new IpdActor(20L, "P-20", "MARKET_PM", 10L);

        assertThatThrownBy(() -> service.accept(100L, null, recipient))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("接手人已离职/禁用");

        verify(projectMemberService, never()).bindMember(anyLong(), anyLong(), anyString(), any(), any());
        verify(handoverMapper, never()).updateById(any(HandoverRecord.class));
        verify(auditLogService, never()).append(any());
    }

    /**
     * S3-3：accept 时 recipient accountStatus=DISABLED ⇒
     * {@code assertRecipientEligible} 抛 ServiceException（"接手人已离职/禁用"），
     * 与 RESIGNED 同口径——禁用即不可承接。
     */
    @Test
    @DisplayName("S3-3 accept_disabledRecipient_throwsServiceException")
    void accept_disabledRecipient_throwsServiceException() {
        HandoverRecord rec = draftRecord();
        when(handoverMapper.selectById(100L)).thenReturn(rec);
        when(projectMapper.selectById(1L)).thenReturn(Project.builder().id(1L).name("P1").mainGroupId(10L).build());

        Person disabled = personActive(20L, "MARKET_PM");
        disabled.setAccountStatus("DISABLED");
        when(personMapper.selectById(20L)).thenReturn(disabled);

        IpdActor recipient = new IpdActor(20L, "P-20", "MARKET_PM", 10L);

        assertThatThrownBy(() -> service.accept(100L, null, recipient))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("接手人已离职/禁用");

        verify(projectMemberService, never()).bindMember(anyLong(), anyLong(), anyString(), any(), any());
    }

    /**
     * S3-4：accept 时 recipient 已转岗（personType=RD_PM 但 handoverRole=MARKET_PM）⇒
     * {@code assertRecipientEligible} 抛 ServiceException("角色不匹配")。
     */
    @Test
    @DisplayName("S3-4 accept_roleMismatch_throwsServiceException")
    void accept_roleMismatch_throwsServiceException() {
        HandoverRecord rec = draftRecord();
        when(handoverMapper.selectById(100L)).thenReturn(rec);
        when(projectMapper.selectById(1L)).thenReturn(Project.builder().id(1L).name("P1").mainGroupId(10L).build());

        // recipient 转岗：personType=RD_PM，但交接记录要求 MARKET_PM
        Person transferred = personActive(20L, "RD_PM");
        when(personMapper.selectById(20L)).thenReturn(transferred);

        IpdActor recipient = new IpdActor(20L, "P-20", "MARKET_PM", 10L);

        assertThatThrownBy(() -> service.accept(100L, null, recipient))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("角色不匹配")
            .hasMessageContaining("MARKET_PM");

        verify(projectMemberService, never()).bindMember(anyLong(), anyLong(), anyString(), any(), any());
    }

    /**
     * S3-5：accept 时 recipient.id() != rec.toPersonId（冒充接手人）⇒
     * accept 内 toPersonId 守卫抛 ServiceException("仅接手人本人可确认移交")。
     * 该守卫位于三层守卫之前——被冒充人即使身份合法也不能 confirm。
     */
    @Test
    @DisplayName("S3-5 accept_recipientIdMismatch_throwsServiceException")
    void accept_recipientIdMismatch_throwsServiceException() {
        HandoverRecord rec = draftRecord();
        when(handoverMapper.selectById(100L)).thenReturn(rec);

        // 冒充：actor.id()=999，但 rec.toPersonId=20
        IpdActor impersonator = new IpdActor(999L, "Other", "MARKET_PM", 10L);

        assertThatThrownBy(() -> service.accept(100L, null, impersonator))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅接手人本人可确认移交");

        // 守卫先于项目查询 / 资格复核 / doAccept
        verify(projectMapper, never()).selectById(anyLong());
        verify(personMapper, never()).selectById(anyLong());
        verify(projectMemberService, never()).bindMember(anyLong(), anyLong(), anyString(), any(), any());
    }

}