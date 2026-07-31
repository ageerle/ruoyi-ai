package org.ruoyi.service.chat.impl.provider;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import io.github.imfangs.dify.client.DifyChatClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.AgentMessageEvent;
import io.github.imfangs.dify.client.event.BaseMessageEvent;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.MessageReplaceEvent;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.DifyConfig;
import io.github.imfangs.dify.client.model.common.Metadata;
import io.github.imfangs.dify.client.model.common.Usage;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dify 服务调用。
 *
 * 使用模型配置中的 apiHost 作为 Dify API 地址，apiKey 作为 Dify App API Key。
 */
@Service
@Slf4j
public class DifyChatServiceImpl implements AbstractChatService {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 300_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return new DifyLangChain4jChatModel(chatModelVo, chatRequest);
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return new DifyLangChain4jChatModel(chatModelVo, null);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.DIFY.getCode();
    }

    private static class DifyLangChain4jChatModel implements StreamingChatModel, ChatModel {

        private final ChatModelVo chatModelVo;
        private final ChatRequest runtimeRequest;
        private final List<ChatModelListener> listeners = List.of(new MyChatModelListener());

        private DifyLangChain4jChatModel(ChatModelVo chatModelVo, ChatRequest runtimeRequest) {
            this.chatModelVo = chatModelVo;
            this.runtimeRequest = runtimeRequest;
        }

        @Override
        public void doChat(dev.langchain4j.model.chat.request.ChatRequest request,
                           StreamingChatResponseHandler handler) {
            StringBuilder answerBuffer = new StringBuilder();
            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicReference<String> messageId = new AtomicReference<>();
            AtomicReference<Metadata> metadata = new AtomicReference<>();

            try {
                DifyChatClient client = createClient();
                client.sendChatMessageStream(toDifyMessage(request, ResponseMode.STREAMING), new ChatStreamCallback() {
                    @Override
                    public void onMessage(MessageEvent event) {
                        appendChunk(event.getAnswer(), event, answerBuffer, messageId, handler);
                    }

                    @Override
                    public void onAgentMessage(AgentMessageEvent event) {
                        appendChunk(event.getAnswer(), event, answerBuffer, messageId, handler);
                    }

                    @Override
                    public void onMessageReplace(MessageReplaceEvent event) {
                        if (StrUtil.isBlank(event.getAnswer())) {
                            return;
                        }
                        answerBuffer.setLength(0);
                        answerBuffer.append(event.getAnswer());
                        rememberMessageId(event, messageId);
                        log.debug("Dify 返回 message_replace 事件，当前 SSE 通道不支持替换已发送内容，仅用于最终消息落库");
                    }

                    @Override
                    public void onMessageEnd(MessageEndEvent event) {
                        metadata.set(event.getMetadata());
                        rememberMessageId(event, messageId);
                        completeOnce(finished, handler, answerBuffer.toString(), messageId.get(), metadata.get());
                    }

                    @Override
                    public void onError(ErrorEvent event) {
                        String message = StrUtil.format("Dify 调用失败: status={}, code={}, message={}",
                            event.getStatus(), event.getCode(), event.getMessage());
                        errorOnce(finished, handler, new IllegalStateException(message));
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        errorOnce(finished, handler, throwable);
                    }
                });
            } catch (IOException | DifyApiException e) {
                errorOnce(finished, handler, e);
            }
        }

        @Override
        public ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest request) {
            try {
                DifyChatClient client = createClient();
                ChatMessageResponse response = client.sendChatMessage(toDifyMessage(request, ResponseMode.BLOCKING));
                return toChatResponse(
                    StrUtil.nullToDefault(response.getAnswer(), ""),
                    response.getMessageId(),
                    response.getMetadata()
                );
            } catch (IOException | DifyApiException e) {
                throw new IllegalStateException("Dify 调用失败", e);
            }
        }

        @Override
        public ChatRequestParameters defaultRequestParameters() {
            return ChatRequestParameters.builder()
                .modelName(chatModelVo.getModelName())
                .build();
        }

        @Override
        public List<ChatModelListener> listeners() {
            return listeners;
        }

        @Override
        public ModelProvider provider() {
            return ModelProvider.OTHER;
        }

        @Override
        public Set<Capability> supportedCapabilities() {
            return Set.of();
        }

        private DifyChatClient createClient() {
            DifyConfig config = DifyConfig.builder()
                .baseUrl(normalizeBaseUrl(chatModelVo.getApiHost()))
                .apiKey(requiredApiKey())
                .connectTimeout(CONNECT_TIMEOUT_MS)
                .readTimeout(READ_TIMEOUT_MS)
                .writeTimeout(WRITE_TIMEOUT_MS)
                .build();
            return DifyClientFactory.createChatClient(config);
        }

        private io.github.imfangs.dify.client.model.chat.ChatMessage toDifyMessage(
            dev.langchain4j.model.chat.request.ChatRequest request,
            ResponseMode responseMode) {
            return io.github.imfangs.dify.client.model.chat.ChatMessage.builder()
                .query(toDifyQuery(request.messages()))
                .inputs(Map.of())
                .responseMode(responseMode)
                .user(resolveUser())
                .autoGenerateName(Boolean.TRUE)
                .build();
        }

        private String toDifyQuery(List<ChatMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return "";
            }
            if (messages.size() == 1) {
                return messageText(messages.get(0));
            }

            StringBuilder builder = new StringBuilder();
            builder.append("以下是当前会话上下文，请基于最后一条用户消息回答。").append("\n\n");
            for (ChatMessage message : messages) {
                builder.append(roleName(message)).append(": ")
                    .append(messageText(message))
                    .append("\n");
            }
            return builder.toString().trim();
        }

        private String messageText(ChatMessage message) {
            if (message instanceof UserMessage userMessage) {
                if (userMessage.hasSingleText()) {
                    return userMessage.singleText();
                }
                return String.valueOf(userMessage.contents());
            }
            if (message instanceof SystemMessage systemMessage) {
                return systemMessage.text();
            }
            if (message instanceof AiMessage aiMessage) {
                return StrUtil.nullToDefault(aiMessage.text(), "");
            }
            return String.valueOf(message);
        }

        private String roleName(ChatMessage message) {
            return switch (message.type()) {
                case SYSTEM -> "System";
                case USER -> "User";
                case AI -> "Assistant";
                case TOOL_EXECUTION_RESULT -> "Tool";
                case CUSTOM -> "Custom";
            };
        }

        private String resolveUser() {
            if (runtimeRequest != null && runtimeRequest.getUserId() != null) {
                return String.valueOf(runtimeRequest.getUserId());
            }
            if (runtimeRequest != null && runtimeRequest.getSessionId() != null) {
                return "session-" + runtimeRequest.getSessionId();
            }
            return "ruoyi-ai";
        }

        private String normalizeBaseUrl(String baseUrl) {
            if (StrUtil.isBlank(baseUrl)) {
                throw new IllegalArgumentException("Dify 的请求地址(apiHost)不能为空");
            }
            return StrUtil.removeSuffix(baseUrl, "/");
        }

        private String requiredApiKey() {
            if (StrUtil.isBlank(chatModelVo.getApiKey())) {
                throw new IllegalArgumentException("Dify 的 API Key 不能为空");
            }
            return chatModelVo.getApiKey();
        }

        private void appendChunk(String chunk, BaseMessageEvent event, StringBuilder answerBuffer,
                                 AtomicReference<String> messageId,
                                 StreamingChatResponseHandler handler) {
            if (StrUtil.isBlank(chunk)) {
                return;
            }
            answerBuffer.append(chunk);
            rememberMessageId(event, messageId);
            handler.onPartialResponse(chunk);
        }

        private void rememberMessageId(BaseMessageEvent event, AtomicReference<String> messageId) {
            if (event != null && StrUtil.isNotBlank(event.getMessageId())) {
                messageId.set(event.getMessageId());
            }
        }

        private void completeOnce(AtomicBoolean finished, StreamingChatResponseHandler handler,
                                  String answer, String messageId, Metadata metadata) {
            if (finished.compareAndSet(false, true)) {
                handler.onCompleteResponse(toChatResponse(answer, messageId, metadata));
            }
        }

        private void errorOnce(AtomicBoolean finished, StreamingChatResponseHandler handler, Throwable error) {
            if (finished.compareAndSet(false, true)) {
                handler.onError(error);
            }
        }

        private ChatResponse toChatResponse(String answer, String messageId, Metadata metadata) {
            return ChatResponse.builder()
                .aiMessage(AiMessage.from(StrUtil.nullToDefault(answer, "")))
                .id(messageId)
                .modelName(chatModelVo.getModelName())
                .tokenUsage(toTokenUsage(metadata))
                .finishReason(FinishReason.STOP)
                .build();
        }

        private TokenUsage toTokenUsage(Metadata metadata) {
            if (metadata == null || metadata.getUsage() == null) {
                return null;
            }
            Usage usage = metadata.getUsage();
            return new TokenUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
            );
        }
    }
}
