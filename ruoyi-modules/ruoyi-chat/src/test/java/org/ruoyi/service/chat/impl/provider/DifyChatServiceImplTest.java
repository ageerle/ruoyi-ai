package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.github.imfangs.dify.client.DifyChatClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.model.DifyConfig;
import io.github.imfangs.dify.client.model.chat.ChatMessageResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Tag("dev")
class DifyChatServiceImplTest {

    private final DifyChatServiceImpl service = new DifyChatServiceImpl();

    @Test
    void getProviderName_isDify() {
        assertEquals(ChatModeType.DIFY.getCode(), service.getProviderName());
    }

    @Test
    void buildStreamingChatModel_returnsModel() {
        StreamingChatModel model = service.buildStreamingChatModel(modelVo(), new ChatRequest());

        assertNotNull(model);
    }

    @Test
    void buildChatModel_returnsModel() {
        ChatModel model = service.buildChatModel(modelVo());

        assertNotNull(model);
    }

    @Test
    void blockingCallPassesAdminConfiguredAppKeyAndCloudBaseUrlToSdk() throws Exception {
        var config = modelVo();
        var client = mock(DifyChatClient.class);
        when(client.sendChatMessage(any())).thenReturn(ChatMessageResponse.builder()
            .answer("Hello from Dify").messageId("message-1").build());
        try (var factory = mockStatic(DifyClientFactory.class)) {
            factory.when(() -> DifyClientFactory.createChatClient(any(DifyConfig.class))).thenReturn(client);
            var response = service.buildChatModel(config).doChat(request());
            assertEquals("Hello from Dify", response.aiMessage().text());
            factory.verify(() -> DifyClientFactory.createChatClient(argThat(value ->
                "https://api.dify.ai/v1".equals(value.getBaseUrl())
                    && "app-test-key".equals(value.getApiKey()))));
            verify(client).sendChatMessage(argThat(message -> message.getResponseMode() == ResponseMode.BLOCKING
                && message.getInputs().isEmpty()));
        }
    }

    @Test
    void streamingCallPassesAdminConfiguredAppKeyAndDeliversResponse() throws Exception {
        var config = modelVo();
        config.setApiHost("https://api.dify.ai/v1/");
        var client = mock(DifyChatClient.class);
        doAnswer(invocation -> {
            ChatStreamCallback callback = invocation.getArgument(1);
            var chunk = new MessageEvent();
            chunk.setAnswer("Hello from Dify");
            callback.onMessage(chunk);
            callback.onMessageEnd(new MessageEndEvent());
            return null;
        }).when(client).sendChatMessageStream(any(), any());
        var handler = mock(StreamingChatResponseHandler.class);
        try (var factory = mockStatic(DifyClientFactory.class)) {
            factory.when(() -> DifyClientFactory.createChatClient(any(DifyConfig.class))).thenReturn(client);
            service.buildStreamingChatModel(config, new ChatRequest()).doChat(request(), handler);
            factory.verify(() -> DifyClientFactory.createChatClient(argThat(value ->
                "https://api.dify.ai/v1".equals(value.getBaseUrl())
                    && "app-test-key".equals(value.getApiKey()))));
            verify(client).sendChatMessageStream(argThat(message -> message.getResponseMode() == ResponseMode.STREAMING), any());
            verify(handler).onPartialResponse("Hello from Dify");
            var response = ArgumentCaptor.forClass(ChatResponse.class);
            verify(handler).onCompleteResponse(response.capture());
            assertEquals("Hello from Dify", response.getValue().aiMessage().text());
            verify(handler, never()).onError(any());
        }
    }

    private dev.langchain4j.model.chat.request.ChatRequest request() {
        return dev.langchain4j.model.chat.request.ChatRequest.builder()
            .messages(UserMessage.from("Hello")).build();
    }

    private ChatModelVo modelVo() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setProviderCode(ChatModeType.DIFY.getCode());
        modelVo.setModelName("dify-chat");
        modelVo.setApiHost("https://api.dify.ai/v1");
        modelVo.setApiKey("app-test-key");
        return modelVo;
    }
}
