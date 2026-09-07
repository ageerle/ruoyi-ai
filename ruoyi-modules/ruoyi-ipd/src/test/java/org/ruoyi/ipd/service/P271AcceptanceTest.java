package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.HandoverRecord;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.HandoverMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-7.1 单项目角色移交验收（AC-HAND-01c/01d/03/06；BR-HAND-01/04、BR-USER-06）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>AC-HAND-01c：产品组长代离职人员一键移交 ⇒ 发起即完成（绑定转移 + 审计留痕）</li>
 *   <li>AC-HAND-01d：名下项目全部移交完成 ⇒ 账号 DISABLED + 企微解绑；未全清 ⇒ 不禁用</li>
 *   <li>AC-HAND-03：代办发起即冻结窗口（ACTIVE → FROZEN_PENDING_HANDOVER）</li>
 *   <li>AC-HAND-06：只移交目标角色绑定，另一侧 PM 绑定不动</li>
 *   <li>接手绑定继承 AC-TEAM-11：超项须备案、重复绑定拒绝</li>
 *   <li>状态机：DRAFT → COMPLETED，仅接手人本人可 accept，重复处理拒绝</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P271AcceptanceTest {

    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private HandoverMapper handoverMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private IpdAuthSession ipdAuthSession;

    private ProjectMemberService projectMemberService;
    private HandoverService handoverService;

    private static final IpdActor GROUP_LEAD = new IpdActor(900L, "产品组长", "GROUP_LEADER", 7L);
    private static final IpdActor NEW_PM = new IpdActor(201L, "接任研发PM", "RD_PM", 7L);
    private static final long FROM_ID = 101L;
    private static final long TO_ID = 201L;
    private static final long PROJECT_ID = 12L;

    /** selectCount 语义分类：FOR UPDATE=接手人活跃计数；含 role=幂等预检恒 0；其余=from 全清检查。 */
    private long toActiveCount = 0L;
    private long fromRemainingActive = 0L;

    @BeforeAll
    static void initMybatisMeta() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "P271-handover");
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, HandoverRecord.class);
        TableInfoHelper.initTableInfo(assistant, Person.class);
    }

    @BeforeEach
    void setUp() {
        projectMemberService = new ProjectMemberService(memberMapper, personMapper, projectMapper,
            systemConfigService, auditLogService);
        handoverService = new HandoverService(memberMapper, personMapper, projectMapper, handoverMapper,
            auditLogService, projectMemberService, NoopTransactionManager.INSTANCE, ipdAuthSession);
        // SEC-REV-HANDOVER-01 适配：assertSameGroup 需要 project.mainGroupId 与 leader.groupId 一致
        Project grouped = new Project();
        grouped.setMainGroupId(7L);
        lenient().when(projectMapper.selectById(anyLong())).thenReturn(grouped);
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(systemConfigService.getIntValue("allowance.projectCountThreshold", 3)).thenReturn(3);
        lenient().when(systemConfigService.getIntValue("allowance.L2", -1)).thenReturn(1500);
        lenient().when(personMapper.selectById(FROM_ID)).thenReturn(resignedRdPm());
        lenient().when(personMapper.selectById(TO_ID)).thenReturn(candidateRdPm());
        lenient().when(memberMapper.selectOne(any())).thenReturn(binding(FROM_ID));
        lenient().doReturn(1).when(memberMapper).update(any(), any());
        // disableIfAllCleared 全清禁用：person 侧条件 update（ACTIVE 守卫）放行 1 条
        lenient().when(personMapper.update(any(), any())).thenReturn(1);
        lenient().when(handoverMapper.selectCount(any())).thenReturn(0L);
        lenient().when(handoverMapper.insert(any(HandoverRecord.class))).thenReturn(1);
        lenient().when(handoverMapper.updateById(any(HandoverRecord.class))).thenReturn(1);
        lenient().when(memberMapper.selectCount(any())).thenAnswer(inv -> {
            LambdaQueryWrapper<ProjectMember> w = inv.getArgument(0);
            String seg = w.getSqlSegment();
            if (seg != null && seg.contains("FOR UPDATE")) {
                return toActiveCount;
            }
            if (seg != null && seg.contains("role")) {
                return 0L;
            }
            return fromRemainingActive;
        });
        // disableIfAllCleared（SEC-REV-02 修正版）：selectList(FOR UPDATE) 计数——空=全清，非空=余留绑定
        lenient().when(memberMapper.selectList(any())).thenAnswer(inv -> fromRemainingActive > 0
            ? List.of(binding(FROM_ID)) : List.of());
    }

    // ---------- 造数 ----------

    private Person resignedRdPm() {
        Person p = new Person();
        p.setId(FROM_ID);
        p.setName("已离职研发PM");
        p.setPersonType("RD_PM");
        p.setLevel("L3");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("RESIGNED");
        p.setWecomUserId("wecom-101");
        p.setDelFlag("0");
        return p;
    }

    private Person candidateRdPm() {
        Person p = new Person();
        p.setId(TO_ID);
        p.setName("接任研发PM");
        p.setPersonType("RD_PM");
        p.setLevel("L2");
        p.setAccountStatus("ACTIVE");
        p.setEmploymentStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    private ProjectMember binding(long personId) {
        return ProjectMember.builder()
            .projectId(PROJECT_ID).personId(personId).role("RD_PM")
            .memberType("PRIMARY").lockedLevel("L3").bonusEligible("1")
            .build();
    }

    /** 一键代办：离职人 RD_PM → 接任人。返回完结后的移交记录。 */
    private HandoverRecord oneBehalfHandover() {
        return handoverService.initiateOnBehalf(PROJECT_ID, "RD_PM", TO_ID, "离职移交", null, GROUP_LEAD);
    }

    // ---------- AC-HAND-01c：一键代办 ----------

    @Test
    @DisplayName("AC-HAND-01c：组长代离职人一键移交 ⇒ 发起即完成，绑定转移+双审计")
    void onBehalfOneShotCompletes() {
        HandoverRecord rec = oneBehalfHandover();

        assertThat(rec.getStatus()).isEqualTo("COMPLETED");
        assertThat(rec.getHandoverRole()).isEqualTo("RD_PM");
        assertThat(rec.getFromPersonId()).isEqualTo(FROM_ID);
        assertThat(rec.getCompletedAt()).isNotNull();
        // 接手绑定落库（bindMember 复用）：INSERT 捕获
        ArgumentCaptor<ProjectMember> memberCap = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberMapper).insert(memberCap.capture());
        assertThat(memberCap.getValue().getPersonId()).isEqualTo(TO_ID);
        assertThat(memberCap.getValue().getMemberType()).isEqualTo("PRIMARY");
        assertThat(memberCap.getValue().getLockedLevel()).isEqualTo("L2");
        // 全链审计 4 条：移交创建 → 接手绑定（bindMember 继承）→ 移交接受 → 全清禁用
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(4)).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .containsExactly("HANDOVER_CREATE", "MEMBER_BIND", "HANDOVER_ACCEPT",
                "ACCOUNT_DISABLED_AFTER_HANDOVER");
    }

    @Test
    @DisplayName("AC-HAND-03+01d：代办发起即冻结窗口；全清后 DISABLED+企微解绑（两条 update 的 set 段断言）")
    void freezeWindowThenDisableWithWecomUnbind() {
        oneBehalfHandover();

        ArgumentCaptor<LambdaUpdateWrapper<Person>> updCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(personMapper, times(2)).update(any(), updCap.capture());
        // 第 1 条：冻结窗口（set 段值为占位符，字面值在 paramNameValuePairs）
        LambdaUpdateWrapper<Person> freeze = updCap.getAllValues().get(0);
        assertThat(freeze.getSqlSet()).contains("account_status");
        assertThat(freeze.getParamNameValuePairs().values()).contains("FROZEN_PENDING_HANDOVER");
        // 第 2 条：全清禁用 + 企微解绑（updateById 不落 null，必须显式 set）
        LambdaUpdateWrapper<Person> disable = updCap.getAllValues().get(1);
        assertThat(disable.getSqlSet()).contains("account_status")
            .contains("wecom_user_id").contains("wecom_bound_at");
        assertThat(disable.getParamNameValuePairs().values()).contains("DISABLED");
    }

    @Test
    @DisplayName("AC-HAND-01d 反例：原负责人名下还有活跃项目 ⇒ 不禁用，无禁用审计")
    void notAllClearedKeepsAccount() {
        fromRemainingActive = 1L;

        oneBehalfHandover();

        // 仅 freeze 一次 update，无 disable 分支
        verify(personMapper, times(1)).update(any(), any());
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(3)).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .doesNotContain("ACCOUNT_DISABLED_AFTER_HANDOVER");
    }

    // ---------- AC-HAND-06：角色隔离 ----------

    @Test
    @DisplayName("AC-HAND-06：只移交 RD_PM ⇒ 退出绑定仅此一条（参数值 RD_PM），市场侧不动")
    void onlyTargetRoleMoves() {
        oneBehalfHandover();

        ArgumentCaptor<LambdaUpdateWrapper<ProjectMember>> exitCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(memberMapper, times(1)).update(any(), exitCap.capture());
        LambdaUpdateWrapper<ProjectMember> w = exitCap.getValue();
        assertThat(w.getSqlSegment()).contains("role").contains("exit_date");
        assertThat(w.getParamNameValuePairs().values()).contains("RD_PM");
        assertThat(w.getParamNameValuePairs().values()).contains(PROJECT_ID);
    }

    // ---------- 接手人资格 ----------

    @Test
    @DisplayName("接手人角色不匹配（MARKET_PM 承接 RD_PM）⇒ 拒绝")
    void toRoleMismatchRejected() {
        Person market = candidateRdPm();
        market.setPersonType("MARKET_PM");
        lenient().when(personMapper.selectById(TO_ID)).thenReturn(market);

        assertThatThrownBy(() -> oneBehalfHandover())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("角色不匹配");
        verify(handoverMapper, never()).insert(any(HandoverRecord.class));
    }

    @Test
    @DisplayName("接手人已离职 ⇒ 拒绝承接")
    void toResignedRejected() {
        Person gone = candidateRdPm();
        gone.setEmploymentStatus("RESIGNED");
        lenient().when(personMapper.selectById(TO_ID)).thenReturn(gone);

        assertThatThrownBy(() -> oneBehalfHandover())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("离职");
    }

    @Test
    @DisplayName("同项目同角色已有进行中移交 ⇒ 重复发起拒绝")
    void duplicateDraftRejected() {
        when(handoverMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> oneBehalfHandover())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可重复发起");
    }

    // ---------- AC-TEAM-11 链贯通：超项备案 ----------

    @Test
    @DisplayName("接手人已绑 2 个项目：无备案拒绝；带备案成功且编号落库")
    void overQuotaRequiresApprovalRef() {
        toActiveCount = 2L;

        assertThatThrownBy(() -> oneBehalfHandover())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("备案");

        HandoverRecord rec = handoverService.initiateOnBehalf(
            PROJECT_ID, "RD_PM", TO_ID, "离职移交", "PC-2026-0042", GROUP_LEAD);
        assertThat(rec.getStatus()).isEqualTo("COMPLETED");
        ArgumentCaptor<ProjectMember> memberCap = ArgumentCaptor.forClass(ProjectMember.class);
        // 第一次被拒发生在 bindMember（member 无 insert）；第二次成功恰好 1 条
        verify(memberMapper, times(1)).insert(memberCap.capture());
        assertThat(memberCap.getValue().getApprovalRef()).isEqualTo("PC-2026-0042");
    }

    @Test
    @DisplayName("原绑定已不存在（退出影响行数 0）⇒ 移交中止，不更新移交记录")
    void exitZeroRowsAborts() {
        lenient().doReturn(0).when(memberMapper).update(any(), any());

        assertThatThrownBy(() -> oneBehalfHandover())
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("移交中止");
        verify(handoverMapper, never()).updateById(any(HandoverRecord.class));
    }

    // ---------- 普通流程：DRAFT → accept ----------

    @Test
    @DisplayName("普通移交：本人发起 DRAFT；非接手人 accept 拒；接手人 accept 完成转移")
    void normalFlowDraftThenAcceptByRecipientOnly() {
        IpdActor fromPm = new IpdActor(FROM_ID, "原研发PM", "RD_PM", 7L);
        HandoverRecord rec = handoverService.initiate(PROJECT_ID, "RD_PM", TO_ID, "轮换", fromPm);
        assertThat(rec.getStatus()).isEqualTo("DRAFT");
        assertThat(rec.getFromPersonId()).isEqualTo(FROM_ID);
        when(handoverMapper.selectById(1L)).thenReturn(rec);

        IpdActor stranger = new IpdActor(999L, "无关人员", "MARKET_PM", 8L);
        assertThatThrownBy(() -> handoverService.accept(1L, null, stranger))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅接手人本人");

        HandoverRecord done = handoverService.accept(1L, null, NEW_PM);
        assertThat(done.getStatus()).isEqualTo("COMPLETED");
        verify(memberMapper).insert(any(ProjectMember.class));
    }
}
