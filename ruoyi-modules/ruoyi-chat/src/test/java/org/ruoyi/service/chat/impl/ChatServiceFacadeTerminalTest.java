package org.ruoyi.service.chat.impl;

import cn.hutool.extra.spring.SpringUtil;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.utils.SseMessageUtils;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ChatServiceFacadeTerminalTest {
    @BeforeAll static void initializeSseUtility() {
        var context = new GenericApplicationContext();
        context.registerBean(SseEmitterManager.class, () -> mock(SseEmitterManager.class));
        context.refresh(); new SpringUtil().setApplicationContext(context);
    }

    @Test void timeoutPersistsPartialFailureAndSendsOneTerminalEvenWithLateCallbacks() {
        var messages = mock(IChatMessageService.class); var request = request(); var handler = handler(messages, request);
        try (var sse = mockStatic(SseMessageUtils.class)) {
            handler.onPartialResponse("已经生成的片段"); handler.onError(new TimeoutException());
            handler.onError(new IllegalStateException()); handler.onPartialResponse("迟到片段"); handler.onCompleteResponse(null);
            verify(messages, times(1)).saveChatMessage(eq(1L), eq(2L), contains("响应未完成"), eq("assistant"), eq("atlas-model"));
            sse.verify(() -> SseMessageUtils.sendErrorAndComplete("2", ChatServiceFacade.SAFE_CHAT_ERROR_MESSAGE), times(1));
            sse.verify(() -> SseMessageUtils.sendDone("2"), never());
            sse.verify(() -> SseMessageUtils.sendContent("2", "迟到片段"), never());
        }
    }

    @Test void successfulCompletionSavesAndClosesOnlyOnce() {
        var messages = mock(IChatMessageService.class); var handler = handler(messages, request());
        try (var sse = mockStatic(SseMessageUtils.class)) {
            handler.onPartialResponse("完整正文"); handler.onCompleteResponse(null); handler.onCompleteResponse(null); handler.onError(new TimeoutException());
            verify(messages, times(1)).saveChatMessage(1L, 2L, "完整正文", "assistant", "atlas-model");
            sse.verify(() -> SseMessageUtils.sendDone("2"), times(1));
            sse.verify(() -> SseMessageUtils.sendErrorAndComplete(anyString(), anyString()), never());
        }
    }

    @Test void emptyCompletionIsAnErrorInsteadOfSuccessfulEmptyManuscript() {
        var messages = mock(IChatMessageService.class); var handler = handler(messages, request());
        try (var sse = mockStatic(SseMessageUtils.class)) {
            handler.onCompleteResponse(null);
            verify(messages).saveChatMessage(1L, 2L, ChatServiceFacade.SAFE_CHAT_ERROR_MESSAGE, "assistant", "atlas-model");
            sse.verify(() -> SseMessageUtils.sendErrorAndComplete("2", ChatServiceFacade.SAFE_CHAT_ERROR_MESSAGE));
            sse.verify(() -> SseMessageUtils.sendDone("2"), never());
        }
    }

    private static ChatRequest request() {
        var request = new ChatRequest(); request.setUserId(1L); request.setSessionId(2L); request.setModel("atlas-model"); return request;
    }
    private static StreamingChatResponseHandler handler(IChatMessageService messages, ChatRequest request) {
        var facade = new ChatServiceFacade(null, null, null, null, null, messages, null, null, null, null, null, null);
        return ReflectionTestUtils.invokeMethod(facade, "createModelChatResponseHandler", request, null, null);
    }
}
