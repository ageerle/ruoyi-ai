package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * P4-2.1：AI 模型配置保存白名单（CODE-01：id/enabled 不可注入；update 时 apiKey=null 表示不改密钥）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiModelSaveReq(String provider, String endpoint, String apiKey,
                             String model, BigDecimal temperature, Integer maxTokens) {
}
