package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 贡献度确认归档版本视图（BR-INC-09 归档版本可追溯；2026-09-08 契约对照轮补交）。
 *
 * <p>每次组长 APPROVE 确认时归档一份不可变快照；版本列表 = 该项目历次确认版次。
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContributionVersionView(
    Long id,
    Long projectId,
    Integer versionNo,
    String status,
    BigDecimal marketShare,
    BigDecimal rdShare,
    BigDecimal dimInitiation,
    BigDecimal dimInnovation,
    BigDecimal dimLaunch,
    BigDecimal dimMarketResult,
    BigDecimal dimLeadership,
    BigDecimal tierCoefficient,
    String marketComment,
    String rdComment,
    Long leaderId,
    String leaderDecision,
    Date leaderDecidedAt,
    String leaderOpinion,
    Date submittedAt,
    Long archivedBy,
    Date archivedAt
) {
}
