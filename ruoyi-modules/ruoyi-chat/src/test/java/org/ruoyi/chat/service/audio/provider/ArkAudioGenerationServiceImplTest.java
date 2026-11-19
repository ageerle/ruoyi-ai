package org.ruoyi.chat.service.audio.provider;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.chat.service.media.OpenAiMediaSupport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ArkAudioGenerationServiceImplTest {

    private static final String COMPLETED_AUDIO = """
        {"code":0,"data":"AQI="}
        {"code":0,"data":"AwQ="}
        {"code":20000000}
        """;

    @Test
    void usesDedicatedVoiceEndpointAndReassemblesAudioChunks() {
        var request = new AtomicReference<JsonNode>();
        var service = serviceReturning(COMPLETED_AUDIO, request);

        var response = service.generateSpeech(context().build());

        assertEquals("ark", service.getProviderName());
        assertEquals("audio", response.getType());
        assertEquals("audio/mpeg", response.getMimeType());
        assertEquals("data:audio/mpeg;base64,AQIDBA==", response.getDataUrl());
        var params = request.get().path("req_params");
        assertEquals("方舟语音测试", params.path("text").asText());
        assertEquals("zh_female_vv_uranus_bigtts", params.path("speaker").asText());
        assertEquals("mp3", params.path("audio_params").path("format").asText());
    }

    @Test
    void convertsWavFromPcmAndMapsVoiceAndSpeechRate() throws Exception {
        var request = new AtomicReference<JsonNode>();
        var service = serviceReturning(COMPLETED_AUDIO, request);
        var response = service.generateSpeech(context().voice("voice-test")
            .responseFormat("wav").speed(1.25).instructions("慢一点").build());

        var params = request.get().path("req_params");
        assertEquals("voice-test", params.path("speaker").asText());
        assertEquals("pcm", params.path("audio_params").path("format").asText());
        assertEquals(25, params.path("audio_params").path("speech_rate").asInt());
        var additions = OpenAiMediaSupport.OBJECT_MAPPER.readTree(params.path("additions").asText());
        assertEquals("慢一点", additions.path("context_texts").path(0).asText());

        assertEquals("audio/wav", response.getMimeType());
        byte[] bytes = Base64.getDecoder().decode(response.getB64Json());
        assertEquals("RIFF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        assertEquals(24000, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(24));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, java.util.Arrays.copyOfRange(bytes, 44, bytes.length));
    }

    @Test
    void rejectsInterruptedAndProviderErrorStreamsRatherThanReturningPartialAudio() {
        var request = new AtomicReference<JsonNode>();
        var interrupted = serviceReturning("{\"code\":0,\"data\":\"AQI=\"}\n", request);
        assertThrows(IllegalArgumentException.class, () -> interrupted.generateSpeech(context().build()));

        var rejected = serviceReturning("{\"code\":45000000,\"message\":\"provider detail\"}\n", request);
        var error = assertThrows(IllegalArgumentException.class, () -> rejected.generateSpeech(context().build()));
        assertTrue(error.getMessage().contains("45000000"));
    }

    private static AudioContext.AudioContextBuilder context() {
        var model = new ChatModelVo();
        model.setModelName("doubao-seed-tts-2.0");
        model.setApiKey("test-only");
        model.setApiHost("https://example.com/untrusted");
        return AudioContext.builder().chatModelVo(model).input("方舟语音测试");
    }

    private static ArkAudioGenerationServiceImpl serviceReturning(String body, AtomicReference<JsonNode> captured) {
        var client = new OkHttpClient.Builder().addInterceptor(chain -> {
            var request = chain.request();
            assertEquals("https://openspeech.bytedance.com/api/v3/plan/tts/unidirectional", request.url().toString());
            assertEquals("test-only", request.header("X-Api-Key"));
            assertEquals("seed-tts-2.0", request.header("X-Api-Resource-Id"));
            var buffer = new Buffer();
            request.body().writeTo(buffer);
            captured.set(OpenAiMediaSupport.OBJECT_MAPPER.readTree(buffer.readUtf8()));
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(ResponseBody.create(body, OpenAiMediaSupport.JSON)).build();
        }).build();
        return new ArkAudioGenerationServiceImpl(client);
    }
}
