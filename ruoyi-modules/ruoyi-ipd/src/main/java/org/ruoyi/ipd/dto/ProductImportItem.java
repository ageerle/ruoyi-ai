package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 超管批量导入在售型号单行（AC-PROD-06）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductImportItem(
    String productCode,
    String productName,
    String modelCode,
    Long groupId) {

    /**
     * 转为待导入产品实体（来源强制 ADMIN_IMPORT）。
     *
     * @return Product
     */
    public org.ruoyi.ipd.domain.Product toEntity() {
        return org.ruoyi.ipd.domain.Product.builder()
            .productCode(productCode).productName(productName).modelCode(modelCode)
            .groupId(groupId).source(org.ruoyi.ipd.domain.Product.SRC_ADMIN_IMPORT)
            .build();
    }
}
