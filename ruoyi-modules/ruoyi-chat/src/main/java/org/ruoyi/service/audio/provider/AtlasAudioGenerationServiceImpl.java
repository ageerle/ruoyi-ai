package org.ruoyi.service.audio.provider;

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
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.audio.AbstractAudioGenerationService;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.service.media.AtlasPredictionService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Atlas Cloud TTS：Gemini 单音色与 Seed Audio references 使用各自的请求字段。
 * 提交返回 predictionId，轮询 /model/prediction/{id} 获取音频；Gemini 付费 POST 不自动重试。
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

    private final OkHttpClient geminiHttpClient = okHttpClient.newBuilder()
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .build();

    @Override
    protected MediaGenerationResponse doGenerateSpeech(AudioContext audioContext) {
        ChatModelVo model = audioContext.getChatModelVo();
        boolean gemini = AtlasSpeechPayloadBuilder.isGeminiTts(model.getModelName());
        ObjectNode payload = AtlasSpeechPayloadBuilder.build(audioContext);

        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(model.getApiHost(), "/model/generateAudio"))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString(), AtlasMediaSupport.JSON))
            .build();
        try (Response response = (gemini ? geminiHttpClient : okHttpClient).newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("Atlas Cloud 音频生成任务创建失败: " + response.code() + " - " + responseText);
            }
            if (!gemini) return atlasPredictionService.toResponse(responseText, "audio");
            // Public documentation also shows a root-level prediction object.
            var root = AtlasMediaSupport.OBJECT_MAPPER.readTree(responseText);
            String normalized = root != null && root.isObject() && !root.has("data") && root.has("id")
                ? AtlasMediaSupport.OBJECT_MAPPER.createObjectNode().set("data", root).toString() : responseText;
            MediaGenerationResponse result = atlasPredictionService.toResponse(normalized, "audio");
            if (result == null || (StrUtil.isBlank(result.getId()) && StrUtil.isBlank(result.getUrl()))) {
                throw new IllegalStateException("Atlas 音频提交结果未知：未返回任务ID或音频URL，请先核对上游任务，禁止重复提交");
            }
            result.setRawResponse(responseText);
            return result;
        } catch (IOException e) {
            if (!gemini) throw new RuntimeException("Atlas Cloud 音频生成任务创建失败: " + e.getMessage(), e);
            throw new RuntimeException("Atlas 音频提交结果未知，请先核对上游任务，禁止重复提交: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ATLAS.getCode();
    }
}
