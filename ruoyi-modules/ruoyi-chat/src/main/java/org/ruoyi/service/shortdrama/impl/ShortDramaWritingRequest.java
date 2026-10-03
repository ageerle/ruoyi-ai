package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;

/** One writing policy for scripts, asset analysis and storyboards, including synchronous fallbacks. */
final class ShortDramaWritingRequest {
    private ShortDramaWritingRequest() {}

    static ChatRequest forText(String model, String source) {
        var request = new ChatRequest();
        if (model != null && model.toLowerCase(java.util.Locale.ROOT).endsWith("deepseek-v4.1-flash")) {
            request.setEnableThinking(false);
            request.setReasoningEffort("none");
            return request;
        }
        request.setEnableThinking(true); // Receive activity counts from providers that still return them.
        if (model != null && (model.endsWith("doubao-seed-2.1-pro-260628") || model.endsWith("doubao-seed-2-1-pro-260628")))
            request.setReasoningEffort(source == null || source.length() <= 2000 ? "none" : "low");
        return request;
    }
}
