package org.ruoyi.controller.chat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.image.IImageGenerationService;
import org.ruoyi.domain.bo.media.ImageGenerationRequest;
import org.ruoyi.service.media.AtlasPredictionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class MediaGenerationControllerTest {
    @Test
    void returnsAtlasTaskWithoutBlockingForImage() {
        var models = mock(IChatModelService.class);
        var factory = mock(ImageServiceFactory.class);
        var service = mock(IImageGenerationService.class);
        var model = model("atlas");
        when(models.selectModelByName("test-image")).thenReturn(model);
        when(factory.getOriginalService("atlas")).thenReturn(service);
        when(service.startImageGeneration(any())).thenReturn(MediaGenerationResponse.builder()
            .id("task-123").type("image").status("pending").build());
        var controller = new MediaGenerationController(models, null, factory, null, new AtlasPredictionService());
        var result = controller.image(request()).getData();
        assertEquals("task-123", result.getId());
        assertEquals("pending", result.getStatus());
        verify(service, never()).generateImage(any());
    }

    @Test
    void preservesOtherProvidersBase64ImageResponse() {
        var models = mock(IChatModelService.class);
        var factory = mock(ImageServiceFactory.class);
        var service = mock(IImageGenerationService.class);
        when(models.selectModelByName("test-image")).thenReturn(model("openai"));
        when(factory.getOriginalService("openai")).thenReturn(service);
        when(service.generateImage(any())).thenReturn("data:image/png;base64,aGVsbG8=");
        var controller = new MediaGenerationController(models, null, factory, null, new AtlasPredictionService());
        var result = controller.image(request()).getData();
        assertEquals("image/png", result.getMimeType());
        assertEquals("aGVsbG8=", result.getB64Json());
        verify(service, never()).startImageGeneration(any());
    }

    private ChatModelVo model(String provider) {
        var model = new ChatModelVo();
        model.setModelName("test-image");
        model.setProviderCode(provider);
        model.setCategory("image");
        return model;
    }

    private ImageGenerationRequest request() {
        var request = new ImageGenerationRequest();
        request.setModel("test-image");
        request.setPrompt("a blue book");
        return request;
    }
}
