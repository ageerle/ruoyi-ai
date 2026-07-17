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
        ObjectNode payload = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", model.getModelName());
        payload.put("prompt", videoContext.getPrompt());
        if (StrUtil.isNotBlank(videoContext.getSize())) {
            payload.put("size", videoContext.getSize());
        }
        if (videoContext.getSeconds() != null) {
            payload.put("duration", videoContext.getSeconds());
        }
        if (StrUtil.isNotBlank(videoContext.getQuality())) {
            payload.put("quality", videoContext.getQuality());
        }
        java.util.List<String> refImages = videoContext.getReferenceImages();
        if (refImages != null && !refImages.isEmpty()) {
            com.fasterxml.jackson.databind.node.ArrayNode arr = payload.putArray("reference_images");
            for (String url : refImages) {
                arr.add(url);
            }
        } else if (StrUtil.isNotBlank(videoContext.getImageUrl())) {
            payload.put("image_url", videoContext.getImageUrl());
        }

        // 同步音频生成（环境音/动效）
        if (videoContext.getGenerateAudio() != null) {
            payload.put("generate_audio", videoContext.getGenerateAudio());
        }
        // 参考音频（对白口型对齐）
        java.util.List<String> refAudios = videoContext.getReferenceAudios();
        if (refAudios != null && !refAudios.isEmpty()) {
            com.fasterxml.jackson.databind.node.ArrayNode arr = payload.putArray("reference_audios");
            for (String url : refAudios) {
                arr.add(url);
            }
        }
        // 返回末帧（同场景连续镜头首帧承接用）
        if (videoContext.getReturnLastFrame() != null) {
            payload.put("return_last_frame", videoContext.getReturnLastFrame());
        }

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

    @Override
    protected MediaGenerationResponse doRetrieveVideo(VideoContext videoContext) {
        return atlasPredictionService.retrieve(videoContext.getChatModelVo(), videoContext.getVideoId());
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ATLAS.getCode();
    }
}
