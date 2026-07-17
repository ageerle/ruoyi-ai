package org.ruoyi.service.audio.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.audio.AbstractAudioGenerationService;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Atlas Cloud 音频生成（bytedance/seed-audio-1.0）。异步：提交 /model/generateAudio 返回 predictionId，
 * 轮询 /model/prediction/{id} 拿音频 URL。支持 references（speaker 音色 + 参考音频）做多角色对白配音。
 */
@Slf4j
@Component("atlasAudio")
@RequiredArgsConstructor
public class AtlasAudioGenerationServiceImpl extends AbstractAudioGenerationService {

    private final AtlasPredictionService atlasPredictionService;

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    @Override
    protected MediaGenerationResponse doGenerateSpeech(AudioContext audioContext) {
        ChatModelVo model = audioContext.getChatModelVo();
        ObjectNode payload = AtlasMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", model.getModelName());
        payload.put("text", audioContext.getInput());
        String format = StrUtil.blankToDefault(audioContext.getResponseFormat(), "mp3");
        payload.put("format", format);

        // 参考资源：多角色音色 + 参考音频
        List<Map<String, String>> refs = audioContext.getReferences();
        if (refs != null && !refs.isEmpty()) {
            ArrayNode arr = payload.putArray("references");
            for (Map<String, String> ref : refs) {
                ObjectNode r = arr.addObject();
                if (StrUtil.isNotBlank(ref.get("speaker"))) r.put("speaker", ref.get("speaker"));
                if (StrUtil.isNotBlank(ref.get("audioUrl"))) r.put("audio_url", ref.get("audioUrl"));
                if (StrUtil.isNotBlank(ref.get("audioData"))) r.put("audio_data", ref.get("audioData"));
                if (StrUtil.isNotBlank(ref.get("imageData"))) r.put("image_data", ref.get("imageData"));
            }
        } else if (StrUtil.isNotBlank(audioContext.getVoice())) {
            // 没有显式 references 但指定了 voice 音色名：作为单一 speaker
            ArrayNode arr = payload.putArray("references");
            ObjectNode r = arr.addObject();
            r.put("speaker", audioContext.getVoice());
        }

        if (audioContext.getSampleRate() != null) payload.put("sample_rate", audioContext.getSampleRate());
        if (audioContext.getPitchRate() != null) payload.put("pitch_rate", audioContext.getPitchRate());
        if (audioContext.getSpeechRate() != null) payload.put("speech_rate", audioContext.getSpeechRate());
        if (audioContext.getLoudnessRate() != null) payload.put("loudness_rate", audioContext.getLoudnessRate());

        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(model.getApiHost(), "/model/generateAudio"))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString(), AtlasMediaSupport.JSON))
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("Atlas Cloud 音频生成任务创建失败: " + response.code() + " - " + responseText);
            }
            return atlasPredictionService.toResponse(responseText, "audio");
        } catch (IOException e) {
            throw new RuntimeException("Atlas Cloud 音频生成任务创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ATLAS.getCode();
    }
}
