package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 负反馈检测结果（P3-8.1；AC-INC-36a 检测引擎输出）。
 *
 * <p>包含：
 * <ul>
 *   <li>projectId / ranAt / ranBy</li>
 *   <li>triggered — 是否检测到任一触发情形</li>
 *   <li>triggers — 每个触发情形的匹配结果（含 evidence 证据 + recommendedAttribution 责任推荐）</li>
 *   <li>summary — 校验总数 + 匹配数</li>
 * </ul>
 *
 * <p>本卡 preview 模式：仅返回，不落库；commit 时由 NegativeFeedbackService.create 落库。
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetectionResult(
    Long projectId,
    Date ranAt,
    Long ranBy,
    boolean triggered,
    List<TriggerDetection> triggers,
    Map<String, Integer> summary
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TriggerDetection(
        String triggerType,
        boolean matched,
        Map<String, Object> evidence,
        Attribution recommendedAttribution,
        Map<String, Boolean> projectHasPm
    ) {
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Attribution(
        String mainRole,
        String mainExecution,
        String relatedRole,
        String relatedExecution,
        BigDecimal tierDelta,
        Integer bonusDisqualify
    ) {
    }
}