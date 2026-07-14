package org.ruoyi.controller.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.common.chat.factory.AudioServiceFactory;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.chat.factory.VideoServiceFactory;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.domain.bo.media.ImageGenerationRequest;
import org.ruoyi.domain.bo.media.SpeechGenerationRequest;
import org.ruoyi.domain.bo.media.VideoGenerationRequest;
import org.ruoyi.enums.ModelType;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaGenerationController {

    private final IChatModelService chatModelService;
    private final AudioServiceFactory audioServiceFactory;
    private final ImageServiceFactory imageServiceFactory;
    private final VideoServiceFactory videoServiceFactory;
    private final AtlasPredictionService atlasPredictionService;

    @PostMapping("/speech")
    public R<MediaGenerationResponse> speech(@Valid @RequestBody SpeechGenerationRequest request) {
        ChatModelVo model = loadModel(request.getModel(), ModelType.AUDIO.getKey());
        MediaGenerationResponse response = audioServiceFactory.getOriginalService(model.getProviderCode())
            .generateSpeech(AudioContext.builder()
                .chatModelVo(model)
                .input(request.getInput())
                .voice(request.getVoice())
                .responseFormat(request.getResponseFormat())
                .speed(request.getSpeed())
                .instructions(request.getInstructions())
                .build());
        return R.ok(response);
    }

    @PostMapping("/image")
    public R<MediaGenerationResponse> image(@Valid @RequestBody ImageGenerationRequest request) {
        ChatModelVo model = loadModel(request.getModel(), ModelType.IMAGE.getKey());
        String result = imageServiceFactory.getOriginalService(model.getProviderCode())
            .generateImage(ImageContext.builder()
                .chatModelVo(model)
                .prompt(request.getPrompt())
                .size(request.getSize())
                .seed(request.getSeed())
                .build());
        return R.ok(toImageResponse(result));
    }

    @PostMapping("/video")
    public R<MediaGenerationResponse> video(@Valid @RequestBody VideoGenerationRequest request) {
        ChatModelVo model = loadModel(request.getModel(), ModelType.VIDEO.getKey());
        MediaGenerationResponse response = videoServiceFactory.getOriginalService(model.getProviderCode())
            .generateVideo(VideoContext.builder()
                .chatModelVo(model)
                .prompt(request.getPrompt())
                .size(request.getSize())
                .seconds(request.getSeconds())
                .quality(request.getQuality())
                .build());
        return R.ok(response);
    }

    @GetMapping("/video")
    public R<MediaGenerationResponse> videoResult(@NotBlank(message = "模型不能为空") @RequestParam String model,
                                                  @NotBlank(message = "videoId不能为空") @RequestParam String videoId) {
        ChatModelVo chatModelVo = loadModel(model, ModelType.VIDEO.getKey());
        MediaGenerationResponse response = videoServiceFactory.getOriginalService(chatModelVo.getProviderCode())
            .retrieveVideo(VideoContext.builder()
                .chatModelVo(chatModelVo)
                .prompt("retrieve")
                .videoId(videoId)
                .build());
        return R.ok(response);
    }

    @GetMapping("/prediction")
    public R<MediaGenerationResponse> prediction(@NotBlank(message = "模型不能为空") @RequestParam String model,
                                                 @NotBlank(message = "predictionId不能为空") @RequestParam String predictionId) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(model);
        if (chatModelVo == null) {
            throw new IllegalArgumentException("未找到模型配置: " + model);
        }
        return R.ok(atlasPredictionService.retrieve(chatModelVo, predictionId));
    }

    private ChatModelVo loadModel(String modelName, String category) {
        ChatModelVo model = chatModelService.selectModelByName(modelName);
        if (model == null) {
            throw new IllegalArgumentException("未找到模型配置: " + modelName);
        }
        if (!category.equals(model.getCategory())) {
            throw new IllegalArgumentException("模型分类不匹配，期望: " + category + ", 实际: " + model.getCategory());
        }
        return model;
    }

    private MediaGenerationResponse toImageResponse(String result) {
        if (result != null && result.startsWith("{")) {
            try {
                return atlasPredictionService.toResponse(result, "image");
            } catch (Exception e) {
                throw new RuntimeException("图片生成响应解析失败: " + e.getMessage(), e);
            }
        }
        if (result != null && result.startsWith("data:")) {
            String mimeType = result.substring("data:".length(), result.indexOf(";base64,"));
            String b64 = result.substring(result.indexOf(";base64,") + ";base64,".length());
            return MediaGenerationResponse.builder()
                .type("image")
                .mimeType(mimeType)
                .b64Json(b64)
                .dataUrl(result)
                .build();
        }
        return MediaGenerationResponse.builder()
            .type("image")
            .mimeType("image/png")
            .url(result)
            .build();
    }
}
