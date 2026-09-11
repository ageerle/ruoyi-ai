package org.ruoyi.ipd.vo;

import java.math.BigDecimal;

/** 项目绩效归档/结算只读视图。 */
public record ProjectScoreView(
    Long projectId,
    Long personId,
    String pmRole,
    Integer versionNo,
    Integer ruleVersion,
    BigDecimal selfScore,
    BigDecimal marketLeaderScore,
    BigDecimal rdLeaderScore,
    BigDecimal weightedScore,
    boolean settled
) {
}
