package org.ruoyi.chat.service.chat.impl;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.ruoyi.chat.service.chat.IChatMessageService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.sse.dto.SseEventDto;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ChatServiceFacadeTerminalTest {

    private final IChatMessageService messages = mock(IChatMessageService.class);
    private final SseEmitterManager streams = mock(SseEmitterManager.class);
    private final SseEmitter emitter = new SseEmitter();

    @Test
    void timeoutPersistsPartialFailureAndSendsOneTerminalEvenWithLateCallbacks() {
        var handler = handler();
        handler.onPartialResponse("已经生成的片段");
        handler.onError(new TimeoutException());
        handler.onError(new IllegalStateException());
        handler.onPartialResponse("迟到片段");
        handler.onCompleteResponse(null);
        verify(messages).saveChatMessage(eq(1L), eq(2L), contains("响应未完成"), eq("assistant"), eq("atlas-model"));
        verify(streams).sendLocalEvent(eq("2"), same(emitter), argThat(event ->
            "error".equals(event.getEvent()) && Boolean.TRUE.equals(event.getDone())));
        verify(streams, never()).sendLocalEvent("2", emitter, SseEventDto.done());
        verify(streams, never()).sendLocalEvent("2", emitter, SseEventDto.content("迟到片段"));
    }

    @Test
    void successfulCompletionSavesAndClosesOnlyOnce() {
        var handler = handler();
        handler.onPartialResponse("完整正文");
        handler.onCompleteResponse(null);
        handler.onCompleteResponse(null);
        handler.onError(new TimeoutException());
        verify(messages).saveChatMessage(1L, 2L, "完整正文", "assistant", "atlas-model");
        verify(streams).sendLocalEvent("2", emitter, SseEventDto.done());
    }

    @Test
    void emptyCompletionIsAnErrorInsteadOfSuccessfulEmptyManuscript() {
        handler().onCompleteResponse(null);
        verify(messages).saveChatMessage(1L, 2L, ChatServiceFacade.SAFE_CHAT_ERROR_MESSAGE, "assistant", "atlas-model");
        verify(streams).sendLocalEvent(eq("2"), same(emitter), argThat(event ->
            "error".equals(event.getEvent()) && Boolean.TRUE.equals(event.getDone())));
        verify(streams, never()).sendLocalEvent("2", emitter, SseEventDto.done());
    }

    private StreamingChatResponseHandler handler() {
        var request = new ChatRequest();
        request.setUserId(1L);
        request.setSessionId(2L);
        request.setModel("atlas-model");
        request.setEmitter(emitter);
        when(streams.isCurrent("2", emitter)).thenReturn(true);
        var facade = new ChatServiceFacade(null, null, null, null, streams, messages, null, null, null, null, null, null);
        return ReflectionTestUtils.invokeMethod(facade, "createModelChatResponseHandler", request, null);
    }
}
