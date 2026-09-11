package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * P1-6.1 生命周期辅助操作请求白名单：
 * copy 用 elementCode（克隆目标的新编码）；revert 用 auditLogId（要回滚到的审计快照）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GateElementLifecycleReq(
    String elementCode,
    Long auditLogId) {
}
