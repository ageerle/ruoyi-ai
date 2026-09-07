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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateArbitration;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.GateArbitrationMapper;
import org.ruoyi.ipd.mapper.GateReviewObserverMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-5.4 Gate 超时、延期、重发与仲裁轮次验收（AC-GATE-06/07/07b/08/09/10/21，BR-GATE-04/05/06）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>AC-GATE-06：否决后重新发起 ⇒ round+1、期限重算；仅 REJECTED/双弃权态可重发</li>
 *   <li>AC-GATE-07/07b：第 3 轮起双方组长列席通知；第 5 轮起超管介入通知</li>
 *   <li>AC-GATE-08：双签 Gate 一方 3 自然日未签 ⇒ ABSTAIN 行 + 按主导方意见执行 + 审计；
 *       主导方弃权 / 两人均未签 ⇒ ABSTAINED_TIMEOUT 不得无依据放行；单签 Gate 不折算</li>
 *   <li>AC-GATE-09：期限前 1 天未签方收 GATE_SIGN_SOON（publishDaily 去重）</li>
 *   <li>AC-GATE-10：双 PM 分歧 ⇒ 自动邀请组长仲裁；两组不一致 ⇒ 自动升级超管终裁，
 *       终裁写入项目审计日志；两组一致 ⇒ 不升级</li>
 *   <li>AC-GATE-21：超管延长签署期限最多 3 次，第 4 次拒绝；非超管拒绝</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P254AcceptanceTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateReviewMapper reviewMapper;
    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private GateArbitrationMapper arbitrationMapper;
    @Mock
    private GateReviewObserverMapper observerMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private GateReviewService service;

    private static final IpdActor MARKET = new IpdActor(301L, "陈市场", "MARKET_PM", 7L);
    private static final IpdActor RD = new IpdActor(302L, "刘研发", "RD_PM", 7L);
    private static final IpdActor SUPER = new IpdActor(303L, "系统管理员", "SUPER_ADMIN", null);
    private static final IpdActor LEADER_A = new IpdActor(304L, "王组长", "GROUP_LEADER", 7L);
    private static final IpdActor LEADER_B = new IpdActor(305L, "李组长", "GROUP_LEADER", 8L);

    private Gate gate;
    private final List<GateReview> signedRows = new ArrayList<>();
    private final List<GateArbitration> arbitrationRows = new ArrayList<>();

    private static Person person(long id, String name, String type, Long groupId) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPersonType(type);
        p.setGroupId(groupId);
        return p;
    }

    private static ProjectMember member(long personId, String role) {
        ProjectMember m = new ProjectMember();
        m.setPersonId(personId);
        m.setRole(role);
        return m;
    }

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P254-gr"), GateReview.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P254-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P254-ga"), GateArbitration.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P254-pm"), ProjectMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P254-person"), Person.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateReviewService(gateMapper, reviewMapper, memberMapper,
            personMapper, arbitrationMapper, observerMapper, systemConfigService, auditLogService, notificationService);
        gate = new Gate();
        gate.setId(601L);
        gate.setProjectId(11L);
        gate.setGateCode("G1");
        gate.setStatus("PENDING");
        gate.setCurrentRound(1);
        gate.setStartedAt(new Date());
        gate.setSignDueAt(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(3)));
        signedRows.clear();
        arbitrationRows.clear();

        lenient().when(gateMapper.selectById(601L)).thenReturn(gate);
        lenient().when(reviewMapper.insert(any(GateReview.class))).thenAnswer(inv -> {
            signedRows.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(reviewMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(signedRows));
        lenient().when(arbitrationMapper.insert(any(GateArbitration.class))).thenAnswer(inv -> {
            arbitrationRows.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(arbitrationMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(arbitrationRows));
        lenient().when(systemConfigService.getIntValue(eq("gate.signDeadlineDays"), eq(3))).thenReturn(3);
        lenient().when(memberMapper.selectList(any()))
            .thenReturn(List.of(member(301L, "MARKET_PM"), member(302L, "RD_PM")));
        lenient().when(gateMapper.update(any(), any())).thenReturn(1);
        // 扫描类用例的入口查询；返回内存 gate（单 gate 单次扫描）
        lenient().when(gateMapper.selectList(any())).thenReturn(List.of(gate));
    }

    private void signed(String reviewerType, Long reviewerId, String decision) {
        GateReview r = GateReview.builder().gateId(601L).reviewerType(reviewerType)
            .reviewerId(reviewerId).decision(decision).signedAt(new Date()).round(gate.getCurrentRound()).build();
        signedRows.add(r);
    }

    private List<String> auditActions() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(captor.capture());
        return captor.getAllValues().stream().map(AuditLog::getAction).toList();
    }

    private static org.mockito.verification.VerificationMode atLeastOnce() {
        return org.mockito.Mockito.atLeastOnce();
    }

    // ---- AC-GATE-06 否决后重新发起 ----

    @Test
    @DisplayName("AC-GATE-06：REJECTED 后重新发起 ⇒ round+1、期限重算、GATE_REOPEN 审计")
    void reopen_roundIncrements() {
        gate.setStatus("REJECTED");

        service.reopen(601L, MARKET);

        verify(gateMapper).update(any(), any());
        assertThat(auditActions()).contains("GATE_REOPEN");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(captor.capture());
        AuditLog reopenAudit = captor.getAllValues().stream()
            .filter(a -> "GATE_REOPEN".equals(a.getAction())).findFirst().orElseThrow();
        assertThat(reopenAudit.getAfterData()).contains("\"round\":2").contains("signDueAt");
    }

    @Test
    @DisplayName("AC-GATE-06 反例：PENDING/APPROVED 不可重新发起；组长无权重发")
    void reopen_onlyRejectedOrTimeout() {
        assertThatThrownBy(() -> service.reopen(601L, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅被驳回或双弃权超时");
        gate.setStatus("APPROVED");
        assertThatThrownBy(() -> service.reopen(601L, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅被驳回或双弃权超时");
        gate.setStatus("REJECTED");
        assertThatThrownBy(() -> service.reopen(601L, LEADER_A))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅签署双方或超管");
    }

    // ---- AC-GATE-07/07b 轮次列席与介入 ----

    @Test
    @DisplayName("AC-GATE-07：第 3 轮重新发起 ⇒ 双方组长自动列席（通知 + view.observers）")
    void reopen_round3_notifiesGroupLeaders() {
        gate.setStatus("REJECTED");
        gate.setCurrentRound(2);
        List<Person> leaders = List.of(person(304L, "王组长", "GROUP_LEADER", 7L),
            person(305L, "李组长", "GROUP_LEADER", 8L));
        // 调用序：reopen→collectLeaders(1)；view(round≥3)→collectLeaders(2)
        when(personMapper.selectList(any())).thenReturn(leaders, leaders);
        when(personMapper.selectBatchIds(any())).thenReturn(
            List.of(person(301L, "陈市场", "MARKET_PM", 7L), person(302L, "刘研发", "RD_PM", 8L)));

        service.reopen(601L, MARKET);
        gate.setCurrentRound(3); // 模拟 DB 行副作用（reopen 走 wrapper，内存对象需手动同步）

        verify(notificationService, times(2)).publish(anyLong(), eq("GATE_ROUND_OBSERVER"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());
        Map<String, Object> view = service.view(601L, MARKET);
        assertThat(view.get("observers")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).hasSize(2);
    }

    @Test
    @DisplayName("AC-GATE-07b：第 5 轮重新发起 ⇒ 超管收介入通知（组长列席同时保持）")
    void reopen_round5_notifiesSuperAdmin() {
        gate.setStatus("REJECTED");
        gate.setCurrentRound(4);
        List<Person> leaders = List.of(person(304L, "王组长", "GROUP_LEADER", 7L));
        // 调用序：reopen→collectLeaders(1)；round≥5→superAdmins(2)
        when(personMapper.selectList(any())).thenReturn(leaders,
            List.of(person(303L, "系统管理员", "SUPER_ADMIN", null)));
        when(personMapper.selectBatchIds(any())).thenReturn(
            List.of(person(301L, "陈市场", "MARKET_PM", 7L), person(302L, "刘研发", "RD_PM", 8L)));

        service.reopen(601L, MARKET);

        verify(notificationService, times(1)).publish(anyLong(), eq("GATE_ROUND_OBSERVER"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());
        verify(notificationService, times(1)).publish(eq(303L), eq("GATE_ADMIN_INTERVENE"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());
    }

    // ---- AC-GATE-08 超时弃权 ----

    @Test
    @DisplayName("AC-GATE-08：主导方已签 APPROVE，另一方超期 ⇒ ABSTAIN + 按主导方意见执行 + 审计弃权")
    void scanTimeout_abstainFollowsLeadSide() {
        gate.setSignDueAt(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
        signed("MARKET_PM", 301L, "APPROVE");

        int handled = service.scanTimeout(SUPER);

        assertThat(handled).isEqualTo(1);
        assertThat(signedRows).anySatisfy(r -> {
            assertThat(r.getReviewerType()).isEqualTo("RD_PM");
            assertThat(r.getDecision()).isEqualTo("ABSTAIN");
            assertThat(r.getSignedAt()).isNull();
        });
        verify(gateMapper, times(1)).update(any(), any());
        assertThat(auditActions()).contains("GATE_ABSTAIN_TIMEOUT", "GATE_APPROVE");
        verify(notificationService, times(2)).publish(anyLong(), eq("GATE_ABSTAINED"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AC-GATE-08：两人均未签超期 ⇒ 双 ABSTAIN 转 ABSTAINED_TIMEOUT，不得无依据放行")
    void scanTimeout_bothUnsigned_notPassed() {
        gate.setSignDueAt(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));

        service.scanTimeout(SUPER);

        assertThat(signedRows).filteredOn(r -> "ABSTAIN".equals(r.getDecision())).hasSize(2);
        assertThat(auditActions()).contains("GATE_ABSTAIN_TIMEOUT").doesNotContain("GATE_APPROVE");
    }

    @Test
    @DisplayName("AC-GATE-08：主导方超期弃权（非主导方已签）⇒ 无放行依据，ABSTAINED_TIMEOUT")
    void scanTimeout_leadSideAbstains_noBasisToPass() {
        gate.setSignDueAt(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
        signed("RD_PM", 302L, "APPROVE");

        service.scanTimeout(SUPER);

        assertThat(signedRows).anySatisfy(r -> {
            assertThat(r.getReviewerType()).isEqualTo("MARKET_PM");
            assertThat(r.getDecision()).isEqualTo("ABSTAIN");
        });
        assertThat(auditActions()).doesNotContain("GATE_APPROVE");
    }

    @Test
    @DisplayName("AC-GATE-08 反例：未到期不折算；单签 Gate（G3）无弃权折算（三天规则按主导区分）")
    void scanTimeout_notDueAndSingleSign_skipped() {
        int handled = service.scanTimeout(SUPER);
        assertThat(handled).isZero();
        assertThat(signedRows).isEmpty();

        gate.setGateCode("G3");
        gate.setSignDueAt(new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)));
        assertThat(service.scanTimeout(SUPER)).isZero();
        assertThat(signedRows).isEmpty();
    }

    // ---- AC-GATE-09 期限前 1 天提醒 ----

    @Test
    @DisplayName("AC-GATE-09：期限前 24h 内未签方收 GATE_SIGN_SOON；已签方不提醒")
    void scanRemind_notifiesUnsignedOnly() {
        gate.setSignDueAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(12)));
        signed("RD_PM", 302L, "APPROVE");

        int reminded = service.scanRemind(SUPER);

        assertThat(reminded).isEqualTo(1);
        verify(notificationService, times(1)).publishDaily(anyLong(), eq("GATE_SIGN_SOON"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString(), any(Date.class));
    }

    @Test
    @DisplayName("AC-GATE-09 反例：未进提醒窗口（>24h）不提醒")
    void scanRemind_outOfWindow_skipped() {
        gate.setSignDueAt(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)));

        assertThat(service.scanRemind(SUPER)).isZero();
        verify(notificationService, never()).publishDaily(anyLong(), anyString(), anyString(),
            anyString(), anyLong(), anyString(), anyString(), anyString(), any(Date.class));
    }

    // ---- AC-GATE-10 冲突仲裁与超管终裁 ----

    @Test
    @DisplayName("AC-GATE-10 全链：分歧驳回自动开仲裁 → 两组长不一致 → 超管终裁写审计")
    void arbitrate_conflictEscalatesToFinalRuling() {
        List<Person> leaders = List.of(person(304L, "王组长", "GROUP_LEADER", 7L),
            person(305L, "李组长", "GROUP_LEADER", 8L));
        // personMapper.selectList 调用序（collectLeaders×5 → superAdmins×1）：
        // settle→openArbitration(1)；arbitrateA：require(2)+maybe(3)；arbitrateB：require(4)+maybe(5)；escalate→superAdmins(6)
        when(personMapper.selectList(any())).thenReturn(leaders, leaders, leaders, leaders, leaders,
            List.of(person(303L, "系统管理员", "SUPER_ADMIN", null)));
        when(personMapper.selectBatchIds(any())).thenReturn(
            List.of(person(301L, "陈市场", "MARKET_PM", 7L), person(302L, "刘研发", "RD_PM", 8L)));

        service.sign(601L, "APPROVE", "同意立项", MARKET);
        service.sign(601L, "REJECT", "基准值缺失", RD);

        assertThat(gate.getStatus()).isEqualTo("REJECTED");
        assertThat(auditActions()).contains("GATE_ARBITRATION_OPEN");
        verify(notificationService, times(2)).publish(anyLong(), eq("GATE_ARBITRATION_REQUEST"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());

        service.arbitrate(601L, "APPROVE", "支持市场侧", LEADER_A);
        GateArbitration second = service.arbitrate(601L, "REJECT", "支持研发侧", LEADER_B);
        assertThat(second.getDecision()).isEqualTo("REJECT");

        assertThat(auditActions()).contains("GATE_ARBITRATION_ESCALATED");
        verify(notificationService, times(1)).publish(eq(303L), eq("GATE_FINAL_RULING_REQUEST"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());

        GateArbitration ruling = service.finalRuling(601L, "REJECT", "维持驳回，补齐基准值后重发", SUPER);
        assertThat(ruling.getArbitratorType()).isEqualTo("SUPER_ADMIN");
        assertThat(auditActions()).contains("GATE_FINAL_RULING");
        verify(notificationService, times(2)).publish(anyLong(), eq("GATE_FINAL_RULING_RESULT"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AC-GATE-10：两组长仲裁一致 ⇒ SETTLED 并知会双方，不升级超管")
    void arbitrate_consistent_settledWithoutEscalation() {
        List<Person> leaders = List.of(person(304L, "王组长", "GROUP_LEADER", 7L),
            person(305L, "李组长", "GROUP_LEADER", 8L));
        when(personMapper.selectList(any())).thenReturn(leaders, leaders, leaders, leaders);
        when(personMapper.selectBatchIds(any())).thenReturn(
            List.of(person(301L, "陈市场", "MARKET_PM", 7L), person(302L, "刘研发", "RD_PM", 8L)));
        service.sign(601L, "APPROVE", null, MARKET);
        service.sign(601L, "REJECT", "证据不足", RD);

        service.arbitrate(601L, "APPROVE", "同意通过", LEADER_A);
        service.arbitrate(601L, "APPROVE", "同意通过", LEADER_B);

        assertThat(auditActions()).contains("GATE_ARBITRATION_SETTLED").doesNotContain("GATE_ARBITRATION_ESCALATED");
        verify(notificationService, never()).publish(anyLong(), eq("GATE_FINAL_RULING_REQUEST"),
            anyString(), anyString(), anyLong(), anyString(), anyString(), anyString());
        verify(notificationService, times(2)).publish(anyLong(), eq("GATE_ARBITRATION_RESULT"),
            eq("ACTION"), eq("gate"), eq(601L), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AC-GATE-10 防线：无分歧（单 REJECT）不开仲裁；超管在组长未对立前终裁被拒；重复仲裁被拒")
    void arbitrate_guards() {
        service.sign(601L, "REJECT", "直接否决", MARKET);
        assertThat(gate.getStatus()).isEqualTo("REJECTED");
        assertThat(auditActions()).doesNotContain("GATE_ARBITRATION_OPEN");

        assertThatThrownBy(() -> service.arbitrate(601L, "APPROVE", null, LEADER_A))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无双 PM 意见分歧");
        assertThatThrownBy(() -> service.finalRuling(601L, "REJECT", null, SUPER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("无双 PM 意见分歧");

        // 构造分歧后：同组长重复提交被拒（共享签名簿先清场，避免 601 行污染 602 的同轮查重）
        signedRows.clear();
        when(personMapper.selectList(any())).thenReturn(List.of(person(304L, "王组长", "GROUP_LEADER", 7L)));
        when(personMapper.selectBatchIds(any())).thenReturn(
            List.of(person(301L, "陈市场", "MARKET_PM", 7L), person(302L, "刘研发", "RD_PM", 7L)));
        Gate g2 = new Gate();
        g2.setId(602L);
        g2.setProjectId(11L);
        g2.setGateCode("G1");
        g2.setStatus("PENDING");
        g2.setCurrentRound(1);
        g2.setStartedAt(new Date());
        lenient().when(gateMapper.selectById(602L)).thenReturn(g2);
        service.sign(602L, "APPROVE", null, MARKET);
        service.sign(602L, "REJECT", "分歧", RD);
        service.arbitrate(602L, "APPROVE", null, LEADER_A);
        assertThatThrownBy(() -> service.arbitrate(602L, "REJECT", null, LEADER_A))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("不可重复提交");
    }

    // ---- AC-GATE-21 签署期限延长 ----

    @Test
    @DisplayName("AC-GATE-21：超管延长 3 次成功，第 4 次拒绝；审计 GATE_SIGN_EXTEND")
    void extendDeadline_maxThree_fourthRejected() {
        // gateMapper.update 走 wrapper（DB 语义）；测试手动同步内存对象模拟 DB 行副作用
        for (int i = 1; i <= 3; i++) {
            service.extendDeadline(601L, 5, SUPER);
            gate.setSignExtensionCount(i);
            gate.setSignDueAt(new Date(gate.getSignDueAt().getTime() + TimeUnit.DAYS.toMillis(5)));
        }
        verify(gateMapper, times(3)).update(any(), any());
        assertThat(auditActions()).contains("GATE_SIGN_EXTEND");

        assertThatThrownBy(() -> service.extendDeadline(601L, 5, SUPER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("最多延长 3 次");
    }

    @Test
    @DisplayName("AC-GATE-21 反例：非超管拒绝；非在签（REJECTED）Gate 拒绝")
    void extendDeadline_guards() {
        assertThatThrownBy(() -> service.extendDeadline(601L, 5, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅超级管理员");
        gate.setStatus("REJECTED");
        assertThatThrownBy(() -> service.extendDeadline(601L, 5, SUPER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅签署中的 Gate");
    }

    @Test
    @DisplayName("P2-5.2 行为兼容：signDueAt 为空时 dueAt 回退 startedAt+N 天（旧数据）")
    void sign_nullSignDueAt_fallsBackToStartedAt() {
        gate.setSignDueAt(null);

        GateReview row = service.sign(601L, "APPROVE", null, MARKET);

        long expected = gate.getStartedAt().getTime() + TimeUnit.DAYS.toMillis(3);
        assertThat(Math.abs(row.getDueAt().getTime() - expected)).isLessThan(2000L);
    }
}
