package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SEC-API-01：认证模板创建入参 DTO。
 * 白名单化，禁止客户端注入 id/tenantId/delFlag/createTime 等服务端权威字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CertTemplateCreateReq(
    @NotBlank(message = "countryCode必填") @Size(max = 10) String countryCode,
    @NotBlank(message = "countryName必填") @Size(max = 64) String countryName,
    @NotBlank(message = "certName必填") @Size(max = 128) String certName,
    @Size(max = 128) String certAuthority,
    @Size(max = 2000) String requirementDesc,
    /** 是否强制（默认"1"） */
    @Size(max = 2) String isMandatory
) {
    /** 转为持久化实体；id/tenantId/delFlag/createTime/updateTime 由服务端覆写 */
    public org.ruoyi.ipd.domain.CertTemplate toEntity() {
        return org.ruoyi.ipd.domain.CertTemplate.builder()
            .countryCode(countryCode)
            .countryName(countryName)
            .certName(certName)
            .certAuthority(certAuthority)
            .requirementDesc(requirementDesc)
            .isMandatory(isMandatory != null && !isMandatory.isBlank() ? isMandatory : "1")
            .build();
    }
}
