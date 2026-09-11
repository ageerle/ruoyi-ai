package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.NegativeFeedback;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.dto.NegativeFeedbackCreateReq;
import org.ruoyi.ipd.dto.NegativeFeedbackDecisionReq;
import org.ruoyi.ipd.mapper.NegativeFeedbackMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P3-8.2 负反馈执行单测（BR-INC-10；AC-INC-36b/37/38/39/40）
 *
 * <p>覆盖 6 维度：
 * <ol>
 *   <li>正常路径：REWORK_EXCEEDED → MARKET_PM停发 + RD_PM减半；QUALITY_ACCIDENT 对称；MISSED_MARKET_WINDOW 双PM共同</li>
 *   <li>边界：recoveryMonth 可空；project 无 PM 抛 NF_NOT_PM</li>
 *   <li>异常：triggerType 非法 / month 格式错 / 重复 NF_REENTRY_NOT_ALLOWED / 状态机 NF_STATE_INVALID</li>
 *   <li>权限：decide 由 requireLeaderOrAdmin 守（单测不覆盖 Sa-Token 注解层）；service 不抛 FORBIDDEN</li>
 *   <li>审计：append 调用次数；lift / decide 各 1 次</li>
 *   <li>幂等：同 project + 同 triggerType 已 EXECUTED → 重提报抛 NF_REENTRY_NOT_ALLOWED</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class NegativeFeedbackServiceTest {

    @Mock
    private NegativeFeedbackMapper negativeFeedbackMapper;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private NegativeFeedbackService service;

    private static final Long PROJECT_ID = 1001L;
    private static final Long MARKET_PM = 9110003L;
    private static final Long RD_PM = 9110010L;
    private static final Long LEADER = 9110002L;

    private final IpdActor actor = new IpdActor(MARKET_PM, "TestPM", "MARKET_PM", 1L);
    private final IpdActor leader = new IpdActor(LEADER, "TestLeader", "GROUP_LEADER", 1L);

    @BeforeEach
    void setUp() {
        service = new NegativeFeedbackService(negativeFeedbackMapper, projectMemberMapper,
            auditLogService, notificationService);
    }

    /* ============================================================
     *  AC-INC-36b 正例：需求返工率超标 → MARKET_PM 停发 + RD_PM 减半
     * ============================================================ */
    @Test
    @DisplayName("AC-INC-36b 正例：REWORK_EXCEEDED → MARKET_PM停发 + RD_PM减半 + tierDelta=-0.5 + bonusDisqualify=1")
    void create_reworkExceeded_mainStopsRelatedHalves() {
        when(negativeFeedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                ProjectMember.builder().projectId(PROJECT_ID).personId(MARKET_PM).role("MARKET_PM").build(),
                ProjectMember.builder().projectId(PROJECT_ID).personId(RD_PM).role("RD_PM").build()));

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "近期需求返工率 25%", "2026-09", null);

        NegativeFeedback row = service.create(req, actor);

        // 主责方 MARKET_PM，mainExecution=STOP_ALLOWANCE
        assertThat(row.getMainRole()).isEqualTo("MARKET_PM");
        assertThat(row.getMainExecution()).isEqualTo("STOP_ALLOWANCE");
        assertThat(row.getMainPersonId()).isEqualTo(MARKET_PM);
        // 连带方 RD_PM，relatedExecution=HALVE_ALLOWANCE
        assertThat(row.getRelatedRole()).isEqualTo("RD_PM");
        assertThat(row.getRelatedExecution()).isEqualTo("HALVE_ALLOWANCE");
        assertThat(row.getRelatedPersonId()).isEqualTo(RD_PM);
        // 奖金资格影响（AC-INC-39）
        assertThat(row.getBonusDisqualify()).isEqualTo(1);
        assertThat(row.getTierDelta()).isEqualByComparingTo("-0.50");
        // 状态机初始
        assertThat(row.getStatus()).isEqualTo("DRAFT");
        // 审计 1 次
        verify(auditLogService, times(1)).append(any());
    }

    /* ============================================================
     *  AC-INC-37 正例：质量事故 → RD_PM 停发 + MARKET_PM 减半
     * ============================================================ */
    @Test
    @DisplayName("AC-INC-37 正例：QUALITY_ACCIDENT → RD_PM停发 + MARKET_PM减半")
    void create_qualityAccident_rdMainStops() {
        when(negativeFeedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                ProjectMember.builder().projectId(PROJECT_ID).personId(MARKET_PM).role("MARKET_PM").build(),
                ProjectMember.builder().projectId(PROJECT_ID).personId(RD_PM).role("RD_PM").build()));

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "QUALITY_ACCIDENT", "严重质量事故 5 起", "2026-09", null);

        NegativeFeedback row = service.create(req, actor);

        assertThat(row.getMainRole()).isEqualTo("RD_PM");
        assertThat(row.getMainExecution()).isEqualTo("STOP_ALLOWANCE");
        assertThat(row.getRelatedRole()).isEqualTo("MARKET_PM");
        assertThat(row.getRelatedExecution()).isEqualTo("HALVE_ALLOWANCE");
    }

    /* ============================================================
     *  AC-INC-38 正例：错过市场窗口 → BOTH 双PM共同担责，无连带减半
     * ============================================================ */
    @Test
    @DisplayName("AC-INC-38 正例：MISSED_MARKET_WINDOW → mainRole=BOTH，双PM都 STOP_ALLOWANCE，无 related")
    void create_missedMarketWindow_bothShared() {
        when(negativeFeedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(
                ProjectMember.builder().projectId(PROJECT_ID).personId(MARKET_PM).role("MARKET_PM").build(),
                ProjectMember.builder().projectId(PROJECT_ID).personId(RD_PM).role("RD_PM").build()));

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "MISSED_MARKET_WINDOW", "错过 2026-Q3 上市窗口", "2026-09", null);

        NegativeFeedback row = service.create(req, actor);

        assertThat(row.getMainRole()).isEqualTo("BOTH");
        assertThat(row.getMainExecution()).isEqualTo("STOP_ALLOWANCE");
        assertThat(row.getRelatedRole()).isNull();
        assertThat(row.getRelatedExecution()).isNull();
        assertThat(row.getRelatedPersonId()).isNull();
    }

    /* ============================================================
     *  AC-INC-40 重复不重复扣减
     * ============================================================ */
    @Test
    @DisplayName("AC-INC-40 异常：同 project 同 triggerType 已 EXECUTED → NF_REENTRY_NOT_ALLOWED")
    void create_reentryNotAllowed() {
        when(negativeFeedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "再次", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, actor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("重复");
    }

    /* ============================================================
     *  异常：triggerType 非法
     * ============================================================ */
    @Test
    @DisplayName("异常：triggerType 非法 → NF_TRIGGER_TYPE_INVALID")
    void create_triggerTypeInvalid() {
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "WHATEVER_INVALID", "异常测试", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, actor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("触发情形");
    }

    /* ============================================================
     *  异常：月份格式错
     * ============================================================ */
    @Test
    @DisplayName("异常：triggerMonth 格式错 → NF_MONTH_FORMAT_INVALID")
    void create_monthFormatInvalid() {
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "异常", "2026-9", null);

        assertThatThrownBy(() -> service.create(req, actor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("月份");
    }

    /* ============================================================
     *  异常：项目无 PM
     * ============================================================ */
    @Test
    @DisplayName("异常：项目无 MARKET_PM / RD_PM → NF_NOT_PM")
    void create_projectHasNoPm() {
        when(negativeFeedbackMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMemberMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of()); // 空

        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "无 PM", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, actor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("MARKET_PM");
    }

    /* ============================================================
     *  状态机：submit DRAFT → PENDING_DECISION
     * ============================================================ */
    @Test
    @DisplayName("状态机：submit DRAFT → PENDING_DECISION 落审计")
    void submit_draftToPendingDecision() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(1L).projectId(PROJECT_ID).status("DRAFT")
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM)
            .mainExecution("STOP_ALLOWANCE").relatedRole("RD_PM").relatedPersonId(RD_PM)
            .relatedExecution("HALVE_ALLOWANCE").bonusDisqualify(1)
            .tierDelta(new BigDecimal("-0.50")).triggerMonth("2026-09")
            .triggeredBy(actor.id()).build();
        when(negativeFeedbackMapper.selectById(1L)).thenReturn(existing);

        NegativeFeedback row = service.submit(1L, actor);

        assertThat(row.getStatus()).isEqualTo("PENDING_DECISION");
        verify(auditLogService, times(1)).append(any());
    }

    /* ============================================================
     *  状态机：decide PENDING_DECISION → EXECUTED + 通知双PM
     * ============================================================ */
    @Test
    @DisplayName("状态机：decide APPROVE → EXECUTED + 通知 2 个 PM（AC-INC-40 联动）")
    void decide_approve_pendingToExecuted_notifyBoth() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(1L).projectId(PROJECT_ID).status("PENDING_DECISION")
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM)
            .relatedRole("RD_PM").relatedPersonId(RD_PM)
            .mainExecution("STOP_ALLOWANCE").relatedExecution("HALVE_ALLOWANCE")
            .bonusDisqualify(1).tierDelta(new BigDecimal("-0.50"))
            .triggerMonth("2026-09").triggerType("REWORK_EXCEEDED")
            .triggeredBy(actor.id()).build();
        when(negativeFeedbackMapper.selectById(1L)).thenReturn(existing);

        NegativeFeedback row = service.decide(1L, new NegativeFeedbackDecisionReq("APPROVE", "已确认"),
            leader);

        assertThat(row.getStatus()).isEqualTo("EXECUTED");
        assertThat(row.getDecidedBy()).isEqualTo(LEADER);
        assertThat(row.getDecisionComment()).isEqualTo("已确认");
        verify(auditLogService, times(1)).append(any());
        verify(notificationService, times(2)).publish(anyLong(), anyString(), anyString(),
            eq("negative_feedback"), any(), anyString(), anyString(), anyString());
    }

    /* ============================================================
     *  状态机：decide REJECT → REJECTED，不通知
     * ============================================================ */
    @Test
    @DisplayName("状态机：decide REJECT → REJECTED，不发通知")
    void decide_reject_noNotify() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(1L).projectId(PROJECT_ID).status("PENDING_DECISION")
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM)
            .mainExecution("STOP_ALLOWANCE").relatedRole("RD_PM").relatedPersonId(RD_PM)
            .relatedExecution("HALVE_ALLOWANCE")
            .triggerMonth("2026-09").triggerType("REWORK_EXCEEDED")
            .triggeredBy(actor.id()).build();
        when(negativeFeedbackMapper.selectById(1L)).thenReturn(existing);

        NegativeFeedback row = service.decide(1L, new NegativeFeedbackDecisionReq("REJECT", "证据不足"),
            leader);

        assertThat(row.getStatus()).isEqualTo("REJECTED");
        verify(notificationService, never()).publish(anyLong(), anyString(), anyString(),
            anyString(), any(), anyString(), anyString(), anyString());
    }

    /* ============================================================
     *  状态机：lift EXECUTED → LIFTED + FYI 通知
     * ============================================================ */
    @Test
    @DisplayName("状态机：lift EXECUTED → LIFTED + FYI 通知主责连带")
    void lift_executedToLifted_notifyFyi() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(1L).projectId(PROJECT_ID).status("EXECUTED")
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM)
            .relatedRole("RD_PM").relatedPersonId(RD_PM)
            .mainExecution("STOP_ALLOWANCE").relatedExecution("HALVE_ALLOWANCE")
            .triggerMonth("2026-09").triggerType("REWORK_EXCEEDED")
            .triggeredBy(actor.id()).build();
        when(negativeFeedbackMapper.selectById(1L)).thenReturn(existing);

        NegativeFeedback row = service.lift(1L, new NegativeFeedbackDecisionReq("LIFT", "已整改"), leader);

        assertThat(row.getStatus()).isEqualTo("LIFTED");
        assertThat(row.getLiftedBy()).isEqualTo(LEADER);
        verify(auditLogService, times(1)).append(any());
        verify(notificationService, times(2)).publish(anyLong(), anyString(), anyString(),
            anyString(), any(), anyString(), anyString(), anyString());
    }

    /* ============================================================
     *  状态机：异常状态尝试
     * ============================================================ */
    @Test
    @DisplayName("异常：EXECUTED 状态再 submit → NF_STATE_INVALID")
    void submit_executed_stateInvalid() {
        NegativeFeedback existing = NegativeFeedback.builder()
            .id(1L).projectId(PROJECT_ID).status("EXECUTED").build();
        when(negativeFeedbackMapper.selectById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> service.submit(1L, actor))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("状态机");
    }

    /* ============================================================
     *  静态派生映射
    * ============================================================ */
    @Test
    @DisplayName("静态方法：deriveRoleMapping SPEC_PILE_COPY → RD_PM主 + MARKET_PM连带")
    void deriveRoleMapping_specPileCopy() {
        var map = NegativeFeedbackService.deriveRoleMapping("SPEC_PILE_COPY");
        assertThat(map.get("mainRole")).isEqualTo("RD_PM");
        assertThat(map.get("relatedRole")).isEqualTo("MARKET_PM");
        assertThat(map.get("mainExec")).isEqualTo("STOP_ALLOWANCE");
        assertThat(map.get("relatedExec")).isEqualTo("HALVE_ALLOWANCE");
    }

    @Test
    @DisplayName("静态方法：deriveRoleMapping 未知情形 → NF_TRIGGER_TYPE_INVALID")
    void deriveRoleMapping_unknownThrows() {
        assertThatThrownBy(() -> NegativeFeedbackService.deriveRoleMapping("WHATEVER"))
            .isInstanceOf(IpdBusinessException.class);
    }
}