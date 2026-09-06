package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * P4-2.1 AI 模型配置（AC-AI-01）。列结构对齐 dbc75862 预铺的 ai_model_configs（12 表 DDL 约定）：
 * api_key_encrypted 落 AES 密文（base64），主密钥来自环境变量 IPD_AIMODEL_ENCRYPT_KEY；
 * temperature/maxTokens 等参数进 config_json（P4-2.2 可扩展 timeoutMs/retryTimes/预算/限流而不改表）。
 * 任何查询端点不回显明文/密文（脱敏走 {@code org.ruoyi.ipd.dto.AiModelView}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "ai_model_configs", autoResultMap = true)
public class AiModelConfig extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 供应商标识（OPENAI|AZURE|LOCAL…，dbc75862 注释口径） */
    private String provider;

    /** 模型名（uk_model_name 唯一键维度） */
    private String modelName;

    /** 接入端点（endpoint_url） */
    private String endpointUrl;

    /** API 密钥密文（AES-base64），严禁回显 */
    private String apiKeyEncrypted;

    /** 模型参数 JSON：{"temperature":0.7,"maxTokens":4096}，P4-2.2 起可扩展 */
    private String configJson;

    /** 激活标志（全局至多一条 true；enable 端点互斥切换） */
    private Boolean isActive;
}
