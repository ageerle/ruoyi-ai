package org.ruoyi.service.coding.harness.loop.protocol;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.ruoyi.service.chat.impl.provider.doubao.DoubaoStreamingChatModel;
import org.ruoyi.service.coding.harness.model.HarnessMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Lossless semantic adapter from the durable Harness ledger to LangChain4j messages. */
public final class LangChain4jMessageMapper {

    private final ToolProtocolValidator validator;

    public LangChain4jMessageMapper() {
        this(new ToolProtocolValidator());
    }

    public LangChain4jMessageMapper(ToolProtocolValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    /**
     * Validates, removes CONTROL records, orders tool results by source call order, and maps the
     * exact request transcript. Incomplete or malformed batches never reach LangChain4j.
     */
    public List<ChatMessage> mapForNextModelRequest(List<HarnessMessage> ledgerMessages) {
        return mapForNextModelRequest(ledgerMessages, ignored -> null);
    }

    /** Allows the run processor to hydrate durable image handles without changing the ledger. */
    public List<ChatMessage> mapForNextModelRequest(
        List<HarnessMessage> ledgerMessages,
        Function<HarnessMessage, ChatMessage> override
    ) {
        Objects.requireNonNull(override, "override");
        ToolProtocolValidation validation = validator.validate(ledgerMessages);
        if (!validation.allowsNextModelRequest()) {
            throw new ToolProtocolException(validation);
        }
        List<ChatMessage> mapped = new ArrayList<>(validation.modelMessages().size());
        for (HarnessMessage message : validation.modelMessages()) {
            ChatMessage replacement = override.apply(message);
            mapped.add(replacement == null ? map(message) : replacement);
        }
        return List.copyOf(mapped);
    }

    public ToolProtocolValidation validate(List<HarnessMessage> ledgerMessages) {
        return validator.validate(ledgerMessages);
    }

    private ChatMessage map(HarnessMessage message) {
        return switch (message.role()) {
            case SYSTEM -> SystemMessage.from(message.content());
            case USER -> UserMessage.from(message.content());
            case ASSISTANT -> mapAssistant(message);
            case TOOL -> ToolExecutionResultMessage.builder()
                .id(message.toolCallId())
                .toolName(message.toolName())
                .text(message.content())
                .isError(message.toolError())
                .build();
            case CONTROL -> throw new IllegalArgumentException(
                "CONTROL records cannot be mapped to model messages");
        };
    }

    private static AiMessage mapAssistant(HarnessMessage message) {
        List<ToolExecutionRequest> requests = message.toolCalls().stream()
            .map(call -> ToolExecutionRequest.builder()
                .id(call.toolCallId())
                .name(call.toolName())
                .arguments(call.arguments())
                .build())
            .toList();
        AiMessage.Builder builder = AiMessage.builder()
            // DeepSeek accepts a nullable response content, but its next-request schema
            // still expects the assistant content field to be present. LangChain4j omits
            // null JSON properties, which turns a reasoning-only LENGTH response into an
            // invalid replay request. Preserve the semantic empty value explicitly while
            // continuing to replay reasoning_content byte-for-byte.
            .text(Objects.toString(message.content(), ""))
            .thinking(message.thinking())
            .toolExecutionRequests(requests);
        Object encrypted = message.metadata() == null
            ? null : message.metadata().get("doubaoEncryptedContent");
        if (encrypted instanceof String encryptedText && !encryptedText.isBlank()) {
            // Doubao 要求把上一轮 assistant 消息的思考加密原文（encrypted_content）原样回传，
            // 以维持工具多轮上下文。经 AiMessage.attributes 透传给 Doubao 桥接序列化。
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put(DoubaoStreamingChatModel.ENCRYPTED_CONTENT_ATTRIBUTE, encryptedText);
            builder.attributes(attributes);
        }
        return builder.build();
    }

    /**
     * 构造首条带图片的用户消息。DeepSeek 使用 AUTO/LOW/HIGH（original→HIGH）；
     * Doubao 额外真实支持 xhigh（映射到 LangChain4j 的 ULTRA_HIGH 枚举，由 Doubao 桥接
     * 序列化为 {@code detail:xhigh}，不降级为 high）。
     */
    public static UserMessage imageUserMessage(String text, List<ImageContent> images) {
        List<dev.langchain4j.data.message.Content> contents = new ArrayList<>(images.size() + 1);
        contents.add(TextContent.from(Objects.toString(text, "")));
        contents.addAll(images);
        return UserMessage.from(contents);
    }

    /** DeepSeek/Doubao detail 字面值到 LangChain4j 图片精度枚举。 */
    public static ImageContent.DetailLevel imageDetailLevel(String detail) {
        if (detail == null) {
            return ImageContent.DetailLevel.AUTO;
        }
        return switch (detail) {
            case "low" -> ImageContent.DetailLevel.LOW;
            case "original", "high" -> ImageContent.DetailLevel.HIGH;
            case "xhigh" -> ImageContent.DetailLevel.ULTRA_HIGH;
            default -> ImageContent.DetailLevel.AUTO;
        };
    }
}
