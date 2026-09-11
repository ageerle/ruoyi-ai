package org.ruoyi.ipd.vo;

import java.math.BigDecimal;
import java.util.List;

/** K01-K04 共担 KPI 归集结果（P3-1.2）。 */
public record SharedKpiCollectView(
    Long projectId,
    String period,
    BigDecimal sharedScore,
    List<Metric> items
) {
    public record Metric(
        String code,
        String source,
        String actual,
        String target,
        BigDecimal score,
        BigDecimal weight,
        boolean included,
        String message
    ) {
    }
}
