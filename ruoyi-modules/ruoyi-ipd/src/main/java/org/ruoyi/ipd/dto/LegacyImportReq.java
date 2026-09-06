package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * P1-9.1：存量项目导入请求（BR-PROD-03 / AC-PROD-04）。
 * source 强制 LEGACY；编码由服务端生成；不接受伪造历史 DONE。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LegacyImportReq(
    String name,
    Long productId,
    String templateType,
    String targetMarkets,
    String level,
    BigDecimal levelCoefficient,
    String levelCoefficientReason,
    BigDecimal targetSalesAmount,
    Integer targetChannelCount,
    Integer targetNps,
    Integer targetSceneCount,
    Long mainGroupId,
    /** 存量生效日（必填） */
    Date legacyEffectiveAt,
    /** 申报当前阶段 CONCEPT|PLAN|DEV|VALID|LAUNCH|LIFECYCLE（及英文别名） */
    String declaredStage,
    /** 必须 true：确认已过节点历史缺失 */
    Boolean missingHistoryAck,
    /** 可选：动作码 → 替代佐证来源（写入 remark，不改 status） */
    Map<String, String> alternativeEvidence
) {
}
