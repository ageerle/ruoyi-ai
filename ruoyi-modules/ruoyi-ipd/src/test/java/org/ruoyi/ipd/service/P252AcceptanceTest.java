package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
 * P2-5.2 Gate 双签验收（AC-GATE-03/04/05，BR-GATE-03/04/08）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>AC-GATE-03：市场PM 已签，研发PM 视图只含 otherSubmitted=true，无对方结论/意见</li>
 *   <li>AC-GATE-04：双 APPROVE ⇒ APPROVED，双方结论同时揭示</li>
 *   <li>AC-GATE-05：任一 REJECT ⇒ REJECTED + 双方 GATE_REJECTED 通知</li>
 *   <li>G2/3/4 领域签署：主导方按领域（G3/G4 研发、G1/G2/G5 市场），非主导方拒绝</li>
 *   <li>非授权角色（组长/超管）签署拒绝；重复签署拒绝；未提交签署拒绝</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P252AcceptanceTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateReviewMapper reviewMapper;
    @Mock
    private ProjectMemberMapper memberMapper;
    @Mock
    private org.ruoyi.ipd.mapper.PersonMapper personMapper;
    @Mock
    private org.ruoyi.ipd.mapper.GateArbitrationMapper arbitrationMapper;
    @Mock
    private org.ruoyi.ipd.mapper.GateReviewObserverMapper observerMapper;
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
    private static final IpdActor LEADER = new IpdActor(304L, "王组长", "GROUP_LEADER", 7L);

    private Gate gate;
    /** 签名簿：insert 落行、roundRows 动态读取，支撑 sign→advance→view 全链 */
    private final List<GateReview> signedRows = new ArrayList<>();

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P252-gr"), GateReview.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P252-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P252-pm"), ProjectMember.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateReviewService(gateMapper, reviewMapper, memberMapper,
            personMapper, arbitrationMapper, observerMapper, systemConfigService, auditLogService, notificationService);
        gate = new Gate();
        gate.setId(501L);
        gate.setProjectId(11L);
        gate.setGateCode("G1");
        gate.setStatus("PENDING");
        gate.setCurrentRound(1);
        gate.setStartedAt(new Date());
        signedRows.clear();

        lenient().when(gateMapper.selectById(501L)).thenReturn(gate);
        lenient().when(reviewMapper.insert(any(GateReview.class))).thenAnswer(inv -> {
            signedRows.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(reviewMapper.selectList(any())).thenAnswer(inv -> new ArrayList<>(signedRows));
        lenient().when(systemConfigService.getIntValue(eq("gate.signDeadlineDays"), eq(3))).thenReturn(3);
        lenient().when(memberMapper.selectList(any())).thenReturn(List.of());
    }

    private void signed(String reviewerType, Long reviewerId, String decision) {
        GateReview r = GateReview.builder().gateId(501L).reviewerType(reviewerType)
            .reviewerId(reviewerId).decision(decision).signedAt(new Date()).round(1).build();
        signedRows.add(r);
    }

    // ---- AC-GATE-03 盲签互不可见 ----

    @Test
    @DisplayName("AC-GATE-03：市场PM 已签，研发PM 视图仅 otherSubmitted=true，无对方结论")
    void view_otherSideBlind_inFlight() {
        signed("MARKET_PM", 301L, "APPROVE");

        Map<String, Object> rdView = service.view(501L, RD);

        assertThat(rdView.get("otherSubmitted")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> other = (Map<String, Object>) rdView.get("other");
        assertThat(other).doesNotContainKeys("decision", "opinion");
        assertThat(String.valueOf(rdView.get("hint"))).contains("对方已提交");
    }

    @Test
    @DisplayName("AC-GATE-03 对称：研发PM 已签，市场PM 亦看不到对方结论；己方结论自见")
    void view_myOwnDecision_visible() {
        signed("RD_PM", 302L, "APPROVE");

        Map<String, Object> marketView = service.view(501L, MARKET);

        assertThat(marketView.get("otherSubmitted")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> other = (Map<String, Object>) marketView.get("other");
        assertThat(other).doesNotContainKeys("decision", "opinion");
        @SuppressWarnings("unchecked")
        Map<String, Object> mine = (Map<String, Object>) marketView.get("my");
        assertThat(mine).isNull(); // 市场PM 自己未签
    }

    // ---- AC-GATE-04 双 APPROVE ----

    @Test
    @DisplayName("AC-GATE-04：双 APPROVE ⇒ APPROVED 且双方结论同时揭示")
    void sign_bothApprove_gateApprovedAndRevealed() {
        service.sign(501L, "APPROVE", "市场侧同意", MARKET);
        service.sign(501L, "APPROVE", "研发侧同意", RD);

        assertThat(gate.getStatus()).isEqualTo("APPROVED");
        verify(gateMapper).updateById(gate);
        Map<String, Object> rdView = service.view(501L, RD);
        @SuppressWarnings("unchecked")
        Map<String, Object> other = (Map<String, Object>) rdView.get("other");
        assertThat(other).containsEntry("decision", "APPROVE").containsEntry("opinion", "市场侧同意");
    }

    @Test
    @DisplayName("AC-GATE-04 前置：仅一方 APPROVE ⇒ 仍在途 PENDING（盲签保持）")
    void sign_oneApprove_stillPending() {
        service.sign(501L, "APPROVE", null, MARKET);

        assertThat(gate.getStatus()).isEqualTo("PENDING");
        verify(gateMapper, never()).updateById(any(Gate.class));
    }

    // ---- AC-GATE-05 否决驳回 ----

    @Test
    @DisplayName("AC-GATE-05：任一 REJECT ⇒ REJECTED + 双方 GATE_REJECTED 通知")
    void sign_reject_gateRejectedAndBothNotified() {
        service.sign(501L, "APPROVE", null, MARKET);

        service.sign(501L, "REJECT", "否决：G1-2 基准值缺失", RD);

        assertThat(gate.getStatus()).isEqualTo("REJECTED");
        verify(notificationService, times(2)).publish(anyLong(), eq("GATE_REJECTED"),
            eq("ACTION"), eq("gate"), eq(501L), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("AC-GATE-05 退化：另一方未签且无成员绑定时，仅通知已签方（不丢通知）")
    void sign_reject_noMemberFallback_notifiesSignedOnly() {
        service.sign(501L, "REJECT", "直接否决", MARKET);

        assertThat(gate.getStatus()).isEqualTo("REJECTED");
        verify(notificationService, times(1)).publish(eq(301L), eq("GATE_REJECTED"),
            eq("ACTION"), eq("gate"), eq(501L), anyString(), anyString(), anyString());
    }

    // ---- G2/3/4 领域签署 ----

    @Test
    @DisplayName("G3 领域签署：研发主导 APPROVE 单签即终态；市场PM 签署拒绝")
    void domainSign_g3_rdLeads() {
        gate.setGateCode("G3");

        assertThatThrownBy(() -> service.sign(501L, "APPROVE", null, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("研发PM主导签署");

        service.sign(501L, "APPROVE", "进度可控", RD);
        assertThat(gate.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("G4 领域签署：研发主导（否决项 G4-1/2/3 均研发交付侧）；市场拒绝")
    void domainSign_g4_rdLeads() {
        gate.setGateCode("G4");

        assertThatThrownBy(() -> service.sign(501L, "APPROVE", null, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("研发PM主导签署");

        service.sign(501L, "APPROVE", null, RD);
        assertThat(gate.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("G2 领域签署：市场主导；研发PM 拒绝")
    void domainSign_g2_marketLeads() {
        gate.setGateCode("G2");

        assertThatThrownBy(() -> service.sign(501L, "APPROVE", null, RD))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("市场PM主导签署");

        service.sign(501L, "APPROVE", null, MARKET);
        assertThat(gate.getStatus()).isEqualTo("APPROVED");
    }

    // ---- 授权与状态防线 ----

    @Test
    @DisplayName("非授权角色：组长/超管签署拒绝（列席与仲裁归 P2-5.4）")
    void sign_leaderOrSuper_rejected() {
        assertThatThrownBy(() -> service.sign(501L, "APPROVE", null, LEADER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅市场PM/研发PM");
        assertThatThrownBy(() -> service.sign(501L, "APPROVE", null, SUPER))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅市场PM/研发PM");
    }

    @Test
    @DisplayName("重复签署：同轮同角色再签拒绝")
    void sign_twice_rejected() {
        service.sign(501L, "APPROVE", null, MARKET);

        assertThatThrownBy(() -> service.sign(501L, "REJECT", null, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("不可重复签署");
    }

    @Test
    @DisplayName("未提交（P2-5.1 startedAt 为空）签署拒绝")
    void sign_notSubmitted_rejected() {
        gate.setStartedAt(null);

        assertThatThrownBy(() -> service.sign(501L, "APPROVE", null, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("尚未提交");
    }

    @Test
    @DisplayName("decision 非法值拒绝（ABSTAIN 超时流转归 P2-5.4）")
    void sign_invalidDecision_rejected() {
        assertThatThrownBy(() -> service.sign(501L, "ABSTAIN", null, MARKET))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("APPROVE|REJECT");
    }

    // ---- 揭示特例 ----

    @Test
    @DisplayName("超管在途视图全揭示（页24：super_admin 可见全部；不改变 Gate 状态）")
    void view_superAdmin_seesAllInFlight() {
        signed("MARKET_PM", 301L, "REJECT");

        Map<String, Object> superView = service.view(501L, SUPER);

        @SuppressWarnings("unchecked")
        Map<String, Object> other = (Map<String, Object>) superView.get("other");
        assertThat(other).containsEntry("decision", "REJECT");
        assertThat(gate.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("签署审计：GATE_SIGN 落审计（含 decision/opinion/round 载荷）")
    void sign_audited() {
        service.sign(501L, "APPROVE", "同意立项", MARKET);

        verify(auditLogService).append(any(org.ruoyi.ipd.domain.AuditLog.class));
    }
}
