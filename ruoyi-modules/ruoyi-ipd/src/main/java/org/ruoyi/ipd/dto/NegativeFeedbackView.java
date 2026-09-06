package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Date;

/**
 * P3-8.2 负反馈详情视图
 *
 * <p>含主责/连带映射 / 执行动作 / 奖金资格影响 / 状态机字段。
 * <p>前端页 P0-10.36 列表 + 详情渲染使用。
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record NegativeFeedbackView(
    Long id,
    Long projectId,
    String triggerType,
    String mainRole,
    Long mainPersonId,
    String relatedRole,
    Long relatedPersonId,
    String mainExecution,
    String relatedExecution,
    Boolean bonusDisqualify,
    BigDecimal tierDelta,
    String triggerMonth,
    String recoveryMonth,
    String triggerEvidence,
    String status,
    Long triggeredBy,
    Long decidedBy,
    Date decidedAt,
    Long liftedBy,
    Date liftedAt,
    String decisionComment
) { }