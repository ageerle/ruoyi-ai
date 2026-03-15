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
    QIAN_WEN("qianwen", "通义千问"),
    PPIO("ppio", "PPIO派欧云"),
    OPEN_AI("openai", "openai");
    private final String code;
    private final String description;

    ChatModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
