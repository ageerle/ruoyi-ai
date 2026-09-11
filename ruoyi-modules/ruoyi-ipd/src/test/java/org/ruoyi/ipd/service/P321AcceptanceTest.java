package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.ProjectScore;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P3-2.1 项目评分 20/40/40 独立提交汇总验收测试
 * AC：AC-KPI-16/16b/16c/18/19/22；BR：BR-KPI-08。
 */
@Tag("dev")
class P321AcceptanceTest {

    private final ProjectScoreService service = new ProjectScoreService();

    // ==================== AC-KPI-16 加权汇总 ====================

    @Test
    @DisplayName("AC-KPI-16 正例: 自评 80 / 市场 90 / 研发 85 ⇒ 80×0.2 + 90×0.4 + 85×0.4 = 86.00")
    void weightedByFormula() {
        BigDecimal result = ProjectScoreService.weighted(
            new BigDecimal("80"), new BigDecimal("90"), new BigDecimal("85"));
        assertThat(result).isEqualByComparingTo("86.00");
    }

    @Test
    @DisplayName("AC-KPI-16 边界: 全 100 ⇒ 100.00")
    void weightedPerfect() {
        BigDecimal result = ProjectScoreService.weighted(
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"));
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("AC-KPI-16 边界: 全 0 ⇒ 0.00")
    void weightedZero() {
        BigDecimal result = ProjectScoreService.weighted(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(result).isEqualByComparingTo("0.00");
    }

    // ==================== AC-KPI-16b 权重校验 ====================

    @Test
    @DisplayName("AC-KPI-16b 正例: 三项权重 [0.2, 0.4, 0.4] 之和 = 1.0")
    void weightsValid() {
        ProjectScoreService.validateWeights(Arrays.asList(
            new BigDecimal("0.20"), new BigDecimal("0.40"), new BigDecimal("0.40")));
    }

    @Test
    @DisplayName("AC-KPI-16b 反例: 三项权重 [0.3, 0.3, 0.3] 之和 = 0.9 抛 IpdBusinessException")
    void weightsSumInvalid() {
        assertThatThrownBy(() -> ProjectScoreService.validateWeights(Arrays.asList(
            new BigDecimal("0.30"), new BigDecimal("0.30"), new BigDecimal("0.30")))
        ).isInstanceOf(IpdBusinessException.class)
         .hasMessageContaining("三项权重之和");
    }

    @Test
    @DisplayName("AC-KPI-16b 反例: 权重数 ≠ 3（如 2 项）抛 IpdBusinessException")
    void weightsSizeInvalid() {
        assertThatThrownBy(() -> ProjectScoreService.validateWeights(Arrays.asList(
            new BigDecimal("0.50"), new BigDecimal("0.50")
        ))).isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("AC-KPI-16b 反例: 单项权重 1.5 抛 IpdBusinessException")
    void weightsSingleOutOfRange() {
        assertThatThrownBy(() -> ProjectScoreService.validateWeights(Arrays.asList(
            new BigDecimal("1.50"), new BigDecimal("-0.10"), new BigDecimal("0.60"))
        )).isInstanceOf(IpdBusinessException.class);
    }

    // ==================== AC-KPI-16c 两 PM 独立打分 ====================

    @Test
    @DisplayName("AC-KPI-16c: 市场 PM 草稿（自评 70/市场组长 80/研发组长 75）⇒ 76.00")
    void marketPmDraft() {
        ProjectScore draft = ProjectScore.builder()
            .projectId(1L).personId(101L).pmRole("MARKET_PM")
            .selfScore(new BigDecimal("70"))
            .marketLeaderScore(new BigDecimal("80"))
            .rdLeaderScore(new BigDecimal("75"))
            .status("DRAFT").build();
        ProjectScore result = service.draft(draft);
        assertThat(result.getWeightedScore()).isEqualByComparingTo("76.00");
    }

    @Test
    @DisplayName("AC-KPI-16c: 研发 PM 草稿（自评 60/市场组长 90/研发组长 70）⇒ 76.00（与市场 PM 同分合理，独立打分）")
    void rdPmDraft() {
        ProjectScore draft = ProjectScore.builder()
            .projectId(1L).personId(202L).pmRole("RD_PM")
            .selfScore(new BigDecimal("60"))
            .marketLeaderScore(new BigDecimal("90"))
            .rdLeaderScore(new BigDecimal("70"))
            .status("DRAFT").build();
        ProjectScore result = service.draft(draft);
        assertThat(result.getWeightedScore()).isEqualByComparingTo("76.00");
    }

    // ==================== AC-KPI-18 / AC-KPI-19: 角色 ====================

    @Test
    @DisplayName("AC-KPI-18/19: pmRole 非法（如 GROUP_LEADER）抛 IpdBusinessException")
    void draftUnknownRole() {
        ProjectScore draft = ProjectScore.builder()
            .projectId(1L).personId(1L).pmRole("GROUP_LEADER")
            .selfScore(new BigDecimal("80"))
            .marketLeaderScore(new BigDecimal("90"))
            .rdLeaderScore(new BigDecimal("85"))
            .status("DRAFT").build();
        assertThatThrownBy(() -> service.draft(draft))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("pmRole");
    }

    @Test
    @DisplayName("AC-KPI-18/19: pmRole=ADMIN（不存在角色）抛 IpdBusinessException")
    void draftAdminRejected() {
        ProjectScore draft = ProjectScore.builder()
            .projectId(1L).personId(1L).pmRole("ADMIN")
            .selfScore(new BigDecimal("80"))
            .marketLeaderScore(new BigDecimal("90"))
            .rdLeaderScore(new BigDecimal("85"))
            .status("DRAFT").build();
        assertThatThrownBy(() -> service.draft(draft))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== 评分范围校验 ====================

    @Test
    @DisplayName("分数范围反例: 自评 120 抛 IpdBusinessException")
    void selfScoreOutOfRange() {
        ProjectScore draft = ProjectScore.builder()
            .projectId(1L).personId(1L).pmRole("MARKET_PM")
            .selfScore(new BigDecimal("120"))
            .marketLeaderScore(new BigDecimal("90"))
            .rdLeaderScore(new BigDecimal("85"))
            .status("DRAFT").build();
        assertThatThrownBy(() -> service.draft(draft))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("分数范围反例: 自评 -1 抛 IpdBusinessException")
    void selfScoreNegative() {
        ProjectScore draft = ProjectScore.builder()
            .projectId(1L).personId(1L).pmRole("MARKET_PM")
            .selfScore(new BigDecimal("-1"))
            .marketLeaderScore(new BigDecimal("90"))
            .rdLeaderScore(new BigDecimal("85"))
            .status("DRAFT").build();
        assertThatThrownBy(() -> service.draft(draft))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== AC-KPI-22 能力等级 vs 绩效不互推导 ====================

    @Test
    @DisplayName("AC-KPI-22: assertNoAbilityInference 接受任意 abilityLevel 不抛")
    void abilityIndependence() {
        ProjectScoreService.assertNoAbilityInference(new BigDecimal("86.00"), "L3");
        ProjectScoreService.assertNoAbilityInference(new BigDecimal("86.00"), null);
    }
}
