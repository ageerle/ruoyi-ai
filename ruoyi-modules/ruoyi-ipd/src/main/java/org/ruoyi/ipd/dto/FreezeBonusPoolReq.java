package org.ruoyi.ipd.dto;

/**
 * P3-4.4：奖金池冻结/确认请求（前端 P0-10.34）
 *
 * <p>冻结/确认奖金池：DRAFT → CONFIRMED。
 * 幂等：同状态再调不写审计。
 *
 * @param reason 冻结理由（可空；审计 reason 字段）
 */
public record FreezeBonusPoolReq(
    String reason
) {
}
