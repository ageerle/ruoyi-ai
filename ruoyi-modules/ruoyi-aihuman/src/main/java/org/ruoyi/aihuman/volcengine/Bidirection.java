package org.ruoyi.aihuman.volcengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.aihuman.protocol.EventType;
import org.ruoyi.aihuman.protocol.Message;
import org.ruoyi.aihuman.protocol.MsgType;
import org.ruoyi.aihuman.protocol.SpeechWebSocketClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class Bidirection {
    private static final String ENDPOINT = "wss://openspeech.bytedance.com/api/v3/tts/bidirection";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get resource ID based on voice type
     *
     * @param voice Voice type string
     * @return Corresponding resource ID
     */
    public static String voiceToResourceId(String voice) {
        // Map different voice types to resource IDs based on actual needs
        if (voice.startsWith("S_")) {
            return "volc.megatts.default";
        }
        return "volc.service_type.10029";
    }

    public static void main(String[] args) throws Exception {
        // Configure parameters
        String appId = System.getProperty("appId", "1055299334");
        String accessToken = System.getProperty("accessToken", "fOHuq4R4dirMYiOruCU3Ek9q75zV0KVW");
        String resourceId = System.getProperty("resourceId", "seed-tts-2.0");
        String voice = System.getProperty("voice", "zh_female_vv_uranus_bigtts");
        String text = System.getProperty("text", "你好呀！我是AI合成的语音，很高兴认识你。");
        String encoding = System.getProperty("encoding", "mp3");

        if (appId.isEmpty() || accessToken.isEmpty()) {
            throw new IllegalArgumentException("Please set appId and accessToken system properties");
        }

        // Set request headers
        Map<String, String> headers = Map.of(
                "X-Api-App-Key", appId,
                "X-Api-Access-Key", accessToken,
                "X-Api-Resource-Id", resourceId.isEmpty() ? voiceToResourceId(voice) : resourceId,
                "X-Api-Connect-Id", UUID.randomUUID().toString());

        // Create WebSocket client
        SpeechWebSocketClient client = new SpeechWebSocketClient(new URI(ENDPOINT), headers);
        try {
            client.connectBlocking();
            Map<String, Object> request = Map.of(
                    "user", Map.of("uid", UUID.randomUUID().toString()),
                    "namespace", "BidirectionalTTS",
                    "req_params", Map.of(
                            "speaker", voice,
                            "audio_params", Map.of(
                                    "format", encoding,
                                    "sample_rate", 24000,
                                    "enable_timestamp", true),
                            // additions requires a JSON string
                            "additions", objectMapper.writeValueAsString(Map.of(
                                    "disable_markdown_filter", false))));

            // Start connection
            client.sendStartConnection();
            // Wait for connection started
            client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);

            // Process each sentence
            String[] sentences = text.split("。");
            boolean audioReceived = false;
            for (int i = 0; i < sentences.length; i++) {
                if (sentences[i].trim().isEmpty()) {
                    continue;
                }

                String sessionId = UUID.randomUUID().toString();
                ByteArrayOutputStream audioStream = new ByteArrayOutputStream();

                // Start session
                Map<String, Object> startReq = Map.of(
                        "user", request.get("user"),
                        "namespace", request.get("namespace"),
                        "req_params", request.get("req_params"),
                        "event", EventType.START_SESSION.getValue());
                client.sendStartSession(objectMapper.writeValueAsBytes(startReq), sessionId);
                // Wait for session started
                client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);

                // Send text
                for (char c : sentences[i].toCharArray()) {
                    // Create new req_params with text
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentReqParams = new HashMap<>(
                            (Map<String, Object>) request.get("req_params"));
                    currentReqParams.put("text", String.valueOf(c));

                    // Create current request
                    Map<String, Object> currentRequest = Map.of(
                            "user", request.get("user"),
                            "namespace", request.get("namespace"),
                            "req_params", currentReqParams,
                            "event", EventType.TASK_REQUEST.getValue());

                    client.sendTaskRequest(objectMapper.writeValueAsBytes(currentRequest), sessionId);
                }

                // End session
                client.sendFinishSession(sessionId);

                // Receive response
                while (true) {
                    Message msg = client.receiveMessage();
                    switch (msg.getType()) {
                        case FULL_SERVER_RESPONSE:
                            break;
                        case AUDIO_ONLY_SERVER:
                            if (!audioReceived && audioStream.size() > 0) {
                                audioReceived = true;
                            }
                            if (msg.getPayload() != null) {
                                audioStream.write(msg.getPayload());
                            }
                            break;
                        default:
                            throw new RuntimeException("Unexpected message: " + msg);
                    }
                    if (msg.getEvent() == EventType.SESSION_FINISHED) {
                        break;
                    }
                }

                if (audioStream.size() > 0) {
                    String fileName = String.format("%s_session_%d.%s", voice, i, encoding);
                    Files.write(new File(fileName).toPath(), audioStream.toByteArray());
                    log.info("Audio saved to file: {}", fileName);
                }
            }

            if (!audioReceived) {
                throw new RuntimeException("No audio data received");
            }

            // End connection
            client.sendFinishConnection();
        } finally {
            client.closeBlocking();
        }
    }
}