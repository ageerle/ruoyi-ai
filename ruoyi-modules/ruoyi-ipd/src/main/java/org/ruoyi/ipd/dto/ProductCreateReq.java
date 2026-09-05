package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CODE-01：新增产品请求白名单（API-02）。
 * projectId/status/tenantId/delFlag/id 不收——1:1 绑定走 bind-project，状态走 changeStatus。
 * source 允许显式声明（BR-PROD-01 三路来源），Controller 校验枚举。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCreateReq(
    String productCode,
    String productName,
    String modelCode,
    String source,
    Long groupId) {

    public org.ruoyi.ipd.domain.Product toEntity() {
        return org.ruoyi.ipd.domain.Product.builder()
            .productCode(productCode).productName(productName).modelCode(modelCode)
            .source(source).groupId(groupId)
            .build();
    }
}
