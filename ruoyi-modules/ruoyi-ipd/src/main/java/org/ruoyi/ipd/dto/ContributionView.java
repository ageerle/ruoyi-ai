package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 贡献度评定视图（P3-6.2；BR-INC-09 / AC-INC-25/27/28）。
 *
 * <p>对外暴露 status / 双 PM 五维度 / marketShare / rdShare / tierCoefficient / 组长确认信息。
 * 含 weightsValid 字段方便前端联动校验（市场比例 40-65%）。
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContributionView(
    Long id,
    Long projectId,
    String status,
    BigDecimal marketShare,
    BigDecimal rdShare,
    boolean weightsValid,
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
    Date createTime,
    Date updateTime
) {
}