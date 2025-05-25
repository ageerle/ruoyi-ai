package org.ruoyi.chat.service.chat;

import dev.langchain4j.service.TokenStream;

public interface StreamAssistant {
    TokenStream chat(String message);
}