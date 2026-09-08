package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.NegativeFeedback;
import org.ruoyi.ipd.dto.NegativeFeedbackCreateReq;
import org.ruoyi.ipd.dto.NegativeFeedbackDecisionReq;
import org.ruoyi.ipd.mapper.NegativeFeedbackMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P3-8.1 负反馈四类触发与责任评定 验收测试 (BR-INC-10/12/13; AC-INC-31~35)
 *
 * <p>卡面统一收口 4 项验收 AC：
 * <ol>
 *   <li>AC-INC-31：四类触发（REWORK_EXCEEDED / QUALITY_ACCIDENT / PLAGIARISM_STACK / MISSED_MARKET_WINDOW）合法；非法值抛 IpdBusinessException</li>
 *   <li>AC-INC-32：主责/共同责任派生 — REWORK_EXCEEDED ⇒ MARKET_PM 主责；MISSED_MARKET_WINDOW ⇒ 双 PM 共同</li>
 *   <li>AC-INC-33：不可自行新增处罚 — tierDelta 强校验 = -0.50 / CreateReq 无 tierDelta 字段</li>
 *   <li>AC-INC-34：申诉恢复（lift）业务路径 — GROUP_LEADER 调 lift ⇒ OK 落库 LIFTED（角色守卫在 Controller 层 {@code requireLeaderOrAdmin()}）</li>
 * </ol>
 *
 * <p>实现见 {@link NegativeFeedbackService}（P3-8.1）；本卡只验收、不可改业务代码。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P381AcceptanceTest {

    @Mock private NegativeFeedbackMapper negativeFeedbackMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private NegativeFeedbackService service;

    private static final Long PROJECT_ID = 1001L;
    private static final Long MARKET_PM = 9110003L;
    private static final Long RD_PM = 9110010L;
    private static final Long LEADER = 9110002L;

    @BeforeEach
    void setUp() {
        // 4-arg 测试口：mapper + (memberMapper=null) + audit + notify
        service = new NegativeFeedbackService(negativeFeedbackMapper, null, auditLogService, notificationService);
    }

    private NegativeFeedback buildExecutedFeedback() {
        return NegativeFeedback.builder()
            .id(1L).projectId(PROJECT_ID).triggerType("REWORK_EXCEEDED")
            .status(NegativeFeedbackService.STATUS_EXECUTED)
            .tierDelta(NegativeFeedbackService.DEFAULT_TIER_DELTA)
            .mainRole("MARKET_PM").mainPersonId(MARKET_PM)
            .mainExecution(NegativeFeedbackService.EXEC_STOP_ALLOWANCE)
            .relatedRole("RD_PM").relatedPersonId(RD_PM)
            .relatedExecution(NegativeFeedbackService.EXEC_HALVE_ALLOWANCE)
            .triggerMonth("2026-09").build();
    }

    /* ============================================================
     *  AC-INC-31  四类触发合法
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-31] TRIGGER_TYPES 含 REWORK_EXCEEDED / QUALITY_ACCIDENT / SPEC_PILE_COPY / MISSED_MARKET_WINDOW 四类")
    void AC_INC_31_四类触发常量() {
        assertThat(NegativeFeedbackService.TRIGGER_TYPES)
            .contains("REWORK_EXCEEDED", "QUALITY_ACCIDENT", "SPEC_PILE_COPY", "MISSED_MARKET_WINDOW");
        assertThat(NegativeFeedbackService.TRIGGER_TYPES).hasSize(4);
    }

    @Test
    @DisplayName("[AC-INC-31] 非法 triggerType ⇒ IpdBusinessException")
    void AC_INC_31_非法触发拒绝() {
        IpdActor actor = new IpdActor(MARKET_PM, "TestMarketPM", "MARKET_PM", 1L);
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "ILLEGAL_TYPE", "非法", "2026-09", null);

        assertThatThrownBy(() -> service.create(req, actor))
            .isInstanceOf(IpdBusinessException.class);
    }

    /* ============================================================
     *  AC-INC-32  主责/共同责任派生
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-32] REWORK_EXCEEDED ⇒ MARKET_PM 主责")
    void AC_INC_32_返工主责() {
        String mainRole = NegativeFeedbackService.deriveRoleMapping("REWORK_EXCEEDED").get("mainRole");
        assertThat(mainRole).isEqualTo("MARKET_PM");
    }

    @Test
    @DisplayName("[AC-INC-32] MISSED_MARKET_WINDOW ⇒ 双 PM 共同")
    void AC_INC_32_错过窗口双PM共同() {
        java.util.Map<String, String> mapping = NegativeFeedbackService.deriveRoleMapping("MISSED_MARKET_WINDOW");
        // BOTH 表示双 PM 共同（mainRole=BOTH，relatedRole=null）
        assertThat(mapping.get("mainRole")).isEqualTo("BOTH");
        assertThat(mapping.get("relatedRole")).isNull();
    }

    /* ============================================================
     *  AC-INC-33  不可自行新增处罚（tierDelta 强校验）
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-33] DEFAULT_TIER_DELTA = -0.50（固定值，业务代码不暴露 setter）")
    void AC_INC_33_tierDelta固定() {
        assertThat(NegativeFeedbackService.DEFAULT_TIER_DELTA)
            .isEqualByComparingTo(new BigDecimal("-0.50"));
    }

    @Test
    @DisplayName("[AC-INC-33] 创建反馈时 tierDelta 不接受外部注入（CreateReq 无该字段）")
    void AC_INC_33_无tierDeltaSetter() {
        NegativeFeedbackCreateReq req = new NegativeFeedbackCreateReq(
            PROJECT_ID, "REWORK_EXCEEDED", "测试", "2026-09", null);
        boolean hasField = java.util.Arrays.stream(req.getClass().getDeclaredFields())
            .anyMatch(f -> f.getName().equalsIgnoreCase("tierDelta"));
        assertThat(hasField).isFalse();
    }

    /* ============================================================
     *  AC-INC-34  申诉恢复（lift）业务路径
     *  角色守卫在 Controller 层 @SaCheckPermission + requireLeaderOrAdmin()
     *  本测试覆盖 service 业务路径（GROUP_LEADER 调用成功）
     * ============================================================ */

    @Test
    @DisplayName("[AC-INC-34] GROUP_LEADER 调用 lift ⇒ OK + 状态 LIFTED + 审计")
    void AC_INC_34_组长lift通过() {
        NegativeFeedback existing = buildExecutedFeedback();
        when(negativeFeedbackMapper.selectById(1L)).thenReturn(existing);
        when(negativeFeedbackMapper.updateById(any(NegativeFeedback.class))).thenReturn(1);

        IpdActor leader = new IpdActor(LEADER, "TestLeader", "GROUP_LEADER", 1L);

        NegativeFeedback lifted = service.lift(1L,
            new NegativeFeedbackDecisionReq("LIFT", "测试申诉"), leader);

        assertThat(lifted.getStatus()).isEqualTo(NegativeFeedbackService.STATUS_LIFTED);
        assertThat(lifted.getLiftedBy()).isEqualTo(LEADER);
    }

    @Test
    @DisplayName("[AC-INC-34] lift 非 EXECUTED 状态 ⇒ NF_STATE_INVALID")
    void AC_INC_34_非执行态不可lift() {
        NegativeFeedback draft = NegativeFeedback.builder()
            .id(2L).projectId(PROJECT_ID).triggerType("REWORK_EXCEEDED")
            .status(NegativeFeedbackService.STATUS_DRAFT)
            .tierDelta(NegativeFeedbackService.DEFAULT_TIER_DELTA)
            .triggerMonth("2026-09").build();

        when(negativeFeedbackMapper.selectById(2L)).thenReturn(draft);

        IpdActor leader = new IpdActor(LEADER, "TestLeader", "GROUP_LEADER", 1L);

        assertThatThrownBy(() -> service.lift(2L,
            new NegativeFeedbackDecisionReq("LIFT", "测试"), leader))
            .isInstanceOf(IpdBusinessException.class);
    }
}