package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CODE-01：产品编辑白名单（P1-1.2）。
 * source/projectId/status 不收——来源不可改，绑定走 bind-project，状态走 changeStatus。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductUpdateReq(
    String productCode,
    String productName,
    String modelCode,
    Long groupId) {

    /**
     * 转为 patch 实体（仅白名单字段）。
     *
     * @return Product 补丁
     */
    public org.ruoyi.ipd.domain.Product toPatch() {
        return org.ruoyi.ipd.domain.Product.builder()
            .productCode(productCode).productName(productName).modelCode(modelCode)
            .groupId(groupId)
            .build();
    }
}
