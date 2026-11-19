package org.ruoyi.chat.service.coding.harness.loop.model;

import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;

/** Process-local progress listener. Implementations must not perform protocol settlement. */
public interface ModelTurnListener {

    ModelTurnListener NOOP = new ModelTurnListener() {
    };

    default void onTextDelta(String delta) {
    }

    default void onThinkingDelta(String delta) {
    }

    default void onPartialToolCall(PartialToolCall partialToolCall) {
    }

    default void onCompleteToolCall(CompleteToolCall completeToolCall) {
    }
}
