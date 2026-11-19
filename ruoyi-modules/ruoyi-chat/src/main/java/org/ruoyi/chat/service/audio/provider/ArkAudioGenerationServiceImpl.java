package org.ruoyi.chat.service.audio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.security.CustomApiCredentialPolicy;
import org.ruoyi.chat.service.audio.AbstractAudioGenerationService;
import org.ruoyi.chat.service.media.OpenAiMediaSupport;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component("arkAudio")
public class ArkAudioGenerationServiceImpl extends AbstractAudioGenerationService {

    private static final String ENDPOINT = "https://openspeech.bytedance.com/api/v3/plan/tts/unidirectional";
    private static final String MODEL_NAME = "doubao-seed-tts-2.0";
    private static final String RESOURCE_ID = "seed-tts-2.0";
    private static final String DEFAULT_SPEAKER = "zh_female_vv_uranus_bigtts";
    private static final int SAMPLE_RATE = 24000;
    private static final int COMPLETED_CODE = 20000000;

    private final OkHttpClient client;

    public ArkAudioGenerationServiceImpl() {
        this(new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .build());
    }

    ArkAudioGenerationServiceImpl(OkHttpClient client) {
        this.client = client;
    }

    @Override
    protected MediaGenerationResponse doGenerateSpeech(AudioContext context) {
        var model = context.getChatModelVo();
        if (!MODEL_NAME.equals(model.getModelName())) {
            throw new IllegalArgumentException("方舟语音合成支持 " + MODEL_NAME + " 模型");
        }
        String apiKey = model.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("请填写方舟 Agent Plan 专属 API Key");
        }
        AudioFormat format = AudioFormat.from(context.getResponseFormat());
        Request request = new Request.Builder()
            .url(ENDPOINT)
            .header("X-Api-Key", apiKey.trim())
            .header("X-Api-Resource-Id", RESOURCE_ID)
            .header("X-Api-Request-Id", UUID.randomUUID().toString())
            .post(RequestBody.create(payload(context, format).toString(), OpenAiMediaSupport.JSON))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("方舟语音合成请求失败（HTTP " + response.code() + "）");
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalArgumentException("方舟语音合成未返回音频");
            }
            byte[] audio = readAudio(body);
            if (format == AudioFormat.WAV) {
                audio = wrapWav(audio);
            }
            String base64 = Base64.getEncoder().encodeToString(audio);
            return MediaGenerationResponse.builder()
                .type("audio")
                .mimeType(format.mimeType)
                .b64Json(base64)
                .dataUrl(OpenAiMediaSupport.dataUrl(format.mimeType, base64))
                .build();
        } catch (IOException e) {
            throw new IllegalStateException("读取方舟语音合成结果失败", e);
        }
    }

    @Override
    public String getProviderName() {
        return CustomApiCredentialPolicy.ARK_PROVIDER;
    }

    private static ObjectNode payload(AudioContext context, AudioFormat format) {
        var payload = OpenAiMediaSupport.OBJECT_MAPPER.createObjectNode();
        var params = payload.putObject("req_params");
        params.put("text", context.getInput());
        String voice = context.getVoice();
        params.put("speaker", voice == null || voice.isBlank() ? DEFAULT_SPEAKER : voice.trim());
        var audio = params.putObject("audio_params");
        audio.put("format", format.apiFormat);
        audio.put("sample_rate", SAMPLE_RATE);
        Double speed = context.getSpeed();
        if (speed != null) {
            if (!Double.isFinite(speed) || speed < 0.5 || speed > 2.0) {
                throw new IllegalArgumentException("方舟语音合成的语速需在 0.5 到 2 倍之间");
            }
            audio.put("speech_rate", (int) Math.round((speed - 1) * 100));
        }
        String instructions = context.getInstructions();
        if (instructions != null && !instructions.isBlank()) {
            var additions = OpenAiMediaSupport.OBJECT_MAPPER.createObjectNode();
            additions.putArray("context_texts").add(instructions.trim());
            params.put("additions", additions.toString());
        }
        return payload;
    }

    private static byte[] readAudio(ResponseBody body) throws IOException {
        var audio = new ByteArrayOutputStream();
        var source = body.source();
        String line;
        while ((line = source.readUtf8Line()) != null) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode chunk = OpenAiMediaSupport.OBJECT_MAPPER.readTree(line);
            JsonNode code = chunk.path("code");
            if (!code.isIntegralNumber() || !code.canConvertToInt()) {
                throw new IllegalArgumentException("方舟语音合成返回了无法识别的结果");
            }
            if (code.intValue() == COMPLETED_CODE) {
                if (audio.size() == 0) {
                    throw new IllegalArgumentException("方舟语音合成未返回音频");
                }
                return audio.toByteArray();
            }
            if (code.intValue() != 0) {
                throw new IllegalArgumentException("方舟语音合成失败（错误码 " + code.intValue() + "）");
            }
            String data = chunk.path("data").asText("");
            if (!data.isEmpty()) {
                audio.writeBytes(Base64.getDecoder().decode(data));
            }
        }
        throw new IllegalArgumentException("方舟语音合成的响应提前结束");
    }

    private static byte[] wrapWav(byte[] pcm) {
        // The service streams 16-bit mono PCM. Write one WAV header after all chunks arrive.
        return ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + pcm.length)
            .put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16)
            .putShort((short) 1).putShort((short) 1).putInt(SAMPLE_RATE)
            .putInt(SAMPLE_RATE * 2).putShort((short) 2).putShort((short) 16)
            .put("data".getBytes(StandardCharsets.US_ASCII)).putInt(pcm.length)
            .put(pcm).array();
    }

    private enum AudioFormat {
        MP3("mp3", "audio/mpeg"),
        OPUS("ogg_opus", "audio/ogg"),
        WAV("pcm", "audio/wav");

        private final String apiFormat;
        private final String mimeType;

        AudioFormat(String apiFormat, String mimeType) {
            this.apiFormat = apiFormat;
            this.mimeType = mimeType;
        }

        static AudioFormat from(String value) {
            if (value == null || value.isBlank()) {
                return MP3;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "mp3" -> MP3;
                case "opus", "ogg_opus" -> OPUS;
                case "wav" -> WAV;
                default -> throw new IllegalArgumentException("方舟语音合成可输出 MP3、WAV 或 Opus");
            };
        }
    }
}
