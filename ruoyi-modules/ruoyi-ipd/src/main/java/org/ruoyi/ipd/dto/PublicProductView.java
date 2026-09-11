package org.ruoyi.ipd.dto;

/**
 * P4-1.1 公开产品下拉视图（页38：三情形选择源——在售 / 在研 / 其他）。
 * listingStatus 派生：ON_SALE=modelCode 非空；IN_DEV=projectId 非空；OTHER。
 */
public record PublicProductView(Long id, String productName, String modelCode, String status, String listingStatus) {
}
