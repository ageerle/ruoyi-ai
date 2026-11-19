package org.ruoyi.chat.controller.chat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.factory.AudioServiceFactory;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.chat.factory.VideoServiceFactory;
import org.ruoyi.common.chat.service.audio.IAudioGenerationService;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.service.image.IImageGenerationService;
import org.ruoyi.common.chat.service.video.IVideoGenerationService;
import org.ruoyi.chat.domain.bo.media.ImageGenerationRequest;
import org.ruoyi.chat.domain.vo.media.MediaModelOption;
import org.ruoyi.chat.service.media.AtlasPredictionService;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void imageModelsUseTheRequestedCategoryAndRegisteredAdapters() {
        var models = mock(IChatModelService.class);
        var context = mock(ApplicationContext.class);
        var openai = mock(IImageGenerationService.class);
        var newProvider = mock(IImageGenerationService.class);
        when(openai.getProviderName()).thenReturn("openai");
        when(newProvider.getProviderName()).thenReturn("new-image-provider");
        when(context.getBeansOfType(IImageGenerationService.class))
            .thenReturn(Map.of("openaiService", openai, "newImageService", newProvider));
        var images = new ImageServiceFactory();
        images.setApplicationContext(context);
        when(models.queryAvailableList(any(ChatModelBo.class)))
            .thenReturn(List.of(model("custom_api"), model("new-image-provider"), model("ark"), model("unregistered")));
        var controller = new MediaGenerationController(models, null, images, null, null);

        assertEquals(List.of(
            new MediaModelOption("test-image", null, "custom_api", true, null),
            new MediaModelOption("test-image", null, "new-image-provider", true, null),
            new MediaModelOption("test-image", null, "ark", true, null),
            new MediaModelOption("test-image", null, "unregistered", false, null)
        ), controller.models("image").getData());
        verify(models).queryAvailableList(argThat(query -> "image".equals(query.getCategory())));
    }

    @Test
    void supportedAudioAndVideoModelsExposeTheirResultQueryTypes() {
        var models = mock(IChatModelService.class);
        var context = mock(ApplicationContext.class);
        var atlas = mock(IAudioGenerationService.class);
        var newProvider = mock(IVideoGenerationService.class);
        when(atlas.getProviderName()).thenReturn("atlas");
        when(newProvider.getProviderName()).thenReturn("new-video-provider");
        when(context.getBeansOfType(IAudioGenerationService.class)).thenReturn(Map.of("atlasService", atlas));
        when(context.getBeansOfType(IVideoGenerationService.class)).thenReturn(Map.of("newVideoService", newProvider));
        var audio = new AudioServiceFactory();
        audio.setApplicationContext(context);
        var videos = new VideoServiceFactory();
        videos.setApplicationContext(context);
        var audioModel = model("atlas");
        audioModel.setCategory("audio");
        var videoModel = model("new-video-provider");
        videoModel.setCategory("video");
        when(models.queryAvailableList(argThat(query -> query != null && "audio".equals(query.getCategory()))))
            .thenReturn(List.of(audioModel));
        when(models.queryAvailableList(argThat(query -> query != null && "video".equals(query.getCategory()))))
            .thenReturn(List.of(videoModel));
        var controller = new MediaGenerationController(models, audio, null, videos, null);

        assertEquals(List.of(new MediaModelOption("test-image", null, "atlas", true, "prediction")),
            controller.models("audio").getData());
        assertEquals(List.of(new MediaModelOption("test-image", null, "new-video-provider", true, "video")),
            controller.models("video").getData());
    }

    @Test
    void rejectsOtherCategoriesBeforeQueryingModels() {
        var models = mock(IChatModelService.class);
        var controller = new MediaGenerationController(models, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> controller.models("chat"));
        verifyNoInteractions(models);
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
