package org.ruoyi.service.audio.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.ruoyi.service.media.OpenAiMediaSupport;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("openaiAudio")
public class OpenAiAudioGenerationServiceImpl extends AbstractAudioGenerationService {

    private static final String DEFAULT_VOICE = "alloy";
    private static final String DEFAULT_FORMAT = "mp3";

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    @Override
    protected MediaGenerationResponse doGenerateSpeech(AudioContext audioContext) {
        ChatModelVo model = audioContext.getChatModelVo();
        String format = StrUtil.blankToDefault(audioContext.getResponseFormat(), DEFAULT_FORMAT);
        String mimeType = mimeType(format);
        ObjectNode payload = OpenAiMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", model.getModelName());
        payload.put("input", audioContext.getInput());
        payload.put("voice", StrUtil.blankToDefault(audioContext.getVoice(), DEFAULT_VOICE));
        payload.put("response_format", format);
        if (audioContext.getSpeed() != null) {
            payload.put("speed", audioContext.getSpeed());
        }
        if (StrUtil.isNotBlank(audioContext.getInstructions())) {
            payload.put("instructions", audioContext.getInstructions());
        }

        try {
            byte[] bytes = postBytes(model, "/audio/speech", payload.toString());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            return MediaGenerationResponse.builder()
                .type("audio")
                .mimeType(mimeType)
                .b64Json(b64)
                .dataUrl(OpenAiMediaSupport.dataUrl(mimeType, b64))
                .build();
        } catch (IOException e) {
            throw new RuntimeException("语音生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

    private byte[] postBytes(ChatModelVo model, String path, String jsonBody) throws IOException {
        Request request = new Request.Builder()
            .url(OpenAiMediaSupport.endpoint(model.getApiHost(), path))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonBody, OpenAiMediaSupport.JSON))
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String err = body == null ? "" : body.string();
                throw new IllegalArgumentException("OpenAI Audio API调用失败: " + response.code() + " - " + err);
            }
            if (body == null) {
                throw new IllegalArgumentException("OpenAI Audio API响应为空");
            }
            return body.bytes();
        }
    }

    private String mimeType(String format) {
        return switch (format) {
            case "opus" -> "audio/opus";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            case "wav" -> "audio/wav";
            case "pcm" -> "audio/pcm";
            default -> "audio/mpeg";
        };
    }
}
