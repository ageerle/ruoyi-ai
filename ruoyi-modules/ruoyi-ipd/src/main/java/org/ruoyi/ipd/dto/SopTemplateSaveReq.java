package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * P1-3.3：SOP draft 编辑白名单（CODE-01：id/version/status 不可由客户端注入）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SopTemplateSaveReq(String title, String content) {
}
