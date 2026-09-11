package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CODE-01：更新 Gate 评审要素请求白名单。
 * gateCode/elementCode 不收——编码是要素身份，不可改；id 来自 path。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GateElementUpdateReq(
    String elementName,
    String passStandard,
    String isVeto,
    Integer sortOrder,
    String enabled) {

    public org.ruoyi.ipd.domain.GateElement toPatch(Long id) {
        return org.ruoyi.ipd.domain.GateElement.builder()
            .id(id).elementName(elementName).passStandard(passStandard)
            .isVeto(isVeto).sortOrder(sortOrder).enabled(enabled)
            .build();
    }
}
