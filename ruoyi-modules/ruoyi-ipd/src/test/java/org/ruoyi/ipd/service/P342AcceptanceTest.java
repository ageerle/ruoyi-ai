package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P3-4.2 奖金池 5% 基数和 S/A/B 系数可配置 验收测试
 * AC: AC-INC-16；BR: BR-INC-04, BR-INC-05
 */
@Tag("dev")
class P342AcceptanceTest {

    private final BonusPoolService service = new BonusPoolService(null);

    private static BigDecimal amt(String s) {
        return new BigDecimal(s);
    }

    // ==================== AC-INC-16: 奖金池基数（实际回款 × 5%，2026-09-06 owner 裁决 [CONSISTENCY-1]）====================

    @Test
    @DisplayName("AC-INC-16: 实际回款 1000000 × 5% = 50000 基础奖金池（ZK 实际回款口径）")
    void actualReceipts1Million() {
        BigDecimal base = service.calculateBasePoolConfigurable(amt("1000000"));
        assertThat(base).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("AC-INC-16: 实际回款 0 ⇒ basePool = 0")
    void actualReceiptsZero() {
        assertThat(service.calculateBasePoolConfigurable(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("AC-INC-16: 实际回款 null ⇒ basePool = 0")
    void actualReceiptsNull() {
        assertThat(service.calculateBasePoolConfigurable(null)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("AC-INC-16: 实际回款 12345.67 × 5% = 617.28（保留 2 位小数）")
    void actualReceiptsDecimal() {
        BigDecimal base = service.calculateBasePoolConfigurable(amt("12345.67"));
        assertThat(base).isEqualByComparingTo("617.28");
    }

    @Test
    @DisplayName("AC-INC-16: 100 万 × 5% = 50000；与现有 calculateBasePool 一致")
    void consistentWithExistingCalc() {
        BigDecimal newCalc = service.calculateBasePoolConfigurable(amt("1000000"));
        BigDecimal oldCalc = service.calculateBasePool(amt("1000000"), new BigDecimal("0.05"));
        assertThat(newCalc).isEqualByComparingTo(oldCalc);
    }

    // ==================== BR-INC-04: poolRate 实时读取开关 ====================

    @Test
    @DisplayName("BR-INC-04: 实时读默认 poolRate = 0.0500")
    void activePoolRate() {
        assertThat(service.readActivePoolRate()).isEqualByComparingTo("0.0500");
    }

    @Test
    @DisplayName("BR-INC-04: 实时读 + 入参 = 1000000 × 5% = 50000")
    void activeRateCalculation() {
        BigDecimal base = service.calculateBasePoolConfigurable(amt("1000000"));
        assertThat(base).isEqualByComparingTo("50000.00");
    }

    // ==================== BR-INC-04: poolRate 非法拒绝 ====================

    @Test
    @DisplayName("BR-INC-04: poolRate = null ⇒ 走默认")
    void poolRateNull() {
        BonusPoolService.validatePoolRate(null);
    }

    @Test
    @DisplayName("BR-INC-04: poolRate = 0.05 合法")
    void poolRateValid() {
        BonusPoolService.validatePoolRate(amt("0.05"));
        BonusPoolService.validatePoolRate(amt("0.0001"));
        BonusPoolService.validatePoolRate(amt("1.0"));
    }

    @Test
    @DisplayName("BR-INC-04: poolRate = 0 抛异常（< MIN）")
    void poolRateZero() {
        assertThatThrownBy(() -> BonusPoolService.validatePoolRate(amt("0")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-04: poolRate = 1.5 抛异常（> MAX=1）")
    void poolRateTooLarge() {
        assertThatThrownBy(() -> BonusPoolService.validatePoolRate(amt("1.5")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-04: poolRate 精度 5 位小数抛异常（> SCALE=4）")
    void poolRatePrecisionTooHigh() {
        assertThatThrownBy(() -> BonusPoolService.validatePoolRate(amt("0.05000")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-04: poolRate 精度 4 位小数合法")
    void poolRatePrecisionOk() {
        BonusPoolService.validatePoolRate(amt("0.0500"));
        BonusPoolService.validatePoolRate(amt("0.1234"));
    }

    // ==================== BR-INC-05: 项目 S/A/B 系数区段 ====================

    @Test
    @DisplayName("BR-INC-05: S 级系数 1.5~2.0 合法")
    void coefficientSValid() {
        BonusPoolService.validateProjectCoefficient("S", amt("1.5"));
        BonusPoolService.validateProjectCoefficient("S", amt("1.8"));
        BonusPoolService.validateProjectCoefficient("S", amt("2.0"));
    }

    @Test
    @DisplayName("BR-INC-05: S 级系数 1.4 抛异常（< MIN）")
    void coefficientSTooLow() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("S", amt("1.4")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-05: S 级系数 2.1 抛异常（> MAX）")
    void coefficientSTooHigh() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("S", amt("2.1")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-05: A 级系数 = 1.0 合法")
    void coefficientAValid() {
        BonusPoolService.validateProjectCoefficient("A", amt("1.0"));
    }

    @Test
    @DisplayName("BR-INC-05: A 级系数 1.1 抛异常（≠ 1.0）")
    void coefficientANotOne() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("A", amt("1.1")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-05: B 级系数 0.6~0.8 合法")
    void coefficientBValid() {
        BonusPoolService.validateProjectCoefficient("B", amt("0.6"));
        BonusPoolService.validateProjectCoefficient("B", amt("0.7"));
        BonusPoolService.validateProjectCoefficient("B", amt("0.8"));
    }

    @Test
    @DisplayName("BR-INC-05: B 级系数 0.5 抛异常（< MIN）")
    void coefficientBTooLow() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("B", amt("0.5")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-05: B 级系数 0.9 抛异常（> MAX）")
    void coefficientBTooHigh() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("B", amt("0.9")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-05: 未知等级 X 抛异常")
    void coefficientUnknownLevel() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("X", amt("1.0")))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("BR-INC-05: 系数 null 抛异常")
    void coefficientNull() {
        assertThatThrownBy(() -> BonusPoolService.validateProjectCoefficient("S", null))
            .isInstanceOf(Exception.class);
    }

    // ==================== BR-INC-05: 默认系数 ====================

    @Test
    @DisplayName("BR-INC-05: S 默认系数 = 1.8")
    void defaultCoefficientS() {
        assertThat(service.getDefaultCoefficientFor("S")).isEqualByComparingTo("1.8");
    }

    @Test
    @DisplayName("BR-INC-05: A 默认系数 = 1.0")
    void defaultCoefficientA() {
        assertThat(service.getDefaultCoefficientFor("A")).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("BR-INC-05: B 默认系数 = 0.7")
    void defaultCoefficientB() {
        assertThat(service.getDefaultCoefficientFor("B")).isEqualByComparingTo("0.7");
    }

    // ==================== BR-INC-04: 入参 poolRate 计算 + Decimal 舍入 ====================

    @Test
    @DisplayName("入参 poolRate = 0.1：1000000 × 0.1 = 100000")
    void calculateWithRate10pct() {
        BigDecimal base = service.calculateBasePoolWithRate(amt("1000000"), amt("0.1"));
        assertThat(base).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("Decimal 舍入：1234.567 × 0.05 = 61.73（HALF_UP）")
    void decimalRounding() {
        BigDecimal base = service.calculateBasePoolWithRate(amt("1234.567"), amt("0.05"));
        assertThat(base).isEqualByComparingTo("61.73");
    }

    @Test
    @DisplayName("入参非法 poolRate = 2.0 抛异常")
    void calculateWithIllegalRate() {
        assertThatThrownBy(() -> service.calculateBasePoolWithRate(amt("1000"), amt("2.0")))
            .isInstanceOf(Exception.class);
    }
}
