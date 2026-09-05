package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CODE-01：新增 Gate 评审要素请求白名单（P1-6 超管）。
 * id/tenantId/delFlag 不收。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GateElementCreateReq(
    String gateCode,
    String elementCode,
    String elementName,
    String passStandard,
    String isVeto,
    Integer sortOrder,
    String enabled) {

    public org.ruoyi.ipd.domain.GateElement toEntity() {
        return org.ruoyi.ipd.domain.GateElement.builder()
            .gateCode(gateCode).elementCode(elementCode).elementName(elementName)
            .passStandard(passStandard).isVeto(isVeto).sortOrder(sortOrder).enabled(enabled)
            .build();
    }
}
