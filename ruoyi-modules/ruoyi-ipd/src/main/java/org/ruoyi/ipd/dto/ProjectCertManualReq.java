package org.ruoyi.ipd.dto;

/**
 * P1-7.1：手工补充项目认证项请求。
 *
 * @param countryCode 国家码
 * @param countryName 国家名
 * @param certName    认证名
 * @param certAuthority 机构（可空）
 * @param isMandatory 是否强制 1/0（可空默认 1）
 */
public record ProjectCertManualReq(
    String countryCode,
    String countryName,
    String certName,
    String certAuthority,
    String isMandatory
) {
}
