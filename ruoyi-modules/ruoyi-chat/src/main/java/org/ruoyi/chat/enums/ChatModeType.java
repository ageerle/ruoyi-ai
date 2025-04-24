package org.ruoyi.chat.enums;

import lombok.Getter;

@Getter
public enum ChatModeType {
    OLLAMA("ollama", "本地部署模型"),
    CHAT("chat", "中转模型"),
    VECTOR("vector", "知识库向量模型");

    private final String code;
    private final String description;

    ChatModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
