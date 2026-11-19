package org.ruoyi.chat.service.chat.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.ruoyi.chat.factory.ChatServiceFactory;
import org.ruoyi.chat.mapper.ChatMessageImageStore.StoredImage;
import org.ruoyi.chat.service.chat.AbstractChatService;
import org.ruoyi.chat.service.chat.ChatImageService;
import org.ruoyi.chat.service.chat.IChatMessageService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ChatServiceFacadeImageTest {

    private static final long USER_ID = 1L;
    private static final AtomicLong NEXT_SESSION = new AtomicLong(1800L);
    private static final long MESSAGE_ID = 5001L;
    private static final String MODEL = "atlas-model";

    private final long sessionId = NEXT_SESSION.incrementAndGet();
    private final IChatModelService modelService = mock(IChatModelService.class);
    private final IChatMessageService messages = mock(IChatMessageService.class);
    private final SseEmitterManager streams = mock(SseEmitterManager.class);
    private final ChatImageService images = mock(ChatImageService.class);
    private final ChatServiceFactory factory = mock(ChatServiceFactory.class);
    private final SseEmitter emitter = new SseEmitter();
    private final ChatServiceFacade facade = new ChatServiceFacade(
        modelService, factory, null, null, streams, messages, images, null, null, null, null, null);

    @Test
    void savesImageOnlyMessageBeforeModelStartsAndUsesDefaultPrompt() {
        ChatRequest request = request("");
        request.getChatModelVo().setSupportsVision(true);
        var model = visionModel(request, request.getChatModelVo());

        Runnable work = startImageChat(request);

        verify(images, never()).load(anyLong());
        verifyNoInteractions(model);
        assertNull(request.getImage());

        work.run();

        assertTrue(request.isImageInput());
        verify(model).chat(argThat((List<ChatMessage> list) ->
            isOriginalImageTurn(list.get(list.size() - 1), "请描述这张图片。")), any());
    }

    @Test
    void selectedMarkedVisionModelGetsQuestionHistoryAndOriginalImage() {
        ChatRequest request = request("这张图是什么颜色？");
        request.getChatModelVo().setProviderCode("ark");
        request.getChatModelVo().setSupportsVision(true);
        when(messages.getMessagesBySessionId(sessionId)).thenReturn(
            List.of(UserMessage.from("之前的问题"), AiMessage.from("之前的回答")));
        var model = visionModel(request, request.getChatModelVo());

        startImageChat(request).run();

        verifyNoInteractions(modelService);
        verify(images, never()).defaultVisionModelName();
        verify(model).chat(argThat((List<ChatMessage> list) ->
            list.size() == 3 && list.get(0).equals(UserMessage.from("之前的问题"))
                && isOriginalImageTurn(list.get(2), "这张图是什么颜色？")), any());
    }

    @Test
    void imageUsesConfiguredDefaultWhenSelectedModelIsNotMarked() {
        ChatRequest request = request("图片中有哪些物体？");
        var visionModel = defaultVisionModel(true);
        when(images.defaultVisionModelName()).thenReturn(visionModel.getModelName());
        when(modelService.selectModelByName(visionModel.getModelName())).thenReturn(visionModel);
        var model = visionModel(request, visionModel);

        startImageChat(request).run();

        assertEquals("ark-vision", request.getModel());
        verify(model).chat(argThat((List<ChatMessage> list) ->
            isOriginalImageTurn(list.get(list.size() - 1), "图片中有哪些物体？")), any());
    }

    @Test
    void stopsWithErrorWhenDefaultVisionModelIsMissing() {
        ChatRequest request = request("这是什么？");
        when(streams.isCurrent(Long.toString(sessionId), emitter)).thenReturn(true);

        startImageChat(request).run();

        verifyImageModelError("配置默认识图模型");
    }

    @Test
    void refusesDefaultVisionModelWhenItsFlagIsOff() {
        ChatRequest request = request("这是什么？");
        var visionModel = defaultVisionModel(false);
        when(images.defaultVisionModelName()).thenReturn(visionModel.getModelName());
        when(modelService.selectModelByName(visionModel.getModelName())).thenReturn(visionModel);
        when(streams.isCurrent(Long.toString(sessionId), emitter)).thenReturn(true);

        startImageChat(request).run();

        verifyImageModelError("支持识图");
    }

    @Test
    void givesActionableErrorForDefaultModelWithDisabledProvider() {
        ChatRequest request = request("这是什么？");
        when(images.defaultVisionModelName()).thenReturn("ark-vision");
        when(modelService.selectModelByName("ark-vision")).thenThrow(new ServiceException("provider disabled"));
        when(streams.isCurrent(Long.toString(sessionId), emitter)).thenReturn(true);

        startImageChat(request).run();

        verifyImageModelError("厂商配置");
    }

    @Test
    void canceledStreamKeepsOriginalAndSkipsModel() {
        ChatRequest request = request("");

        startImageChat(request).run();

        verify(images, never()).load(anyLong());
        verifyNoInteractions(factory);
    }

    private ChatModelVo defaultVisionModel(boolean enabled) {
        var model = new ChatModelVo();
        model.setModelName("ark-vision");
        model.setProviderCode("ark");
        model.setCategory("chat");
        model.setSupportsVision(enabled);
        return model;
    }

    private void verifyImageModelError(String message) {
        verify(images, never()).load(anyLong());
        verifyNoInteractions(factory);
        verify(streams).sendLocalEvent(eq(Long.toString(sessionId)), same(emitter), argThat(event ->
            "error".equals(event.getEvent()) && Boolean.TRUE.equals(event.getDone()) && event.getError().contains(message)));
    }

    private StreamingChatModel visionModel(ChatRequest request, ChatModelVo config) {
        var provider = mock(AbstractChatService.class);
        var model = mock(StreamingChatModel.class);
        when(factory.getOriginalService(config.getProviderCode())).thenReturn(provider);
        when(provider.buildStreamingChatModel(config, request)).thenReturn(model);
        when(streams.isCurrent(Long.toString(sessionId), emitter)).thenReturn(true);
        return model;
    }

    private boolean isOriginalImageTurn(ChatMessage message, String question) {
        return message instanceof UserMessage userMessage
            && userMessage.contents().size() == 2
            && userMessage.contents().get(0).equals(TextContent.from(question))
            && userMessage.contents().get(1).equals(ImageContent.from("AQID", "image/png"));
    }

    private Runnable startImageChat(ChatRequest request) {
        var image = new StoredImage("image/png", new byte[]{1, 2, 3});
        when(images.decode(request.getImage())).thenReturn(image);
        when(images.load(MESSAGE_ID)).thenReturn(image);
        when(messages.saveUserImageMessage(USER_ID, sessionId, request.getContent(), MODEL, image))
            .thenReturn(MESSAGE_ID);
        String originalContent = request.getContent();

        ReflectionTestUtils.invokeMethod(facade, "startImageChat", request, null);

        var work = ArgumentCaptor.forClass(Runnable.class);
        var ordered = inOrder(messages, images);
        ordered.verify(messages).saveUserImageMessage(USER_ID, sessionId, originalContent, MODEL, image);
        ordered.verify(images).execute(work.capture());
        return work.getValue();
    }

    private ChatRequest request(String originalContent) {
        var request = new ChatRequest();
        request.setUserId(USER_ID);
        request.setSessionId(sessionId);
        request.setModel(MODEL);
        request.setEmitter(emitter);
        request.setContent(originalContent);
        request.setImage(new ChatRequest.Image());
        var model = new ChatModelVo();
        model.setModelName(MODEL);
        model.setProviderCode("custom_api");
        model.setCategory("chat");
        request.setChatModelVo(model);
        return request;
    }
}
