package org.ruoyi.ipd.vo;

import org.ruoyi.ipd.domain.Product;

import java.util.Date;

/**
 * View: Product 的对外暴露视图，移除内部字段（tenantId / delFlag / createBy / updateBy / createDept / params）。
 * 对应接口：/api/v1/products
 */
public record ProductVO(
    Long id,
    String productCode,
    String productName,
    String modelCode,
    String source,
    Long projectId,
    Long groupId,
    String status,
    Date createTime,
    Date updateTime
) {
    public static ProductVO from(Product p) {
        return new ProductVO(
            p.getId(),
            p.getProductCode(),
            p.getProductName(),
            p.getModelCode(),
            p.getSource(),
            p.getProjectId(),
            p.getGroupId(),
            p.getStatus(),
            p.getCreateTime(),
            p.getUpdateTime()
        );
    }
}