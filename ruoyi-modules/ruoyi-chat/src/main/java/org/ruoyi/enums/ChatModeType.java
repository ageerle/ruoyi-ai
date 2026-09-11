package org.ruoyi.enums;

import lombok.Getter;

/**
 * 模型分类
 *
 * @author ageerle@163.com
 * @date 2025-12-14
 */
@Getter
public enum ChatModeType {
    OLLAMA("ollama", "ollama本地部署模型"),
    ZHI_PU("zhipu", "智谱清言"),
    DEEP_SEEK("deepseek", "深度求索"),
    PPIO("ppio", "PPIO 派欧云"),
    QIAN_WEN("qianwen", "通义千问"),
    OPEN_AI("openai", "openai"),
    ATLAS("atlas", "Atlas Cloud"),
    CUSTOM_API("custom_api", "自定义 OpenAI"),
    CUSTOM_ANTHROPIC("custom_anthropic", "自定义 Anthropic"),
    MINIMAX("minimax", "MiniMax"),
    DIFY("dify", "Dify"),
    COZE("coze", "Coze"),
    LITE_LLM("litellm", "LiteLLM"),
    XIAOMI("xiaomi", "小米MiMo");
    private final String code;
    private final String description;

    ChatModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
