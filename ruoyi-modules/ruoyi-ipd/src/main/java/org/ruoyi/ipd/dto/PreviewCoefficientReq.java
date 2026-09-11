package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * P3-4.5：项目绩效系数预览请求（前端 P0-10.34 激励管理-奖金池核算-试算按钮）
 *
 * <p>预览端点契约（AC-INC-22/23/24 + BR-INC-07）：
 * <ul>
 *   <li>score：综合得分 [0, 100]（必填）</li>
 *   <li>strategy：取数策略（PROJECT_SCORE / WEIGHTED_AVG / LAST_QUARTER；可空 → 走 system_configs）</li>
 *   <li>projectId：项目 ID（预留给 P3-4.6 多项目叠加；当前 MVP 不参与计算）</li>
 * </ul>
 *
 * <p>preview 不写 audit_log、不 insert bonus_pools；仅返回试算结果供前端即时反馈。
 */
public record PreviewCoefficientReq(
    @NotNull(message = "项目 ID 不能为空") Long projectId,

    @NotNull(message = "综合得分不能为空")
    @DecimalMin(value = "0", message = "综合得分不能为负")
    BigDecimal score,

    String strategy
) {
}