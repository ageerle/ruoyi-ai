package org.ruoyi.chat.service.chat.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ServiceException;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.service.v4.tools.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.chat.service.chat.ISseService;
import org.ruoyi.chat.factory.SseServiceFactory;
import org.ruoyi.chat.util.IpUtil;
import org.ruoyi.chat.util.SSEUtil;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;

import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.DateUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.core.utils.file.MimeTypeUtils;

import org.ruoyi.common.redis.utils.RedisUtils;

import org.ruoyi.service.EmbeddingService;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements ISseService {

    private final OpenAiStreamClient openAiStreamClient;

    private final EmbeddingService embeddingService;

    private final VectorStoreService vectorStore;

    private final ConfigService configService;

    private final IChatCostService chatCostService;

    private final SseServiceFactory sseServiceFactory;

    private static final String requestIdTemplate = "company-%d";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        try {
            // 构建消息列表增加联网、知识库等内容
            buildChatMessageList(chatRequest);
            // 根据模型名称前缀调用不同的处理逻辑
            switchModelAndHandle(chatRequest,sseEmitter);
            // 未登录用户限制对话次数
            checkUnauthenticatedUserChatLimit(request);
            // 保存消息记录 并扣除费用
            chatCostService.deductToken(chatRequest);
        } catch (Exception e) {
            String message = e.getMessage();
            SSEUtil.sendErrorEvent(sseEmitter, message);
            return sseEmitter;
        }
        return sseEmitter;
    }

    /**
     * 检查未登录用户是否超过当日对话次数限制
     *
     * @param request 当前请求
     * @throws ServiceException 如果当日免费次数已用完
     */
    public void checkUnauthenticatedUserChatLimit(HttpServletRequest request) throws ServiceException {
        // 未登录用户限制对话次数
        if (!StpUtil.isLogin()) {
            String clientIp = IpUtil.getClientIp(request);
            // 访客每天默认只能对话5次
            int timeWindowInSeconds = 5;
            String redisKey = "clientIp:" + clientIp;
            int count = 0;
            // 检查Redis中的对话次数
            if (RedisUtils.getCacheObject(redisKey) == null) {
                // 缓存有效时间1天
                RedisUtils.setCacheObject(redisKey, count, Duration.ofSeconds(86400));
            } else {
                count = RedisUtils.getCacheObject(redisKey);
                if (count >= timeWindowInSeconds) {
                    throw new ServiceException("当日免费次数已用完");
                }
                count++;
                RedisUtils.setCacheObject(redisKey, count);
            }
        }
    }

    /**
     *  根据模型名称前缀调用不同的处理逻辑
     */
    private void switchModelAndHandle(ChatRequest chatRequest,SseEmitter emitter) {
        String model = chatRequest.getModel();
        // 如果模型名称以ollama开头，则调用ollama中部署的本地模型
        if (model.startsWith("ollama-")) {
            String[] parts = chatRequest.getModel().split("ollama-", 2); // 限制分割次数为2
            if (parts.length > 1) {
                chatRequest.setModel(parts[1]);
                IChatService chatService = sseServiceFactory.getSseService("ollama");
                chatService.chat(chatRequest,emitter);
            } else {
                throw new IllegalArgumentException("Invalid ollama model name: " + chatRequest.getModel());
            }
        } else if (model.startsWith("gpt-4-gizmo")) {
            chatRequest.setModel("gpt-4-gizmo");
        }
    }

    /**
     *  构建消息列表
     */
    private void buildChatMessageList(ChatRequest chatRequest){
        // 获取对话消息列表
        List<Message> messages = chatRequest.getMessages();
        String sysPrompt = chatRequest.getSysPrompt();
        if(StringUtils.isEmpty(sysPrompt)){
            sysPrompt ="你是一个由RuoYI-AI开发的人工智能助手，名字叫熊猫助手。你擅长中英文对话，能够理解并处理各种问题，提供安全、有帮助、准确的回答。" +
                    "当前时间："+ DateUtils.getDate();
        }
        // 设置系统默认提示词
        Message sysMessage = Message.builder().content(sysPrompt).role(Message.Role.SYSTEM).build();
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
        // 用户对话内容
        String chatString = null;
        // 获取用户对话信息
        Object content = messages.get(messages.size() - 1).getContent();
        if (content instanceof List<?> listContent) {
            if (CollectionUtil.isNotEmpty(listContent)) {
                chatString = listContent.get(0).toString();
            }
        } else if (content instanceof String) {
            chatString = (String) content;
        }
        // 设置对话信息
        chatRequest.setPrompt(chatString);
        // 加载联网信息
        if(chatRequest.getSearch()){
            Message message = Message.builder().role(Message.Role.ASSISTANT).content("联网信息:"+webSearch(chatString)).build();
            messages.add(message);
        }
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
        String zpValue = configService.getConfigValue("zhipu", "key");
        if(StringUtils.isEmpty(zpValue)){
            throw new IllegalStateException("请在chat_config中配置智谱key信息");
        }else {
            ClientV4 client = new ClientV4.Builder(zpValue)
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
