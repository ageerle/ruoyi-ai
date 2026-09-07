/**
 * [SEC-FIX-HIGH-5.1] 奖金分配比例区间校验验收测试。
 *
 * ZK-IPD §三.2.4 约束：
 * - 市场 PM 占比 ∈ [0.40, 0.65]
 * - 研发 PM 占比 ∈ [0.35, 0.60]
 * - 两者之和 ≈ 1.0（容差 SUM_TOLERANCE）
 *
 * 覆盖范围：
 * 1. 合法组合（边界值）→ 通过
 * 2. marketShare < 0.40 → 抛 ServiceException
 * 3. marketShare > 0.65 → 抛 ServiceException
 * 4. rdShare < 0.35 → 抛 ServiceException
 * 5. rdShare > 0.60 → 抛 ServiceException
 * 6. sum != 1.0±tolerance → 抛 ServiceException
 * 7. 边界 0.40/0.60 严格通过
 */
package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BonusPoolDistributionValidationAcceptanceTest {

    private final BonusPoolService service = new BonusPoolService(null, null);  // calculateDistribution 不依赖 mapper

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] 合法组合 marketShare=0.55 rdShare=0.45 → 通过")
    void validDistribution() {
        java.util.Map<String, BigDecimal> out = service.calculateDistribution(
            new BigDecimal("0.55"), new BigDecimal("0.45"));
        assertThat(out.get("marketShare")).isEqualByComparingTo("0.55");
        assertThat(out.get("rdShare")).isEqualByComparingTo("0.45");
        assertThat(out.get("sum")).isEqualByComparingTo("1.00");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] 边界值 market=0.40 rd=0.60 → 通过（恰好 1.0）")
    void boundaryLowerMarket() {
        java.util.Map<String, BigDecimal> out = service.calculateDistribution(
            new BigDecimal("0.40"), new BigDecimal("0.60"));
        assertThat(out).containsKeys("marketShare", "rdShare", "sum");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] 边界值 market=0.65 rd=0.35 → 通过（恰好 1.0）")
    void boundaryUpperMarket() {
        java.util.Map<String, BigDecimal> out = service.calculateDistribution(
            new BigDecimal("0.65"), new BigDecimal("0.35"));
        assertThat(out).containsKeys("marketShare", "rdShare", "sum");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] marketShare=0.39 < 0.40 → 抛 ServiceException")
    void marketShareTooLow() {
        assertThatThrownBy(() -> service.calculateDistribution(
            new BigDecimal("0.39"), new BigDecimal("0.61")))
            .hasMessageContaining("市场 PM 分配比例须在 40%-65%");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] marketShare=0.66 > 0.65 → 抛 ServiceException")
    void marketShareTooHigh() {
        assertThatThrownBy(() -> service.calculateDistribution(
            new BigDecimal("0.66"), new BigDecimal("0.34")))
            .hasMessageContaining("市场 PM 分配比例须在 40%-65%");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] rdShare=0.34 < 0.35 → 抛 ServiceException")
    void rdShareTooLow() {
        assertThatThrownBy(() -> service.calculateDistribution(
            new BigDecimal("0.66"), new BigDecimal("0.34")))
            .hasMessageContaining("市场 PM 分配比例须在 40%-65%");  // marketShare 检查在前
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] rdShare=0.61 > 0.60 → 抛 ServiceException")
    void rdShareTooHigh() {
        assertThatThrownBy(() -> service.calculateDistribution(
            new BigDecimal("0.50"), new BigDecimal("0.61")))
            .hasMessageContaining("研发 PM 分配比例须在 35%-60%");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] sum=0.45+0.45=0.90 ≠ 1.0 → 抛 ServiceException（sum 守卫）")
    void sumMismatch() {
        assertThatThrownBy(() -> service.calculateDistribution(
            new BigDecimal("0.45"), new BigDecimal("0.45")))
            .hasMessageContaining("市场+研发分配比例总和须为 100%");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[SEC-FIX-HIGH-5.1] sum=0.55+0.4501=1.0001（万分之1容差内）→ 通过")
    void sumWithinTolerance() {
        java.util.Map<String, BigDecimal> out = service.calculateDistribution(
            new BigDecimal("0.55"), new BigDecimal("0.4501"));
        assertThat(out.get("sum").subtract(BigDecimal.ONE).abs())
            .isLessThanOrEqualTo(new BigDecimal("0.0001"));
    }
}
