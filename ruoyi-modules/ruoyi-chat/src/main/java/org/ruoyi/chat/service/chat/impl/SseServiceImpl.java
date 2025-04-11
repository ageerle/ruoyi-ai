package org.ruoyi.chat.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ServiceException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.service.v4.tools.*;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.chat.config.ChatConfig;
import org.ruoyi.chat.listener.SSEEventSourceListener;

import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.chat.service.chat.ISseService;
import org.ruoyi.chat.util.IpUtil;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;

import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.core.utils.file.MimeTypeUtils;

import org.ruoyi.common.redis.utils.RedisUtils;

import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.EmbeddingService;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.VectorStoreService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements ISseService {

    private final OpenAiStreamClient openAiStreamClient;

    private final ChatConfig chatConfig;

    private final IChatModelService chatModelService;

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStore;

    private final ConfigService configService;

    private final IChatCostService chatCostService;

    private static final String requestIdTemplate = "mycompany-%d";

    private static final ObjectMapper mapper = new ObjectMapper();

    private OpenAiStreamClient openAiModelStreamClient;

    @Override
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(sseEmitter);
        // 获取对话消息列表
        List<Message> messages = chatRequest.getMessages();
        // 用户对话内容
        String chatString = null;
        try {
            if (StpUtil.isLogin()) {
                // 通过模型名称查询模型信息
                ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
                if(chatModelVo!=null){
                    // 通过模型信息构建请求客户端
                    openAiModelStreamClient = chatConfig.createOpenAiStreamClient(chatModelVo.getApiHost(), chatModelVo.getApiKey());
                }else {
                    // 使用默认客户端
                    openAiModelStreamClient  = openAiStreamClient;
                }
                // 设置默认提示词
                Message sysMessage = Message.builder().content(chatModelVo.getSystemPrompt()).role(Message.Role.SYSTEM).build();
                messages.add(0,sysMessage);

                // 查询向量库相关信息加入到上下文
                if(chatRequest.getKid()!=null){
                    List<Message> knMessages = new ArrayList<>();
                    String content = messages.get(messages.size() - 1).getContent().toString();
                    List<String> nearestList;
                    List<Double> queryVector = embeddingService.getQueryVector(content, chatRequest.getKid());
                    nearestList = vectorStore.nearest(queryVector, chatRequest.getKid());
                    for (String prompt : nearestList) {
                        Message userMessage = Message.builder().content(prompt).role(Message.Role.USER).build();
                        knMessages.add(userMessage);
                    }
                    Message userMessage = Message.builder().content(content + (!nearestList.isEmpty() ? "\n\n注意：回答问题时，须严格根据我给你的系统上下文内容原文进行回答，请不要自己发挥,回答时保持原来文本的段落层级" : "")).role(Message.Role.USER).build();
                    knMessages.add(userMessage);
                    messages.addAll(knMessages);
                }

                // 获取用户对话信息
                Object content = messages.get(messages.size() - 1).getContent();
                if (content instanceof List<?> listContent) {
                    if (CollectionUtil.isNotEmpty(listContent)) {
                        chatString = listContent.get(0).toString();
                    }
                } else if (content instanceof String) {
                    chatString = (String) content;
                }

                // 加载联网信息
                if(chatRequest.getSearch()){
                    Message message = Message.builder().role(Message.Role.ASSISTANT).content("联网信息:"+webSearch(chatString)).build();
                    messages.add(message);
                }
            }else {
                // 未登录用户限制对话次数
                String clientIp = IpUtil.getClientIp(request);

                // 访客每天默认只能对话5次
                int timeWindowInSeconds = 5;

                String redisKey = "clientIp:" + clientIp;

                int count = 0;

                if (RedisUtils.getCacheObject(redisKey) == null) {
                    // 缓存有效时间1天
                    RedisUtils.setCacheObject(redisKey, count, Duration.ofSeconds(86400));
                }else {
                    count = RedisUtils.getCacheObject(redisKey);
                    if (count >= timeWindowInSeconds) {
                        throw new ServiceException("当日免费次数已用完");
                    }
                    count++;
                    RedisUtils.setCacheObject(redisKey, count);
                }
            }

            ChatCompletion completion = ChatCompletion
                    .builder()
                    .messages(messages)
                    .model(chatRequest.getModel())
                    .stream(chatRequest.getStream())
                    .build();
            openAiModelStreamClient.streamChatCompletion(completion, openAIEventSourceListener);
            // 保存消息记录 并扣除费用
            chatCostService.deductToken(chatRequest);
        } catch (Exception e) {
            String message = e.getMessage();
            sendErrorEvent(sseEmitter, message);
            return sseEmitter;
        }
        return sseEmitter;
    }


    /**
     * 发送SSE错误事件的封装方法
     *
     * @param sseEmitter
     * @param errorMessage
     */
    private void sendErrorEvent(SseEmitter sseEmitter, String errorMessage) {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("error")
                .data(errorMessage);
        try {
            sseEmitter.send(event);
        } catch (IOException e) {
            log.error("SSE发送失败: {}", e.getMessage());
        }
        sseEmitter.complete();
    }

    /**
     * 文字转语音
     */
    @Override
    public ResponseEntity<Resource> textToSpeed(TextToSpeech textToSpeech) {
        ResponseBody body = openAiStreamClient.textToSpeech(textToSpeech);
        if (body != null) {
            // 将ResponseBody转换为InputStreamResource
            InputStreamResource resource = new InputStreamResource(body.byteStream());
            // 创建并返回ResponseEntity
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(resource);
        } else {
            // 如果ResponseBody为空，返回404状态码
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 语音转文字
     */
    @Override
    public WhisperResponse speechToTextTranscriptionsV2(MultipartFile file) {
        // 确保文件不为空
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot convert an empty MultipartFile");
        }
        if (!FileUtils.isValidFileExtention(file, MimeTypeUtils.AUDIO__EXTENSION)) {
            throw new IllegalStateException("File Extention not supported");
        }
        // 创建一个文件对象
        File fileA = new File(System.getProperty("java.io.tmpdir") + File.separator + file.getOriginalFilename());
        try {
            // 将 MultipartFile 的内容写入文件
            file.transferTo(fileA);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert MultipartFile to File", e);
        }
        return openAiStreamClient.speechToTextTranscriptions(fileA);
    }


    @Override
    public UploadFileResponse upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot upload an empty MultipartFile");
        }
        if (!FileUtils.isValidFileExtention(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION)) {
            throw new IllegalStateException("File Extention not supported");
        }
        return openAiStreamClient.uploadFile("fine-tune", convertMultiPartToFile(file));
    }

    private File convertMultiPartToFile(MultipartFile multipartFile) {
        File file = null;
        try {
            // 获取原始文件名
            String originalFileName = multipartFile.getOriginalFilename();
            // 默认扩展名
            String extension = ".tmp";
            // 尝试从原始文件名中获取扩展名
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // 使用原始文件的扩展名创建临时文件
            Path tempFile = Files.createTempFile(null, extension);
            file = tempFile.toFile();

            // 将MultipartFile的内容写入文件
            try (InputStream inputStream = multipartFile.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            } catch (IOException e) {
                // 处理文件写入异常
                e.printStackTrace();
            }
        } catch (IOException e) {
            // 处理临时文件创建异常
            e.printStackTrace();
        }
        return file;
    }

    @Override
    public SseEmitter ollamaChat(ChatRequest chatRequest) {
        String[] parts = chatRequest.getModel().split("ollama-");
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());
        final SseEmitter emitter = new SseEmitter();
        String host = chatModelVo.getApiHost();
        List<Message> msgList = chatRequest.getMessages();
        List<OllamaChatMessage> messages = new ArrayList<>();

        for (Message message : msgList) {
            OllamaChatMessage ollamaChatMessage = new OllamaChatMessage();
            ollamaChatMessage.setRole(OllamaChatMessageRole.USER);
            ollamaChatMessage.setContent(message.getContent().toString());
            messages.add(ollamaChatMessage);
        }
        OllamaAPI api = new OllamaAPI(host);
        api.setRequestTimeoutSeconds(100);
        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(parts[1]);

        OllamaChatRequestModel requestModel = builder
            .withMessages(messages)
            .build();

        // 异步执行 OllAma API 调用
        CompletableFuture.runAsync(() -> {
            try {
                StringBuilder response = new StringBuilder();
                OllamaStreamHandler streamHandler = (s) -> {
                    String substr = s.substring(response.length());
                    response.append(substr);
                    System.out.println(substr);
                    try {
                        emitter.send(substr);
                    } catch (IOException e) {
                        sendErrorEvent(emitter, e.getMessage());
                    }
                };
                api.chat(requestModel, streamHandler);
                emitter.complete();
            } catch (Exception e) {
                sendErrorEvent(emitter, e.getMessage());
            }
        });
        return emitter;
    }

    @Override
    public String wxCpChat(String prompt) {
        List<Message> messageList = new ArrayList<>();
        Message message = Message.builder().role(Message.Role.USER).content(prompt).build();
        messageList.add(message);
        ChatCompletion chatCompletion = ChatCompletion
            .builder()
            .messages(messageList)
            .model("gpt-4o-mini")
            .stream(false)
            .build();
        ChatCompletionResponse chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);
        return chatCompletionResponse.getChoices().get(0).getMessage().getContent().toString();
    }

    @Override
    public String webSearch (String prompt) {
        String zhipuValue = configService.getConfigValue("zhipu", "key");
        if(StringUtils.isEmpty(zhipuValue)){
            throw new IllegalStateException("zhipu config value is empty,请在chat_config中配置zhipu key信息");
        }else {
            ClientV4 client = new ClientV4.Builder(zhipuValue)
                    .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
                    .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
                    .build();

            SearchChatMessage jsonNodes = new SearchChatMessage();
            jsonNodes.setRole(Message.Role.USER.getName());
            jsonNodes.setContent(prompt);

            String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
            WebSearchParamsRequest chatCompletionRequest = WebSearchParamsRequest.builder()
                    .model("web-search-pro")
                    .stream(Boolean.TRUE)
                    .messages(Collections.singletonList(jsonNodes))
                    .requestId(requestId)
                    .build();
            WebSearchApiResponse webSearchApiResponse = client.webSearchProStreamingInvoke(chatCompletionRequest);
            List<ChoiceDelta> choices = new ArrayList<>();
            if (webSearchApiResponse.isSuccess()) {
                AtomicBoolean isFirst = new AtomicBoolean(true);

                AtomicReference<WebSearchPro> lastAccumulator = new AtomicReference<>();

                webSearchApiResponse.getFlowable().map(result -> result)
                        .doOnNext(accumulator -> {
                            {
                                if (isFirst.getAndSet(false)) {
                                    log.info("Response: ");
                                }
                                ChoiceDelta delta = accumulator.getChoices().get(0).getDelta();
                                if (delta != null && delta.getToolCalls() != null) {
                                    log.info("tool_calls: {}", mapper.writeValueAsString(delta.getToolCalls()));
                                }
                                choices.add(delta);
                            }
                        })
                        .doOnComplete(() -> System.out.println("Stream completed."))
                        .doOnError(throwable -> System.err.println("Error: " + throwable))
                        .blockingSubscribe();

                WebSearchPro chatMessageAccumulator = lastAccumulator.get();
                webSearchApiResponse.setFlowable(null);
                webSearchApiResponse.setData(chatMessageAccumulator);
            }
            return  choices.get(1).getToolCalls().toString();
        }
    }

}
