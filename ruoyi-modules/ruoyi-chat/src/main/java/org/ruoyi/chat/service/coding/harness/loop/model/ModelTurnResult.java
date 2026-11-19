package org.ruoyi.chat.service.coding.harness.loop.model;

import dev.langchain4j.model.chat.response.ChatResponse;

import java.time.Duration;

/** Complete, provider-assembled response and wall-clock duration for one model turn. */
public record ModelTurnResult(ChatResponse response, Duration elapsed) {

    public ModelTurnResult {
        if (response == null || elapsed == null || elapsed.isNegative()) {
            throw new IllegalArgumentException("Invalid model turn result");
        }
    }
}
