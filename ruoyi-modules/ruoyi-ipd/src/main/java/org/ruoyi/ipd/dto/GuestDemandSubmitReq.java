package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * P4-1.1 游客需求提交请求（页38 字段模型；TS-06 requirements）。
 * <p>白名单 DTO：未知字段一律忽略，表单不含「负责人」字段（由系统路由，BR-REQ-02）。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuestDemandSubmitReq(
    String customerName,
    String feedbackPerson,
    String contact,
    Long productId,
    String rawModel,
    String functionalRequirement,
    String website
) {
}
