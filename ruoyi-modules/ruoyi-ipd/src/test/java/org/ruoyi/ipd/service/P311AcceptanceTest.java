package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.IpdBusinessException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P3-1.1 功能 KPI 指标来源与计算验收测试
 * AC：AC-KPI-01/02/03/04/15；BR：BR-KPI-01/02/03/06。
 *
 * <p>覆盖维度：
 * <ol>
 *   <li>正向：综合得分公式、四项权重、偏差天数</li>
 *   <li>反向：共担权重 < 30%、四项不等、pmRole 错、负偏差</li>
 *   <li>边界：w=0.7 共享=0.3 临界、偏差=0/30/31、得分=0/100</li>
 *   <li>异常：null 权重、空权重列表、负偏差、窗口起始晚于截止</li>
 *   <li>并发：单线程纯函数（覆盖 by 跳过）</li>
 *   <li>幂等：纯函数无副作用（覆盖 by 跳过）</li>
 * </ol>
 */
@Tag("dev")
class P311AcceptanceTest {

    private final KpiRecordService service = new KpiRecordService();

    // ==================== AC-KPI-04: 综合得分计算 ====================

    @Test
    @DisplayName("AC-KPI-04 正例: 功能 80 + 共担 70，w=0.6 ⇒ 80×0.6 + 70×0.4 = 76.00")
    void comprehensiveByFormula() {
        BigDecimal result = service.computeComprehensive(
            new BigDecimal("80"), new BigDecimal("70"), new BigDecimal("0.60"));
        assertThat(result).isEqualByComparingTo("76.00");
    }

    @Test
    @DisplayName("AC-KPI-04 边界: 功能 100 + 共担 100 ⇒ 100.00")
    void comprehensivePerfect() {
        BigDecimal result = service.computeComprehensive(
            new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("0.60"));
        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("AC-KPI-04 边界: 功能 0 + 共担 0 ⇒ 0.00")
    void comprehensiveZero() {
        BigDecimal result = service.computeComprehensive(
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.60"));
        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("AC-KPI-04: null w 走默认 0.6")
    void comprehensiveDefaultWeight() {
        BigDecimal result = service.computeComprehensive(
            new BigDecimal("80"), new BigDecimal("70"), null);
        assertThat(result).isEqualByComparingTo("76.00");
    }

    // ==================== AC-KPI-01: 权重校验 ====================

    @Test
    @DisplayName("AC-KPI-01 正例: w=0.6（共享=0.4 ≥ 0.3）保存成功")
    void saveWeightsOk() {
        BigDecimal effective = service.saveKpiWeights(new BigDecimal("0.60"));
        assertThat(effective).isEqualByComparingTo("0.60");
    }

    @Test
    @DisplayName("AC-KPI-01 边界: w=0.7（共享=0.3 临界）保存成功")
    void saveWeightsBoundary() {
        BigDecimal effective = service.saveKpiWeights(new BigDecimal("0.70"));
        assertThat(effective).isEqualByComparingTo("0.70");
    }

    @Test
    @DisplayName("AC-KPI-01 反例: w=0.71（共享=0.29 < 0.3）抛 IpdBusinessException")
    void saveWeightsRejectTooLow() {
        assertThatThrownBy(() -> service.saveKpiWeights(new BigDecimal("0.71")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("共担 KPI 权重不得低于 30%");
    }

    @Test
    @DisplayName("AC-KPI-01 反例: w=1.0（共享=0）抛 IpdBusinessException")
    void saveWeightsRejectAll() {
        assertThatThrownBy(() -> service.saveKpiWeights(BigDecimal.ONE))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("AC-KPI-01 反例: w=0（共享=1.0）抛 IpdBusinessException")
    void saveWeightsRejectZero() {
        assertThatThrownBy(() -> service.saveKpiWeights(BigDecimal.ZERO))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== AC-KPI-02: 市场 PM 四项功能 KPI ====================

    @Test
    @DisplayName("AC-KPI-02 正例: 市场 PM 四项各 15% 总 60%")
    void marketPmFourFieldsOk() {
        List<BigDecimal> weights = Arrays.asList(
            new BigDecimal("0.15"), new BigDecimal("0.15"),
            new BigDecimal("0.15"), new BigDecimal("0.15"));
        service.validateFunctionalEntry("MARKET_PM", weights);
    }

    @Test
    @DisplayName("AC-KPI-02 反例: 市场 PM 仅录入 3 项抛 IpdBusinessException")
    void marketPmThreeFieldsReject() {
        List<BigDecimal> weights = Arrays.asList(
            new BigDecimal("0.15"), new BigDecimal("0.15"), new BigDecimal("0.30"));
        assertThatThrownBy(() -> service.validateFunctionalEntry("MARKET_PM", weights))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("AC-KPI-02 反例: 市场 PM 四项和 ≠ 60%（如 0.30×4=1.20）抛 IpdBusinessException")
    void marketPmSumMismatch() {
        List<BigDecimal> weights = Arrays.asList(
            new BigDecimal("0.30"), new BigDecimal("0.30"),
            new BigDecimal("0.30"), new BigDecimal("0.30"));
        assertThatThrownBy(() -> service.validateFunctionalEntry("MARKET_PM", weights))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("总权重");
    }

    // ==================== AC-KPI-03: 研发 PM 四项功能 KPI ====================

    @Test
    @DisplayName("AC-KPI-03 正例: 研发 PM 四项各 15% 总 60%")
    void rdPmFourFieldsOk() {
        List<BigDecimal> weights = Arrays.asList(
            new BigDecimal("0.15"), new BigDecimal("0.15"),
            new BigDecimal("0.15"), new BigDecimal("0.15"));
        service.validateFunctionalEntry("RD_PM", weights);
    }

    @Test
    @DisplayName("AC-KPI-03 反例: 研发 PM 错把单项设为 1.0 抛 IpdBusinessException")
    void rdPmSingleFieldInvalid() {
        List<BigDecimal> weights = Arrays.asList(
            new BigDecimal("1.00"), new BigDecimal("-0.10"),
            new BigDecimal("0.20"), new BigDecimal("0.50"));
        assertThatThrownBy(() -> service.validateFunctionalEntry("RD_PM", weights))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("validateFunctionalEntry 反例: pmRole 未知抛 IpdBusinessException")
    void functionalEntryUnknownRole() {
        assertThatThrownBy(() -> service.validateFunctionalEntry("GUEST",
            Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== AC-KPI-15: 偏差天数 + 窗口命中率 ====================

    @Test
    @DisplayName("AC-KPI-15: 实际上市落在计划窗口内偏差 = 0")
    void deviationDaysInsideWindow() {
        long windowStart = 1_700_000_000_000L;
        long windowEnd = 1_702_592_000_000L; // +30 天
        long actual = windowStart + 86400_000L * 15L; // +15 天
        int d = service.computeDeviationDays(actual, windowStart, windowEnd);
        assertThat(d).isZero();
    }

    @Test
    @DisplayName("AC-KPI-15: 实际上市晚于窗口截止 5 天偏差 = 5")
    void deviationDaysAfterWindow() {
        long windowEnd = 1_702_592_000_000L;
        long actual = windowEnd + 86400_000L * 5L;
        int d = service.computeDeviationDays(actual, windowEnd - 86400_000L * 30L, windowEnd);
        assertThat(d).isEqualTo(5);
    }

    @Test
    @DisplayName("AC-KPI-15: 实际上市早于窗口起点 10 天偏差 = 10")
    void deviationDaysBeforeWindow() {
        long windowStart = 1_700_000_000_000L;
        long actual = windowStart - 86400_000L * 10L;
        int d = service.computeDeviationDays(actual, windowStart, windowStart + 86400_000L * 30L);
        assertThat(d).isEqualTo(10);
    }

    @Test
    @DisplayName("AC-KPI-15: 窗口起点晚于截止抛 IpdBusinessException")
    void deviationDaysInvalidWindow() {
        long start = 2_000_000_000_000L;
        long end = 1_000_000_000_000L;
        assertThatThrownBy(() -> service.computeDeviationDays(start, start, end))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("AC-KPI-15 窗口命中率: 偏差=0 ⇒ 100")
    void windowHitRateZeroDeviation() {
        assertThat(service.computeWindowHitRate(0)).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("AC-KPI-15 窗口命中率: 偏差=30 ⇒ 70（线性边界）")
    void windowHitRateThirty() {
        assertThat(service.computeWindowHitRate(30)).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("AC-KPI-15 窗口命中率: 偏差=31 ⇒ 0（>30 一律零）")
    void windowHitRateBeyondTolerance() {
        assertThat(service.computeWindowHitRate(31)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("AC-KPI-15 窗口命中率反例: 负偏差抛 IpdBusinessException")
    void windowHitRateNegativeDeviation() {
        assertThatThrownBy(() -> service.computeWindowHitRate(-1))
            .isInstanceOf(IpdBusinessException.class);
    }
}
