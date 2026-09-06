package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 月度账务切换验收报告（P3-7.1；BR-INC-12；AC-INC-50/51）
 *
 * <p>包含：
 * <ul>
 *   <li>month / ranAt / ranBy</li>
 *   <li>isLocked / lockedAt / lockedBy</li>
 *   <li>diffRate 总差异率 + passed 是否通过</li>
 *   <li>checks — 5 类校验明细</li>
 *   <li>summary — 总数 + 通过 / 失败计数</li>
 * </ul>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SwitchingAcceptanceReport(
    String month,
    Date ranAt,
    Long ranBy,
    boolean isLocked,
    Date lockedAt,
    Long lockedBy,
    BigDecimal diffRate,
    boolean passed,
    List<CheckResult> checks,
    Map<String, Integer> summary,
    String unlockReason,
    Date unlockedAt,
    Long unlockedBy
) {

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CheckResult(
        String name,
        boolean passed,
        Object expected,
        Object actual,
        BigDecimal diff,
        String note,
        Integer duplicateCount,
        BigDecimal kpiScoreSum,
        BigDecimal bonusDistributionSum
    ) {
    }
}