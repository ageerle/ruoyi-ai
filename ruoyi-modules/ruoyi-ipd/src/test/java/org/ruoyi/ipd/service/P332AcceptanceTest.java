package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3-3.2 绩效 < 60 停发与 60 天无产出确认 验收测试
 * AC: AC-INC-05, AC-INC-07, AC-INC-08；BR: BR-INC-11
 */
@Tag("dev")
class P332AcceptanceTest {

    // 纯算法 + 日期逻辑，不需要 mock mapper
    private final AllowanceService service = new AllowanceService(null, null);

    // ==================== AC-INC-05: 绩效 < 60 停发 ====================

    @Test
    @DisplayName("AC-INC-05: 综合考核 58 分 ⇒ STOP_SCORE_BELOW_60")
    void score58Stop() {
        assertThat(service.determineLowScoreStop(new BigDecimal("58")))
            .isEqualTo("STOP_SCORE_BELOW_60");
    }

    @Test
    @DisplayName("AC-INC-05: 综合考核 0 分 ⇒ STOP_SCORE_BELOW_60")
    void score0Stop() {
        assertThat(service.determineLowScoreStop(new BigDecimal("0")))
            .isEqualTo("STOP_SCORE_BELOW_60");
    }

    @Test
    @DisplayName("边界: 综合考核 59.99 ⇒ STOP_SCORE_BELOW_60（< 60）")
    void score59_99Stop() {
        assertThat(service.determineLowScoreStop(new BigDecimal("59.99")))
            .isEqualTo("STOP_SCORE_BELOW_60");
    }

    @Test
    @DisplayName("边界: 综合考核 60（不含） ⇒ 不停发")
    void score60NotStop() {
        assertThat(service.determineLowScoreStop(new BigDecimal("60"))).isNull();
    }

    @Test
    @DisplayName("正例: 综合考核 80 ⇒ 不停发")
    void score80NotStop() {
        assertThat(service.determineLowScoreStop(new BigDecimal("80"))).isNull();
    }

    @Test
    @DisplayName("正例: 综合考核 100 ⇒ 不停发")
    void score100NotStop() {
        assertThat(service.determineLowScoreStop(new BigDecimal("100"))).isNull();
    }

    @Test
    @DisplayName("正例: 综合考核 null ⇒ 不停发（无证据不发）")
    void scoreNullNotStop() {
        assertThat(service.determineLowScoreStop(null)).isNull();
    }

    // ==================== AC-INC-07/08: 60 天无产出判定 ====================

    @Test
    @DisplayName("AC-INC-07: 附加项目最后活动 = 60 天前 ⇒ STOP_NO_OUTPUT_60_DAYS")
    void additionalProject60DaysNoOutput() {
        // asOfDate = 2026-09-06, lastActivityDate = 2026-07-08 = 60 days ago
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-07-08");
        assertThat(service.determineNoOutput60DaysStop(last, asOf, true))
            .isEqualTo("STOP_NO_OUTPUT_60_DAYS");
    }

    @Test
    @DisplayName("AC-INC-07: 附加项目最后活动 = 90 天前 ⇒ STOP_NO_OUTPUT_60_DAYS")
    void additionalProject90DaysNoOutput() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-06-08");
        assertThat(service.determineNoOutput60DaysStop(last, asOf, true))
            .isEqualTo("STOP_NO_OUTPUT_60_DAYS");
    }

    @Test
    @DisplayName("AC-INC-07: 附加项目从未活动（null）⇒ STOP_NO_OUTPUT_60_DAYS")
    void additionalProjectNeverActive() {
        Date asOf = parseDate("2026-09-06");
        assertThat(service.determineNoOutput60DaysStop(null, asOf, true))
            .isEqualTo("STOP_NO_OUTPUT_60_DAYS");
    }

    @Test
    @DisplayName("边界: 附加项目最后活动 = 59 天前 ⇒ 不停发（< 60）")
    void additionalProject59DaysNotStop() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-07-09");
        assertThat(service.determineNoOutput60DaysStop(last, asOf, true)).isNull();
    }

    @Test
    @DisplayName("正例: 附加项目最后活动 = 30 天前 ⇒ 不停发")
    void additionalProject30DaysNotStop() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-08-07");
        assertThat(service.determineNoOutput60DaysStop(last, asOf, true)).isNull();
    }

    @Test
    @DisplayName("AC-INC-08: 主项目 90 天无产出 ⇒ 不停发")
    void mainProject90DaysNotStop() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-06-08");
        assertThat(service.determineNoOutput60DaysStop(last, asOf, false)).isNull();
    }

    @Test
    @DisplayName("AC-INC-08: 主项目从未活动 ⇒ 不停发")
    void mainProjectNeverActiveNotStop() {
        Date asOf = parseDate("2026-09-06");
        assertThat(service.determineNoOutput60DaysStop(null, asOf, false)).isNull();
    }

    @Test
    @DisplayName("正例: asOfDate=null ⇒ 不停发（无扫描日依据）")
    void noAsOfDateNotStop() {
        Date last = parseDate("2026-06-08");
        assertThat(service.determineNoOutput60DaysStop(last, null, true)).isNull();
    }

    // ==================== determineStopReasonP332 三层合一 ====================

    @Test
    @DisplayName("三层合一: 绩效 < 60 优先于 60 天无产出（附加项目）")
    void combinedScoreBelow60Priority() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-06-08");
        String reason = service.determineStopReasonP332(
            new BigDecimal("58"), last, asOf, true);
        assertThat(reason).isEqualTo("STOP_SCORE_BELOW_60");
    }

    @Test
    @DisplayName("三层合一: 绩效 = 60 但附加项目 60 天无产出 ⇒ STOP_NO_OUTPUT_60_DAYS")
    void combined60DaysNoOutput() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-07-08");
        String reason = service.determineStopReasonP332(
            new BigDecimal("60"), last, asOf, true);
        assertThat(reason).isEqualTo("STOP_NO_OUTPUT_60_DAYS");
    }

    @Test
    @DisplayName("三层合一: 绩效 OK 且主项目无产出 ⇒ 不停发")
    void combinedMainNoOutputNotStop() {
        Date asOf = parseDate("2026-09-06");
        String reason = service.determineStopReasonP332(
            new BigDecimal("80"), null, asOf, false);
        assertThat(reason).isNull();
    }

    @Test
    @DisplayName("三层合一: 绩效 OK + 附加项目活跃 ⇒ 不停发")
    void combinedAllOkNotStop() {
        Date asOf = parseDate("2026-09-06");
        Date last = parseDate("2026-09-01");
        String reason = service.determineStopReasonP332(
            new BigDecimal("80"), last, asOf, true);
        assertThat(reason).isNull();
    }

    // ==================== 兼容旧契约 determineStopReason ====================

    @Test
    @DisplayName("兼容旧契约: 58 分 + 主项目（isMainProject=true）⇒ SCORE_BELOW_60（旧名）")
    void legacyScoreBelow60() {
        assertThat(service.determineStopReason(new BigDecimal("58"), false, true))
            .isEqualTo("SCORE_BELOW_60");
    }

    @Test
    @DisplayName("兼容旧契约: 主项目无产出 ⇒ 不停发")
    void legacyMainNoOutput() {
        assertThat(service.determineStopReason(null, true, true)).isNull();
    }

    @Test
    @DisplayName("兼容旧契约: 附加项目无产出 ⇒ NO_OUTPUT_60_DAYS")
    void legacyAdditionalNoOutput() {
        assertThat(service.determineStopReason(null, true, false))
            .isEqualTo("NO_OUTPUT_60_DAYS");
    }

    // ==================== 工具方法 ====================

    private static Date parseDate(String s) {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
