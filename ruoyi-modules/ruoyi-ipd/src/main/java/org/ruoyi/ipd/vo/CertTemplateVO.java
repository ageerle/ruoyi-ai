package org.ruoyi.ipd.vo;

import org.ruoyi.ipd.domain.CertTemplate;

import java.util.Date;

/**
 * View: CertTemplate 的对外暴露视图，移除内部字段（tenantId / delFlag / createBy / updateBy / createDept / params）。
 * 对应接口：/api/v1/cert-templates
 */
public record CertTemplateVO(
    Long id,
    String countryCode,
    String countryName,
    String certName,
    String certAuthority,
    String requirementDesc,
    String isMandatory,
    Date createTime,
    Date updateTime
) {
    public static CertTemplateVO from(CertTemplate t) {
        return new CertTemplateVO(
            t.getId(),
            t.getCountryCode(),
            t.getCountryName(),
            t.getCertName(),
            t.getCertAuthority(),
            t.getRequirementDesc(),
            t.getIsMandatory(),
            t.getCreateTime(),
            t.getUpdateTime()
        );
    }
}