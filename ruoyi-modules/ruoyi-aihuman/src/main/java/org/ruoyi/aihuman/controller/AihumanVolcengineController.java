package org.ruoyi.aihuman.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.ruoyi.aihuman.domain.VoiceRequest;
import org.ruoyi.aihuman.protocol.EventType;
import org.ruoyi.aihuman.protocol.Message;
import org.ruoyi.aihuman.protocol.MsgType;
import org.ruoyi.aihuman.protocol.SpeechWebSocketClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 火山引擎相关接口
 *
 * @author ruoyi
 */
// 临时免登录
@SaIgnore

@Validated
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/aihuman/volcengine")
public class AihumanVolcengineController {

    @Autowired
    private ResourceLoader resourceLoader;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(AihumanVolcengineController.class);


    @PostMapping("/generate-voice-direct")
    public ResponseEntity<byte[]> generateVoiceDirect(@RequestBody VoiceRequest request) {
        try {
            // 生成唯一的语音ID
            String voiceId = UUID.randomUUID().toString().replace("-", "");

            log.info("开始生成语音，voiceId: {}", voiceId);

            // 调用火山引擎TTS API获取音频数据
            byte[] audioData = generateVoiceData(request, voiceId);

            // 设置响应头，返回音频数据
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentDispositionFormData("attachment", "voice_" + System.currentTimeMillis() + ".wav");
            headers.setContentLength(audioData.length);

            log.info("语音生成成功并返回，长度: {} bytes", audioData.length);
            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("生成语音失败", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] generateVoiceData(VoiceRequest request, String voiceId) {
        try {
            // 这里是调用火山引擎TTS API的核心逻辑
            // 您需要根据火山引擎的API文档实现具体的调用逻辑
            // 注意：这只是一个示例框架，您需要根据实际情况进行实现

            // 调用火山引擎API并获取音频数据
            // 假设您已经有现有的调用逻辑，这里保留原有的实现
            String endpoint = request.getEndpoint();
            String appId = request.getAppId();
            String accessToken = request.getAccessToken();
            String resourceId = request.getResourceId();
            String voice = request.getVoice();
            String text = request.getText();
            String encoding = request.getEncoding();

            // 调用原有的火山引擎API调用方法（如果有）
            // 或者直接在这里实现API调用逻辑
            byte[] audioData = callVolcengineTtsApiByte(endpoint, appId, accessToken,
                    resourceId, voice, text, encoding);

            log.info("成功生成语音数据，大小: {} bytes", audioData.length);
            return audioData;
        } catch (Exception e) {
            log.error("生成语音数据失败", e);
            throw new RuntimeException("生成语音数据失败", e);
        }

    }

    private byte[] callVolcengineTtsApiByte(String endpoint, String appId, String accessToken,
                                            String resourceId, String voice, String text, String encoding) {
        try {
            // 确保resourceId不为空，如果为空则根据voice类型获取默认值
            if (resourceId == null || resourceId.isEmpty()) {
                resourceId = voiceToResourceId(voice);
            }

            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Api-App-Key", appId);
            headers.put("X-Api-Access-Key", accessToken);
            headers.put("X-Api-Resource-Id", resourceId);
            headers.put("X-Api-Connect-Id", UUID.randomUUID().toString());

            // 创建WebSocket客户端
            SpeechWebSocketClient client = new SpeechWebSocketClient(new URI(endpoint), headers);
            ByteArrayOutputStream totalAudioStream = new ByteArrayOutputStream();
            boolean audioReceived = false;

            try {
                // 连接WebSocket
                client.connectBlocking();

                // 构建请求参数
                Map<String, Object> request = new HashMap<>();
                request.put("user", Map.of("uid", UUID.randomUUID().toString()));
                request.put("namespace", "BidirectionalTTS");

                Map<String, Object> reqParams = new HashMap<>();
                reqParams.put("speaker", voice);

                Map<String, Object> audioParams = new HashMap<>();
                audioParams.put("format", encoding);
                audioParams.put("sample_rate", 24000);
                audioParams.put("enable_timestamp", true);

                reqParams.put("audio_params", audioParams);
                reqParams.put("additions", objectMapper.writeValueAsString(Map.of("disable_markdown_filter", false)));

                request.put("req_params", reqParams);

                // 开始连接
                client.sendStartConnection();
                // 等待连接成功
                client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);

                // 处理每个句子
                String[] sentences = text.split("。");
                for (int i = 0; i < sentences.length; i++) {
                    if (sentences[i].trim().isEmpty()) {
                        continue;
                    }

                    String sessionId = UUID.randomUUID().toString();
                    ByteArrayOutputStream sentenceAudioStream = new ByteArrayOutputStream();

                    // 开始会话
                    Map<String, Object> startReq = new HashMap<>();
                    startReq.put("user", request.get("user"));
                    startReq.put("namespace", request.get("namespace"));
                    startReq.put("req_params", request.get("req_params"));
                    startReq.put("event", EventType.START_SESSION.getValue());
                    client.sendStartSession(objectMapper.writeValueAsBytes(startReq), sessionId);
                    // 等待会话开始
                    client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);

                    // 发送文本内容
                    for (char c : sentences[i].toCharArray()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> currentReqParams = new HashMap<>((Map<String, Object>) request.get("req_params"));
                        currentReqParams.put("text", String.valueOf(c));

                        Map<String, Object> currentRequest = new HashMap<>();
                        currentRequest.put("user", request.get("user"));
                        currentRequest.put("namespace", request.get("namespace"));
                        currentRequest.put("req_params", currentReqParams);
                        currentRequest.put("event", EventType.TASK_REQUEST.getValue());

                        client.sendTaskRequest(objectMapper.writeValueAsBytes(currentRequest), sessionId);
                    }

                    // 结束会话
                    client.sendFinishSession(sessionId);

                    // 接收响应
                    while (true) {
                        Message msg = client.receiveMessage();
                        switch (msg.getType()) {
                            case FULL_SERVER_RESPONSE:
                                break;
                            case AUDIO_ONLY_SERVER:
                                if (!audioReceived && sentenceAudioStream.size() > 0) {
                                    audioReceived = true;
                                }
                                if (msg.getPayload() != null) {
                                    sentenceAudioStream.write(msg.getPayload());
                                }
                                break;
                            default:
                                // 不抛出异常，记录日志并继续处理
                                log.warn("Unexpected message type: {}", msg.getType());
                        }
                        if (msg.getEvent() == EventType.SESSION_FINISHED) {
                            break;
                        }
                    }

                    // 将当前句子的音频追加到总音频流
                    if (sentenceAudioStream.size() > 0) {
                        totalAudioStream.write(sentenceAudioStream.toByteArray());
                    }
                }

                // 验证是否收到音频数据
                if (totalAudioStream.size() > 0) {
                    log.info("Audio data generated successfully, size: {} bytes", totalAudioStream.size());
                    return totalAudioStream.toByteArray();
                } else {
                    throw new RuntimeException("No audio data received");
                }
            } finally {
                // 结束连接
                client.sendFinishConnection();
                client.closeBlocking();
            }
        } catch (Exception e) {
            log.error("Error calling Volcengine TTS API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate voice", e);
        }
    }


    /**
     * 生成语音文件接口
     * 用户传入JSON参数，返回音频文件的播放地址
     */
    @PostMapping("/generate-voice")
    public ResponseEntity<?> generateVoice(@RequestBody VoiceRequest request) {
        try {
            // 1. 解析请求参数
            String endpoint = request.getEndpoint();
            String appId = request.getAppId();
            String accessToken = request.getAccessToken();
            String resourceId = request.getResourceId();
            String voice = request.getVoice();
            String text = request.getText();
            String encoding = request.getEncoding();

            // 1.1 验证必要参数
            if (endpoint == null || endpoint.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "endpoint cannot be null or empty"));
            }
            if (appId == null || appId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "appId cannot be null or empty"));
            }
            if (accessToken == null || accessToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "accessToken cannot be null or empty"));
            }
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "text cannot be null or empty"));
            }

            // 1.2 设置默认值
            if (encoding == null || encoding.isEmpty()) {
                encoding = "mp3";
            }

            // 2. 调用火山引擎API生成音频文件
            String audioUrl = callVolcengineTtsApi(endpoint, appId, accessToken, resourceId, voice, text, encoding);

            // 3. 构造并返回响应
            Map<String, String> response = new HashMap<>();
            response.put("audioUrl", audioUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 处理异常情况
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "生成音频文件失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 调用火山引擎TTS API生成音频文件
     */
    private String callVolcengineTtsApi(String endpoint, String appId, String accessToken,
                                       String resourceId, String voice, String text, String encoding) {
        try {
            // 确保resourceId不为空，如果为空则根据voice类型获取默认值
            if (resourceId == null || resourceId.isEmpty()) {
                resourceId = voiceToResourceId(voice);
            }

            // 生成唯一的文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String randomId = UUID.randomUUID().toString().substring(0, 8);
            String fileName = "voice_" + timestamp + "_" + randomId + "." + encoding;

            // 获取resources/voice目录路径
            String voiceDirPath = getVoiceDirectoryPath();
            File voiceDir = new File(voiceDirPath);
            if (!voiceDir.exists()) {
                voiceDir.mkdirs();
            }

            String filePath = voiceDirPath + File.separator + fileName;

            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Api-App-Key", appId);
            headers.put("X-Api-Access-Key", accessToken);
            headers.put("X-Api-Resource-Id", resourceId);
            headers.put("X-Api-Connect-Id", UUID.randomUUID().toString());

            // 创建WebSocket客户端
            SpeechWebSocketClient client = new SpeechWebSocketClient(new URI(endpoint), headers);
            ByteArrayOutputStream totalAudioStream = new ByteArrayOutputStream();
            boolean audioReceived = false;

            try {
                // 连接WebSocket
                client.connectBlocking();

                // 构建请求参数
                Map<String, Object> request = new HashMap<>();
                request.put("user", Map.of("uid", UUID.randomUUID().toString()));
                request.put("namespace", "BidirectionalTTS");

                Map<String, Object> reqParams = new HashMap<>();
                reqParams.put("speaker", voice);

                Map<String, Object> audioParams = new HashMap<>();
                audioParams.put("format", encoding);
                audioParams.put("sample_rate", 24000);
                audioParams.put("enable_timestamp", true);

                reqParams.put("audio_params", audioParams);
                reqParams.put("additions", objectMapper.writeValueAsString(Map.of("disable_markdown_filter", false)));

                request.put("req_params", reqParams);

                // 开始连接
                client.sendStartConnection();
                // 等待连接成功
                client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.CONNECTION_STARTED);

                // 处理每个句子
                String[] sentences = text.split("。");
                for (int i = 0; i < sentences.length; i++) {
                    if (sentences[i].trim().isEmpty()) {
                        continue;
                    }

                    String sessionId = UUID.randomUUID().toString();
                    ByteArrayOutputStream sentenceAudioStream = new ByteArrayOutputStream();

                    // 开始会话
                    Map<String, Object> startReq = new HashMap<>();
                    startReq.put("user", request.get("user"));
                    startReq.put("namespace", request.get("namespace"));
                    startReq.put("req_params", request.get("req_params"));
                    startReq.put("event", EventType.START_SESSION.getValue());
                    client.sendStartSession(objectMapper.writeValueAsBytes(startReq), sessionId);
                    // 等待会话开始
                    client.waitForMessage(MsgType.FULL_SERVER_RESPONSE, EventType.SESSION_STARTED);

                    // 发送文本内容
                    for (char c : sentences[i].toCharArray()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> currentReqParams = new HashMap<>((Map<String, Object>) request.get("req_params"));
                        currentReqParams.put("text", String.valueOf(c));

                        Map<String, Object> currentRequest = new HashMap<>();
                        currentRequest.put("user", request.get("user"));
                        currentRequest.put("namespace", request.get("namespace"));
                        currentRequest.put("req_params", currentReqParams);
                        currentRequest.put("event", EventType.TASK_REQUEST.getValue());

                        client.sendTaskRequest(objectMapper.writeValueAsBytes(currentRequest), sessionId);
                    }

                    // 结束会话
                    client.sendFinishSession(sessionId);

                    // 接收响应
                    while (true) {
                        Message msg = client.receiveMessage();
                        switch (msg.getType()) {
                            case FULL_SERVER_RESPONSE:
                                break;
                            case AUDIO_ONLY_SERVER:
                                if (!audioReceived && sentenceAudioStream.size() > 0) {
                                    audioReceived = true;
                                }
                                if (msg.getPayload() != null) {
                                    sentenceAudioStream.write(msg.getPayload());
                                }
                                break;
                            default:
                                // 不抛出异常，记录日志并继续处理
                                log.warn("Unexpected message type: {}", msg.getType());
                        }
                        if (msg.getEvent() == EventType.SESSION_FINISHED) {
                            break;
                        }
                    }

                    // 将当前句子的音频追加到总音频流
                    if (sentenceAudioStream.size() > 0) {
                        totalAudioStream.write(sentenceAudioStream.toByteArray());
                    }
                }

                // 保存音频文件
                if (totalAudioStream.size() > 0) {
                    Files.write(Paths.get(filePath), totalAudioStream.toByteArray(), StandardOpenOption.CREATE);
                    log.info("Audio saved to file: {}", filePath);
                } else {
                    throw new RuntimeException("No audio data received");
                }

                // 结束连接
                client.sendFinishConnection();
            } finally {
                client.closeBlocking();
            }

            // 返回音频文件的访问路径
            return "/voice/" + fileName;
        } catch (Exception e) {
            log.error("Error calling Volcengine TTS API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate voice", e);
        }
    }

    /**
     * 根据voice类型获取resourceId
     */
    private String voiceToResourceId(String voice) {
        if (voice != null && voice.startsWith("S_")) {
            return "volc.megatts.default";
        }
        return "volc.service_type.10029";
    }

    /**
     * 获取voice目录路径
     */
    private String getVoiceDirectoryPath() {
        try {
            // 获取当前项目根目录
            String projectRoot = System.getProperty("user.dir");

            // 构建目标目录路径：ruoyi-ai/ruoyi-modules/ruoyi-aihuman/src/main/resources/voice
            File targetDir = new File(projectRoot, "ruoyi-modules/ruoyi-aihuman/src/main/resources/voice");

            // 确保目录存在
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (created) {
                    logger.info("成功创建目录: {}", targetDir.getAbsolutePath());
                } else {
                    logger.warn("无法创建目录: {}", targetDir.getAbsolutePath());

                    // 降级方案：直接使用项目根目录下的voice文件夹
                    File fallbackDir = new File(projectRoot, "voice");
                    if (!fallbackDir.exists()) {
                        fallbackDir.mkdirs();
                    }
                    return fallbackDir.getAbsolutePath();
                }
            }

            return targetDir.getAbsolutePath();
        } catch (Exception e) {
            logger.error("获取音频目录路径失败", e);

            // 异常情况下的安全降级
            File safeDir = new File("voice");
            if (!safeDir.exists()) {
                safeDir.mkdirs();
            }
            return safeDir.getAbsolutePath();
        }
    }
}


