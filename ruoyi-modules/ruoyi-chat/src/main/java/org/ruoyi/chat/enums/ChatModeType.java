package org.ruoyi.chat.enums;

import lombok.Getter;

@Getter
public enum ChatModeType {
    OLLAMA("ollama", "本地部署模型"),
    CHAT("chat", "中转模型"),
    DIFY("dify", "DIFY"),
    COZE("coze", "扣子"),

    ZHIPU("zhipu", "智谱清言"),

    DEEPSEEK("deepseek", "深度求索"),

    QIANWEN("qianwen", "通义千问"),

    VECTOR("vector", "知识库向量模型");

    private final String code;
    private final String description;

    ChatModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
