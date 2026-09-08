package org.ruoyi.service.coding.harness.loop.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fail-closed validation for provider completion signals.
 *
 * <p>LangChain4j 1.17.2 maps unknown OpenAI-compatible {@code finish_reason} values to
 * {@code null}. OpenAI metadata retains the raw SSE frames, so the Harness validates that raw
 * signal instead of treating a non-empty partial answer as a natural stop.</p>
 */
public final class ProviderFinishReasonGuard {

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final String RESOURCE_EXHAUSTED = "insufficient_system_resource";

    private ProviderFinishReasonGuard() { }

    /** Returns {@code null} only when the response has a trustworthy, shape-consistent terminal. */
    public static String rejectionReason(ChatResponse response) {
        if (response == null || response.aiMessage() == null || response.metadata() == null) {
            return "Provider completed without a valid response terminal";
        }

        FinishReason finishReason = response.finishReason();
        if (response.metadata() instanceof OpenAiChatResponseMetadata openAiMetadata) {
            RawFinishReason raw = rawFinishReason(openAiMetadata.rawServerSentEvents());
            if (raw.malformed()) {
                return "OpenAI-compatible provider returned an unreadable finish reason";
            }
            if (raw.values().isEmpty()) {
                return "OpenAI-compatible provider completed without a raw finish reason";
            }
            if (raw.values().size() != 1) {
                return "OpenAI-compatible provider returned conflicting finish reasons";
            }
            String providerReason = raw.values().iterator().next();
            if (RESOURCE_EXHAUSTED.equals(providerReason)) {
                return "Provider reported insufficient system resources";
            }
            FinishReason rawMapped = mapKnownOpenAiReason(providerReason);
            if (rawMapped == null) {
                return "OpenAI-compatible provider returned an unknown finish reason";
            }
            if (finishReason != rawMapped) {
                return "OpenAI-compatible raw and mapped finish reasons disagree";
            }
        } else if (finishReason == null) {
            return "Provider completed without a finish reason";
        }

        return shapeRejection(finishReason, response.aiMessage());
    }

    private static RawFinishReason rawFinishReason(List<ServerSentEvent> events) {
        if (events == null || events.isEmpty()) {
            return new RawFinishReason(Set.of(), false);
        }
        Set<String> values = new LinkedHashSet<>();
        for (ServerSentEvent event : events) {
            String data = event == null ? null : event.data();
            if (data == null || data.isBlank() || "[DONE]".equals(data.strip())) {
                continue;
            }
            try {
                JsonNode root = JSON.readTree(data);
                if (root == null || !root.isObject()) {
                    return new RawFinishReason(Set.copyOf(values), true);
                }
                JsonNode choices = root.get("choices");
                if (choices == null || !choices.isArray()) {
                    return new RawFinishReason(Set.copyOf(values), true);
                }
                if (choices.isEmpty()) {
                    JsonNode usage = root.get("usage");
                    if (usage == null || !usage.isObject()) {
                        return new RawFinishReason(Set.copyOf(values), true);
                    }
                    continue;
                }
                if (choices.size() != 1 || !choices.get(0).isObject()) {
                    return new RawFinishReason(Set.copyOf(values), true);
                }
                JsonNode choice = choices.get(0);
                JsonNode choiceIndex = choice.get("index");
                if (choiceIndex == null || !choiceIndex.isIntegralNumber()
                    || choiceIndex.bigIntegerValue().signum() != 0) {
                    return new RawFinishReason(Set.copyOf(values), true);
                }
                JsonNode finishReason = choice.get("finish_reason");
                if (finishReason == null || finishReason.isNull()) {
                    continue;
                }
                if (!finishReason.isTextual() || finishReason.textValue().isBlank()) {
                    return new RawFinishReason(Set.copyOf(values), true);
                }
                values.add(finishReason.textValue());
            } catch (Exception malformed) {
                return new RawFinishReason(Set.copyOf(values), true);
            }
        }
        return new RawFinishReason(Set.copyOf(values), false);
    }

    private static FinishReason mapKnownOpenAiReason(String reason) {
        return switch (reason) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            case "tool_calls", "function_call" -> FinishReason.TOOL_EXECUTION;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> null;
        };
    }

    private static String shapeRejection(FinishReason finishReason, AiMessage message) {
        boolean hasTools = message.toolExecutionRequests() != null
            && !message.toolExecutionRequests().isEmpty();
        return switch (finishReason) {
            case STOP -> {
                if (hasTools) {
                    yield "Provider reported STOP while returning tool calls";
                }
                boolean hasAnswer = (message.text() != null && !message.text().isBlank())
                    || (message.thinking() != null && !message.thinking().isBlank());
                yield hasAnswer ? null : "Provider reported STOP without any response content";
            }
            case TOOL_EXECUTION -> hasTools
                ? null : "Provider reported TOOL_EXECUTION without a tool call";
            case LENGTH -> hasTools
                ? "Provider truncated a tool-bearing response" : null;
            case CONTENT_FILTER -> "Provider content filtering prevented a complete response";
            case OTHER -> "Provider returned an unsupported finish reason";
        };
    }

    private record RawFinishReason(Set<String> values, boolean malformed) { }
}
