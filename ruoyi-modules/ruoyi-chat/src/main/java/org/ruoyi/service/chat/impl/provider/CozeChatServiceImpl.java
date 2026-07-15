package org.ruoyi.service.chat.impl.provider;

import cn.hutool.core.util.StrUtil;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.model.Chat;
import com.coze.openapi.client.chat.model.ChatError;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.chat.model.ChatUsage;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
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
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.observability.MyChatModelListener;
import org.ruoyi.service.chat.AbstractChatService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coze bot chat service.
 *
 * In model config, modelName stores the Coze bot ID, apiHost stores the Coze API host,
 * and apiKey stores the PAT or OAuth access token.
 */
@Service
@Slf4j
public class CozeChatServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(ChatModelVo chatModelVo, ChatRequest chatRequest) {
        return new CozeLangChain4jChatModel(chatModelVo, chatRequest);
    }

    @Override
    public ChatModel buildChatModel(ChatModelVo chatModelVo) {
        return new CozeLangChain4jChatModel(chatModelVo, null);
    }

    @Override
    public String getProviderName() {
        return ChatModeType.COZE.getCode();
    }

    private static class CozeLangChain4jChatModel implements StreamingChatModel, ChatModel {

        private static final int CONNECT_TIMEOUT_MS = 10_000;
        private static final int READ_TIMEOUT_MS = 300_000;

        private final ChatModelVo chatModelVo;
        private final ChatRequest runtimeRequest;
        private final List<ChatModelListener> listeners = List.of(new MyChatModelListener());

        private CozeLangChain4jChatModel(ChatModelVo chatModelVo, ChatRequest runtimeRequest) {
            this.chatModelVo = chatModelVo;
            this.runtimeRequest = runtimeRequest;
        }

        @Override
        public void doChat(dev.langchain4j.model.chat.request.ChatRequest request,
                           StreamingChatResponseHandler handler) {
            StringBuilder answerBuffer = new StringBuilder();
            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicReference<String> messageId = new AtomicReference<>();
            AtomicReference<Chat> completedChat = new AtomicReference<>();
            CozeAPI coze = null;
            try {
                coze = createClient();
                coze.chat().stream(toCozeRequest(request)).blockingForEach(event ->
                    handleEvent(event, answerBuffer, messageId, completedChat, finished, handler, null)
                );
                completeOnce(finished, handler, answerBuffer.toString(), messageId.get(), completedChat.get());
            } catch (Throwable e) {
                errorOnce(finished, handler, e);
            } finally {
                shutdown(coze);
            }
        }

        @Override
        public ChatResponse doChat(dev.langchain4j.model.chat.request.ChatRequest request) {
            StringBuilder answerBuffer = new StringBuilder();
            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicReference<String> messageId = new AtomicReference<>();
            AtomicReference<Chat> completedChat = new AtomicReference<>();
            AtomicReference<RuntimeException> failure = new AtomicReference<>();
            CozeAPI coze = null;
            try {
                coze = createClient();
                coze.chat().stream(toCozeRequest(request)).blockingForEach(event ->
                    handleEvent(event, answerBuffer, messageId, completedChat, finished, null, failure)
                );
                if (failure.get() != null) {
                    throw failure.get();
                }
                return toChatResponse(answerBuffer.toString(), messageId.get(), completedChat.get());
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException("Coze call failed", e);
            } finally {
                shutdown(coze);
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

        private CozeAPI createClient() {
            return new CozeAPI.Builder()
                .baseURL(normalizeBaseUrl(chatModelVo.getApiHost()))
                .auth(new TokenAuth(requiredApiKey()))
                .connectTimeout(CONNECT_TIMEOUT_MS)
                .readTimeout(READ_TIMEOUT_MS)
                .build();
        }

        private CreateChatReq toCozeRequest(dev.langchain4j.model.chat.request.ChatRequest request) {
            return CreateChatReq.builder()
                .botID(requiredBotId())
                .userID(resolveUser())
                .messages(toCozeMessages(request.messages()))
                .autoSaveHistory(Boolean.FALSE)
                .build();
        }

        private List<Message> toCozeMessages(List<ChatMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return List.of(Message.buildUserQuestionText(""));
            }

            List<Message> cozeMessages = new ArrayList<>();
            StringBuilder systemMessages = new StringBuilder();
            for (ChatMessage message : messages) {
                if (message instanceof SystemMessage systemMessage) {
                    appendBlock(systemMessages, systemMessage.text());
                    continue;
                }

                String text = messageText(message);
                if (StrUtil.isBlank(text)) {
                    continue;
                }
                if (message instanceof AiMessage) {
                    cozeMessages.add(Message.buildAssistantAnswer(text));
                } else {
                    cozeMessages.add(Message.buildUserQuestionText(text));
                }
            }

            if (!systemMessages.isEmpty()) {
                cozeMessages.add(0, Message.buildUserQuestionText("System:\n" + systemMessages));
            }
            if (cozeMessages.isEmpty()) {
                cozeMessages.add(Message.buildUserQuestionText(""));
            }
            return cozeMessages;
        }

        private void handleEvent(ChatEvent event,
                                 StringBuilder answerBuffer,
                                 AtomicReference<String> messageId,
                                 AtomicReference<Chat> completedChat,
                                 AtomicBoolean finished,
                                 StreamingChatResponseHandler handler,
                                 AtomicReference<RuntimeException> failure) {
            if (event == null || finished.get()) {
                return;
            }

            if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
                appendDelta(event, answerBuffer, messageId, handler);
                return;
            }

            if (ChatEventType.CONVERSATION_MESSAGE_COMPLETED.equals(event.getEvent())) {
                rememberMessageId(event, messageId);
                if (answerBuffer.isEmpty() && event.getMessage() != null
                    && StrUtil.isNotEmpty(event.getMessage().getContent())) {
                    answerBuffer.append(event.getMessage().getContent());
                }
                return;
            }

            if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
                completedChat.set(event.getChat());
                complete(handler, failure, finished, answerBuffer.toString(), messageId.get(), event.getChat());
                return;
            }

            if (ChatEventType.CONVERSATION_CHAT_FAILED.equals(event.getEvent())
                || ChatEventType.ERROR.equals(event.getEvent())) {
                RuntimeException error = toFailure(event);
                fail(handler, failure, finished, error);
            }
        }

        private void appendDelta(ChatEvent event, StringBuilder answerBuffer,
                                 AtomicReference<String> messageId,
                                 StreamingChatResponseHandler handler) {
            if (event.getMessage() == null) {
                return;
            }
            String chunk = event.getMessage().getContent();
            if (StrUtil.isEmpty(chunk)) {
                return;
            }
            answerBuffer.append(chunk);
            rememberMessageId(event, messageId);
            if (handler != null) {
                handler.onPartialResponse(chunk);
            }
        }

        private void complete(StreamingChatResponseHandler handler,
                              AtomicReference<RuntimeException> failure,
                              AtomicBoolean finished,
                              String answer,
                              String messageId,
                              Chat chat) {
            if (handler != null) {
                completeOnce(finished, handler, answer, messageId, chat);
            } else {
                finished.compareAndSet(false, true);
            }
        }

        private void fail(StreamingChatResponseHandler handler,
                          AtomicReference<RuntimeException> failure,
                          AtomicBoolean finished,
                          RuntimeException error) {
            if (handler != null) {
                errorOnce(finished, handler, error);
            } else if (finished.compareAndSet(false, true)) {
                failure.set(error);
            }
        }

        private void rememberMessageId(ChatEvent event, AtomicReference<String> messageId) {
            if (event.getMessage() != null && StrUtil.isNotBlank(event.getMessage().getId())) {
                messageId.set(event.getMessage().getId());
            }
        }

        private void completeOnce(AtomicBoolean finished,
                                  StreamingChatResponseHandler handler,
                                  String answer,
                                  String messageId,
                                  Chat chat) {
            if (finished.compareAndSet(false, true)) {
                handler.onCompleteResponse(toChatResponse(answer, messageId, chat));
            }
        }

        private void errorOnce(AtomicBoolean finished, StreamingChatResponseHandler handler, Throwable error) {
            if (finished.compareAndSet(false, true)) {
                handler.onError(error);
            }
        }

        private ChatResponse toChatResponse(String answer, String messageId, Chat chat) {
            return ChatResponse.builder()
                .aiMessage(AiMessage.from(StrUtil.nullToDefault(answer, "")))
                .id(firstNotBlank(messageId, chat == null ? null : chat.getID()))
                .modelName(chatModelVo.getModelName())
                .tokenUsage(toTokenUsage(chat == null ? null : chat.getUsage()))
                .finishReason(FinishReason.STOP)
                .build();
        }

        private TokenUsage toTokenUsage(ChatUsage usage) {
            if (usage == null) {
                return null;
            }
            return new TokenUsage(
                usage.getInputTokens(),
                usage.getOutputTokens(),
                usage.getTokenCount()
            );
        }

        private RuntimeException toFailure(ChatEvent event) {
            Chat chat = event.getChat();
            if (chat != null && chat.getLastError() != null) {
                ChatError error = chat.getLastError();
                return new IllegalStateException(
                    StrUtil.format("Coze call failed: code={}, message={}", error.getCode(), error.getMsg())
                );
            }
            return new IllegalStateException("Coze call failed: " + event.getEvent());
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

        private void appendBlock(StringBuilder builder, String text) {
            if (StrUtil.isBlank(text)) {
                return;
            }
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(text);
        }

        private String normalizeBaseUrl(String baseUrl) {
            if (StrUtil.isBlank(baseUrl)) {
                throw new IllegalArgumentException("Coze apiHost cannot be blank");
            }
            String value = StrUtil.removeSuffix(baseUrl.trim(), "/");
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                value = "https://" + value;
            }
            return value;
        }

        private String requiredApiKey() {
            if (StrUtil.isBlank(chatModelVo.getApiKey())) {
                throw new IllegalArgumentException("Coze apiKey cannot be blank");
            }
            return chatModelVo.getApiKey();
        }

        private String requiredBotId() {
            if (StrUtil.isBlank(chatModelVo.getModelName())) {
                throw new IllegalArgumentException("Coze bot ID(modelName) cannot be blank");
            }
            return chatModelVo.getModelName();
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

        private String firstNotBlank(String first, String second) {
            return StrUtil.isNotBlank(first) ? first : second;
        }

        private void shutdown(CozeAPI coze) {
            if (coze == null) {
                return;
            }
            try {
                coze.shutdownExecutor();
            } catch (Exception e) {
                log.debug("Coze client shutdown failed: {}", e.getMessage());
            }
        }
    }
}
