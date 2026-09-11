package org.ruoyi.ipd.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * P1-4.1 / P1-8.2：轻管日期 / BioCV FAR·FRR / 证书 / 算法分类白名单录入。
 * 禁止携带 status——状态只能走 /transit。
 *
 * @param actualDoneAt 实际完成日期（轻管 BR-IPD-05；秒级）
 * @param farValue     BioCV 实测 FAR（D11 等 valueFields 含 FAR）
 * @param frrValue     BioCV 实测 FRR
 * @param certNo       认证证书编号（V02 等 valueFields 含 CERT_NO）
 * @param certPassedAt 认证通过日期
 * @param algoType     算法分类 FINGERPRINT|FACE|PALM|VEIN|MULTI
 */
public record StageActionFieldsReq(
    Date actualDoneAt,
    BigDecimal farValue,
    BigDecimal frrValue,
    String certNo,
    Date certPassedAt,
    String algoType
) {
}
