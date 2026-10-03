package org.ruoyi.service.video.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import org.ruoyi.service.video.AbstractVideoGenerationService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("atlasVideo")
@RequiredArgsConstructor
public class AtlasVideoGenerationServiceImpl extends AbstractVideoGenerationService {

    private final AtlasPredictionService atlasPredictionService;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    @Override
    protected MediaGenerationResponse doGenerateVideo(VideoContext videoContext) {
        ChatModelVo model = videoContext.getChatModelVo();
        ObjectNode payload = buildPayload(videoContext);
        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(model.getApiHost(), "/model/generateVideo"))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString(), AtlasMediaSupport.JSON))
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("Atlas Cloud视频生成任务创建失败: " + response.code() + " - " + responseText);
            }
            return atlasPredictionService.toResponse(responseText, "video");
        } catch (IOException e) {
            throw new RuntimeException("Atlas Cloud视频生成任务创建失败: " + e.getMessage(), e);
        }
    }

    static ObjectNode buildPayload(VideoContext videoContext) {
        ChatModelVo model = videoContext.getChatModelVo();
        boolean seedanceMini = model.getModelName().startsWith("bytedance/seedance-2.0-mini/");
        ObjectNode payload = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", model.getModelName());
        Integer requestSeconds = requestDuration(model.getModelName(), videoContext.getSeconds());
        String prompt = videoContext.getPrompt();
        if (model.getModelName().startsWith("bytedance/seedance-2.5/")) prompt = prompt.replaceAll("@image(\\d+)", "@Image$1").replaceAll("@audio(\\d+)", "@Audio$1");
        payload.put("prompt", prompt);
        if (StrUtil.isNotBlank(videoContext.getSize())) {
            payload.put(model.getModelName().startsWith("bytedance/seedance-") ? "ratio" : "size", videoContext.getSize());
        }
        if (videoContext.getSeconds() != null) {
            payload.put("duration", requestSeconds);
        }
        if (StrUtil.isNotBlank(videoContext.getQuality())) {
            payload.put("quality", videoContext.getQuality());
        }
        java.util.List<String> refImages = videoContext.getReferenceImages();
        if (seedanceMini && refImages != null && refImages.size() > 9) {
            throw new IllegalArgumentException("Seedance 2.0 Mini最多9张参考图");
        }
        if (model.getModelName().startsWith("bytedance/seedance-") && model.getModelName().endsWith("/image-to-video") && refImages != null && !refImages.isEmpty()) {
            payload.put("image",refImages.get(0));
        } else if (refImages != null && !refImages.isEmpty()) {
            com.fasterxml.jackson.databind.node.ArrayNode arr = payload.putArray("reference_images");
            for (String url : refImages) {
                arr.add(url);
            }
        } else if (StrUtil.isNotBlank(videoContext.getImageUrl())) {
            if (seedanceMini && model.getModelName().endsWith("/reference-to-video")) {
                payload.putArray("reference_images").add(videoContext.getImageUrl());
            } else {
                payload.put("image_url", videoContext.getImageUrl());
            }
        }

        if (seedanceMini) {
            // Mini has native 480p/720p and separate SR outputs; never silently request an unsupported native 1080p.
            String resolution = StrUtil.blankToDefault(videoContext.getResolution(), "720p");
            String providerResolution = switch (resolution.toLowerCase(java.util.Locale.ROOT)) {
                case "480p" -> "480p";
                case "720p" -> "720p";
                case "720p-sr" -> "720p-SR";
                case "1080p-sr" -> "1080p-SR";
                case "1440p-sr" -> "1440p-SR";
                default -> throw new IllegalArgumentException("Seedance 2.0 Mini不支持该输出分辨率: " + resolution);
            };
            payload.put("resolution", providerResolution);
        }

        if (model.getModelName().startsWith("bytedance/seedance-2.5/")) {
            String resolution = StrUtil.blankToDefault(videoContext.getResolution(), "720p");
            if (!java.util.Set.of("480p", "720p", "720p-sr", "720p-esr", "1080p", "1080p-sr",
                    "1080p-esr", "1080p-esr & 60fps", "1440p-sr", "1440p-esr", "4k-esr").contains(resolution)) {
                throw new IllegalArgumentException("Seedance 2.5不支持该输出分辨率: " + resolution);
            }
            payload.put("resolution", resolution);
            payload.put("output_format", "mp4");
            if (refImages != null && refImages.size() > 30) throw new IllegalArgumentException("Seedance 2.5最多30张参考图");
            if (refImages != null && !refImages.isEmpty()) payload.put("omni_reference_task_type", "reference");
        }

        // 同步音频生成（环境音/动效）
        if (videoContext.getGenerateAudio() != null) {
            payload.put("generate_audio", videoContext.getGenerateAudio());
        }
        // 参考音频（对白口型对齐）
        java.util.List<String> refAudios = videoContext.getReferenceAudios();
        if (refAudios != null && !refAudios.isEmpty()) {
            if (seedanceMini) {
                if (refAudios.size() > 3) throw new IllegalArgumentException("Seedance 2.0 Mini最多3条参考音频，总时长须不超过15秒");
                if (!payload.has("reference_images") && !payload.has("image")) {
                    throw new IllegalArgumentException("Seedance 2.0 Mini参考音频需要至少1张参考图");
                }
            }
            if (model.getModelName().startsWith("bytedance/seedance-2.5/") && refAudios.size() > 10) throw new IllegalArgumentException("Seedance 2.5最多10条参考音频");
            com.fasterxml.jackson.databind.node.ArrayNode arr = payload.putArray("reference_audios");
            for (String url : refAudios) {
                arr.add(url);
            }
        }
        // 返回末帧（同场景连续镜头首帧承接用）
        if (videoContext.getReturnLastFrame() != null) {
            payload.put("return_last_frame", videoContext.getReturnLastFrame());
        }

        return payload;
    }

    static Integer requestDuration(String model, Integer seconds) {
        if (seconds == null) return null;
        if (seconds != -1 && seconds <= 0) throw new IllegalArgumentException("视频秒数须为正整数或-1");
        // Preserve an explicit user choice. Provider limits and defaults belong to the selected model.
        return seconds;
    }

    @Override
    protected MediaGenerationResponse doRetrieveVideo(VideoContext videoContext) {
        return atlasPredictionService.retrieve(videoContext.getChatModelVo(), videoContext.getVideoId());
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ATLAS.getCode();
    }
}
