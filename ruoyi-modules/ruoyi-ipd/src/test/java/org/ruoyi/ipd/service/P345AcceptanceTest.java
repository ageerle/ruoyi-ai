package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P3-4.5 项目绩效系数分档与可切换取数策略 验收测试
 * AC: AC-INC-22, AC-INC-23, AC-INC-24；BR: BR-INC-07
 */
@Tag("dev")
class P345AcceptanceTest {

    private final ProjectScoreService service = new ProjectScoreService();

    private static BigDecimal score(String s) {
        return new BigDecimal(s);
    }

    // ==================== AC-INC-22: 96 分 ⇒ 1.0 ====================

    @Test
    @DisplayName("AC-INC-22: 综合 96 分 ⇒ 1.0")
    void score96Is1_0() {
        assertThat(service.projectPerformanceCoefficient(score("96"))).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("AC-INC-22: 综合 100 分 ⇒ 1.0")
    void score100Is1_0() {
        assertThat(service.projectPerformanceCoefficient(score("100"))).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("边界: 综合 95 分（区间下端点，含）⇒ 1.0")
    void score95Is1_0() {
        assertThat(service.projectPerformanceCoefficient(score("95"))).isEqualByComparingTo("1.0");
    }

    // ==================== AC-INC-23: 88 分 ⇒ 0.8 ====================

    @Test
    @DisplayName("AC-INC-23: 综合 88 分 ⇒ 0.8")
    void score88Is0_8() {
        assertThat(service.projectPerformanceCoefficient(score("88"))).isEqualByComparingTo("0.8");
    }

    @Test
    @DisplayName("AC-INC-23: 综合 90 分 ⇒ 0.8")
    void score90Is0_8() {
        assertThat(service.projectPerformanceCoefficient(score("90"))).isEqualByComparingTo("0.8");
    }

    @Test
    @DisplayName("边界: 综合 85 分（85/70 区间上端点，含）⇒ 0.8")
    void score85Is0_8() {
        assertThat(service.projectPerformanceCoefficient(score("85"))).isEqualByComparingTo("0.8");
    }

    @Test
    @DisplayName("边界: 综合 84.99 ⇒ 0.6（< 85）")
    void score84_99Is0_6() {
        assertThat(service.projectPerformanceCoefficient(score("84.99"))).isEqualByComparingTo("0.6");
    }

    // ==================== 70/60 区间 ====================

    @Test
    @DisplayName("综合 75 分 ⇒ 0.6")
    void score75Is0_6() {
        assertThat(service.projectPerformanceCoefficient(score("75"))).isEqualByComparingTo("0.6");
    }

    @Test
    @DisplayName("边界: 综合 70 分（含）⇒ 0.6")
    void score70Is0_6() {
        assertThat(service.projectPerformanceCoefficient(score("70"))).isEqualByComparingTo("0.6");
    }

    @Test
    @DisplayName("边界: 综合 69.99 ⇒ 0.3（< 70）")
    void score69_99Is0_3() {
        assertThat(service.projectPerformanceCoefficient(score("69.99"))).isEqualByComparingTo("0.3");
    }

    @Test
    @DisplayName("综合 65 分 ⇒ 0.3")
    void score65Is0_3() {
        assertThat(service.projectPerformanceCoefficient(score("65"))).isEqualByComparingTo("0.3");
    }

    @Test
    @DisplayName("边界: 综合 60 分（含）⇒ 0.3")
    void score60Is0_3() {
        assertThat(service.projectPerformanceCoefficient(score("60"))).isEqualByComparingTo("0.3");
    }

    // ==================== AC-INC-24: 55 分 ⇒ 0（取消奖金资格）====================

    @Test
    @DisplayName("AC-INC-24: 综合 55 分 ⇒ 0（取消奖金资格）")
    void score55Is0() {
        assertThat(service.projectPerformanceCoefficient(score("55"))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("边界: 综合 59.99 ⇒ 0（< 60）")
    void score59_99Is0() {
        assertThat(service.projectPerformanceCoefficient(score("59.99"))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("综合 0 分 ⇒ 0")
    void score0Is0() {
        assertThat(service.projectPerformanceCoefficient(score("0"))).isEqualByComparingTo("0");
    }

    // ==================== 异常路径 ====================

    @Test
    @DisplayName("异常: 综合得分 null 抛 IpdBusinessException")
    void scoreNullThrows() {
        assertThatThrownBy(() -> service.projectPerformanceCoefficient(null))
            .isInstanceOf(org.ruoyi.ipd.common.IpdBusinessException.class);
    }

    // ==================== BR-INC-07: 取数策略 ====================

    @Test
    @DisplayName("BR-INC-07: 合法策略 PROJECT_SCORE / WEIGHTED_AVG / LAST_QUARTER")
    void strategiesValid() {
        ProjectScoreService.validateProjectPerfStrategy("PROJECT_SCORE");
        ProjectScoreService.validateProjectPerfStrategy("WEIGHTED_AVG");
        ProjectScoreService.validateProjectPerfStrategy("LAST_QUARTER");
    }

    @Test
    @DisplayName("BR-INC-07: null 策略走默认（不抛）")
    void strategyNullOk() {
        ProjectScoreService.validateProjectPerfStrategy(null);
    }

    @Test
    @DisplayName("BR-INC-07: 非法策略 X 抛异常")
    void strategyInvalid() {
        assertThatThrownBy(() -> ProjectScoreService.validateProjectPerfStrategy("X"))
            .isInstanceOf(org.ruoyi.ipd.common.IpdBusinessException.class);
    }

    // ==================== AC-INC-22b: 能力等级不得代替项目绩效 ====================

    @Test
    @DisplayName("AC-INC-22b: abilityLevel L1~L5 传入不抛错（仅 log debug）")
    void abilityLevelNotThrows() {
        // 不抛异常即可
        ProjectScoreService.assertAbilityLevelNotUsedAsCoefficient("L1");
        ProjectScoreService.assertAbilityLevelNotUsedAsCoefficient("L5");
        ProjectScoreService.assertAbilityLevelNotUsedAsCoefficient(null);
    }

    @Test
    @DisplayName("AC-INC-22b: 系数计算不依赖 abilityLevel 入参")
    void coefficientIndependentOfAbility() {
        // 仅传 weightedScore，能力等级不影响系数
        BigDecimal coef1 = service.projectPerformanceCoefficient(score("88"));
        BigDecimal coef2 = service.projectPerformanceCoefficient(score("55"));
        assertThat(coef1).isEqualByComparingTo("0.8");
        assertThat(coef2).isEqualByComparingTo("0");
    }

    // ==================== 完整正反例覆盖 ====================

    @Test
    @DisplayName("完整: 100/95/90/85/80/75/70/65/60/55/45 全部正确分档")
    void fullRange() {
        assertThat(service.projectPerformanceCoefficient(score("100"))).isEqualByComparingTo("1.0");
        assertThat(service.projectPerformanceCoefficient(score("95"))).isEqualByComparingTo("1.0");
        assertThat(service.projectPerformanceCoefficient(score("90"))).isEqualByComparingTo("0.8");
        assertThat(service.projectPerformanceCoefficient(score("85"))).isEqualByComparingTo("0.8");
        assertThat(service.projectPerformanceCoefficient(score("84"))).isEqualByComparingTo("0.6");
        assertThat(service.projectPerformanceCoefficient(score("75"))).isEqualByComparingTo("0.6");
        assertThat(service.projectPerformanceCoefficient(score("70"))).isEqualByComparingTo("0.6");
        assertThat(service.projectPerformanceCoefficient(score("69"))).isEqualByComparingTo("0.3");
        assertThat(service.projectPerformanceCoefficient(score("65"))).isEqualByComparingTo("0.3");
        assertThat(service.projectPerformanceCoefficient(score("60"))).isEqualByComparingTo("0.3");
        assertThat(service.projectPerformanceCoefficient(score("55"))).isEqualByComparingTo("0");
        assertThat(service.projectPerformanceCoefficient(score("45"))).isEqualByComparingTo("0");
    }
}
