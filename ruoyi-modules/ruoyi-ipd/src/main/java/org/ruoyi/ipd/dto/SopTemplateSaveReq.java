package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * P1-3.3 SOP 模板 publish 入参（id/version/status 不可由客户端注入）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SopTemplateSaveReq(String templateCode, String templateName, String description, String category) {
}