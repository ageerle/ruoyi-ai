package org.ruoyi.ipd.dto;

import java.math.BigDecimal;

/**
 * P4-2.1：AI 模型配置回显视图——**不含 api_key 明文/密文**，仅脱敏掩码（BR：密钥不回显）。
 */
public record AiModelView(Long id, String provider, String endpoint, String model,
                          BigDecimal temperature, Integer maxTokens, String enabled,
                          String maskedKey) {
}
