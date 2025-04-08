package org.ruoyi.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.ruoyi.common.chat.config.ChatConfig;
import org.ruoyi.common.chat.config.LocalCache;
import org.ruoyi.common.chat.domain.request.ChatRequest;
import org.ruoyi.common.chat.domain.request.Dall3Request;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.chat.*;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.images.Image;
import org.ruoyi.common.chat.entity.images.ImageResponse;
import org.ruoyi.common.chat.entity.images.Item;
import org.ruoyi.common.chat.entity.images.ResponseFormat;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.openai.plugin.PluginAbstract;
import org.ruoyi.common.chat.plugin.CmdPlugin;
import org.ruoyi.common.chat.plugin.CmdReq;
import org.ruoyi.common.chat.plugin.SqlPlugin;
import org.ruoyi.common.chat.plugin.SqlReq;
import org.ruoyi.common.chat.utils.TikTokensUtil;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.core.utils.file.MimeTypeUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.SysModel;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.request.translation.TranslationRequest;
import org.ruoyi.system.domain.vo.ChatGptsVo;
import org.ruoyi.system.listener.SSEEventSourceListener;
import org.ruoyi.system.service.*;
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
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private OpenAiStreamClient openAiStreamClient;

    private final ChatConfig chatConfig;


    private final IChatCostService chatService;

    private final IChatMessageService chatMessageService;

    private final ISysModelService sysModelService;

    private final ConfigService configService;

    private final IChatGptsService chatGptsService;

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();

    private static final String requestIdTemplate = "mycompany-%d";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
        SseEmitter sseEmitter = new SseEmitter(0L);
        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(sseEmitter);
        // 获取对话消息列表
        List<Message> messages = chatRequest.getMessages();

        try {
            String chatString = null;
            Object content = messages.get(messages.size() - 1).getContent();
            if (content instanceof List<?> listContent) {
                if (!listContent.isEmpty() && listContent.get(0) instanceof Content) {
                    chatString = ((Content) listContent.get(0)).getText();
                }
            } else if (content instanceof String) {
                chatString = (String) content;
            }

            String configValue = getKey("enabled");
            if (Boolean.parseBoolean(configValue)) {
                // 判断文本是否合规
                String type = textReview(chatString);
                // 审核状态 1 代表合法
                if (!"1".equals(type) && StringUtils.isNotEmpty(type)) {
                    throw new BaseException("文本不合规,请修改!");
                }
            }

            if (StpUtil.isLogin()) {
                LocalCache.CACHE.put("userId", getUserId());

                ChatMessageBo chatMessageBo = new ChatMessageBo();
                chatMessageBo.setUserId(getUserId());
                chatMessageBo.setModelName(chatRequest.getModel());
                chatMessageBo.setContent(chatString);

                String model = chatRequest.getModel();
                SysModel sysModel = sysModelService.selectModelByName(model);
                if (sysModel == null) {
                    // 如果模型不存在默认使用token扣费方式
                    processByToken(chatRequest.getModel(), chatString, chatMessageBo);
                } else {
                    openAiStreamClient = chatConfig.createOpenAiStreamClient(sysModel.getApiHost(), sysModel.getApiKey());
                    if (StringUtils.isNotEmpty(chatRequest.getAppId())) { // 设置应用的系统角色为描述
                        ChatGptsVo chatGptsVo = chatGptsService.queryById(Long.valueOf(chatRequest.getAppId()));
                        Message sysMessage = Message.builder().content(chatGptsVo.getSystemPrompt()).role(Message.Role.SYSTEM).build();
                        messages.add(0,sysMessage);
                    } else {
                        // 模型设置默认提示词
                        if (StringUtils.isNotEmpty(sysModel.getSystemPrompt())) {
                            Message sysMessage = Message.builder().content(sysModel.getSystemPrompt()).role(Message.Role.SYSTEM).build();
                            messages.add(0,sysMessage);
                        }
                    }
                    // 计费类型: 1 token扣费 2 次数扣费
                    if ("2".equals(sysModel.getModelType())) {
                        processByModelPrice(sysModel, chatMessageBo);
                    } else {
                        processByToken(chatRequest.getModel(), chatString, chatMessageBo);
                    }
                }
            }
            String zhipuValue = configService.getConfigValue("zhipu", "key");
            // 添加联网信息
            if(StringUtils.isNotEmpty(zhipuValue)){
                ClientV4 client = new ClientV4.Builder(zhipuValue)
                        .networkConfig(300, 100, 100, 100, TimeUnit.SECONDS)
                        .connectionPool(new okhttp3.ConnectionPool(8, 1, TimeUnit.SECONDS))
                        .build();

                SearchChatMessage jsonNodes = new SearchChatMessage();
                jsonNodes.setRole(Message.Role.USER.getName());
                jsonNodes.setContent(chatString);

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

                    webSearchApiResponse.setFlowable(null);// 打印前置空
                    webSearchApiResponse.setData(chatMessageAccumulator);
                }


                Message message = Message.builder().role(Message.Role.ASSISTANT).content(choices.get(1).getToolCalls().toString()).build();
                messages.add(message);
            }

            if ("openCmd".equals(chatRequest.getModel())) {
                sseEmitter.send(cmdPlugin(messages));
                sseEmitter.complete();
            } else if ("sqlPlugin".equals(chatRequest.getModel())) {
                sseEmitter.send(sqlPlugin(messages));
                sseEmitter.complete();
            } else {
                ChatCompletion completion = ChatCompletion
                        .builder()
                        .messages(messages)
                        .model(chatRequest.getModel())
                        .temperature(chatRequest.getTemperature())
                        .topP(chatRequest.getTop_p())
                        .stream(true)
                        .build();
                openAiStreamClient.streamChatCompletion(completion, openAIEventSourceListener);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            sendErrorEvent(sseEmitter, message);
            return sseEmitter;
        }
        return sseEmitter;
    }

    public String cmdPlugin(List<Message> messages) {
        CmdPlugin plugin = new CmdPlugin(CmdReq.class);
        // 插件名称
        plugin.setName("命令行工具");
        // 方法名称
        plugin.setFunction("openCmd");
        // 方法说明
        plugin.setDescription("提供一个命令行指令,比如<记事本>,指令使用中文");

        PluginAbstract.Arg arg = new PluginAbstract.Arg();
        // 参数名称
        arg.setName("cmd");
        // 参数说明
        arg.setDescription("命令行指令");
        // 参数类型
        arg.setType("string");
        arg.setRequired(true);
        plugin.setArgs(Collections.singletonList(arg));
        //有四个重载方法，都可以使用
        ChatCompletionResponse response = openAiStreamClient.chatCompletionWithPlugin(messages,"gpt-4o-mini",plugin);
        return response.getChoices().get(0).getMessage().getContent().toString();
    }

    public String sqlPlugin(List<Message> messages) {
        SqlPlugin plugin = new SqlPlugin(SqlReq.class);
        // 插件名称
        plugin.setName("数据库查询插件");
        // 方法名称
        plugin.setFunction("sqlPlugin");
        // 方法说明
        plugin.setDescription("提供一个用户名称查询余额信息");

        PluginAbstract.Arg arg = new PluginAbstract.Arg();
        // 参数名称
        arg.setName("username");
        // 参数说明
        arg.setDescription("用户名称");
        // 参数类型
        arg.setType("string");
        arg.setRequired(true);
        plugin.setArgs(Collections.singletonList(arg));
        //有四个重载方法，都可以使用
        ChatCompletionResponse response = openAiStreamClient.chatCompletionWithPlugin(messages,"gpt-4o-mini",plugin);
        return response.getChoices().get(0).getMessage().getContent().toString();
    }

    /**
     * 根据次数扣除余额
     *
     * @param model         模型信息
     * @param chatMessageBo 对话信息
     */
    private void processByModelPrice(SysModel model, ChatMessageBo chatMessageBo) {
        double cost = model.getModelPrice();
        chatService.deductUserBalance(getUserId(), cost);
        chatMessageBo.setDeductCost(cost);
        chatMessageService.insertByBo(chatMessageBo);
    }

    /**
     * 根据token扣除余额
     *
     * @param modelName     模型名称
     * @param text       消息内容
     * @param chatMessageBo 消息记录
     */
    private void processByToken(String modelName, String text, ChatMessageBo chatMessageBo) {
        int tokens = TikTokensUtil.tokens(modelName, text);
        chatMessageBo.setTotalTokens(tokens);
        chatService.deductToken(chatMessageBo);
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
    public String chat(ChatRequest chatRequest, String userId) {
         return  null;
    }

    /**
     * dall-e-3绘画接口
     *
     * @param request
     * @return
     */
    @Override
    public List<Item> dall3(Dall3Request request) {
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
        chatService.checkUserGrade();
        // DALL3 绘图模型
        Image image = Image.builder()
            .responseFormat(ResponseFormat.URL.getName())
            .model(Image.Model.DALL_E_3.getName())
            .prompt(request.getPrompt())
            .n(1)
            .quality(request.getQuality())
            .size(request.getSize())
            .style(request.getStyle())
            .build();
        ImageResponse imageResponse = openAiStreamClient.genImages(image);
        SysModel sysModel = sysModelService.selectModelByName(request.getModel());
        //chatService.deductUserBalance(getUserId(),sysModelList.get(0).getModelPrice());
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(Image.Model.DALL_E_3.getName());
        chatMessageBo.setContent(request.getPrompt());
        chatMessageBo.setDeductCost(sysModel.getModelPrice());
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
        return imageResponse.getData();
    }

    @Override
    public List<Item> wxDall(String prompt, String userId) {
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
        // DALL3 绘图模型
        Image image = Image.builder()
            .responseFormat(ResponseFormat.URL.getName())
            .model(Image.Model.DALL_E_3.getName())
            .prompt(prompt)
            .n(1)
            .build();
        ImageResponse imageResponse = openAiStreamClient.genImages(image);
        SysModel dall3 = sysModelService.selectModelByName("dall3");
        chatService.deductUserBalance(Long.valueOf(userId), 0.3);
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(Image.Model.DALL_E_3.getName());
        chatMessageBo.setContent(prompt);
        chatMessageBo.setDeductCost(dall3.getModelPrice());
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
        return imageResponse.getData();
    }

    /**
     * 获取用户Id
     *
     * @return
     */
    public Long getUserId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }

    @Override
    public UploadFileResponse upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot upload an empty MultipartFile");
        }
        if (!FileUtils.isValidFileExtention(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION)) {
            throw new IllegalStateException("File Extention not supported");
        }
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
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

    // 发送SSE错误事件的封装方法
    private void sendErrorEvent(SseEmitter sseEmitter, String errorMessage) {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
            .name("error")
            .data(errorMessage);
        try {
            sseEmitter.send(event);
        } catch (IOException e) {
            log.error("发送事件失败: {}", e.getMessage());
        }
        sseEmitter.complete();
    }

    /**
     * 文本内容审核
     *
     * @param msg
     * @return String
     * @Date 2023/5/27
     **/
    public String textReview(String msg) {
        String conclusionType = "";
        try {
            String text = URLEncoder.encode(msg);
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, "text=" + text);
            Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined?access_token=" + getAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
            JSONObject jsonObject = JSONObject.parseObject(response.body().string());
            conclusionType = jsonObject.getString("conclusionType");
        } catch (IOException e) {
            log.info("发生错误{}", e.getMessage());
        }
        return conclusionType;
    }

    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    public String getAccessToken() throws IOException {
        okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + getKey("apiKey")
            + "&client_secret=" + getKey("secretKey"));
        Request request = new Request.Builder()
            .url("https://aip.baidubce.com/oauth/2.0/token")
            .method("POST", body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return JSONObject.parseObject(response.body().string()).getString("access_token");
    }

    public String getKey(String key) {
        return configService.getConfigValue("review", key);
    }

    /**
     * 获取客户端的 IP 地址
     *
     * @param request HTTP 请求对象
     * @return 客户端的 IP 地址，如果无法获取则返回 "unknown"
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress.split(",")[0].trim();
        }

        ipAddress = request.getHeader("Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        ipAddress = request.getHeader("WL-Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        ipAddress = request.getHeader("HTTP_CLIENT_IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        ipAddress = request.getRemoteAddr();
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        return "unknown";
    }


    @Override
    public String translation(TranslationRequest translationRequest) {
        // 翻译模型固定为gpt-4o-mini
        translationRequest.setModel("gpt-4o-mini");
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(translationRequest.getModel());
        chatMessageBo.setContent(translationRequest.getPrompt());
        chatMessageBo.setDeductCost(0.01);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
        List<Message> messageList = new ArrayList<>();
        Message sysMessage = Message.builder().role(Message.Role.SYSTEM).content("你是一位精通各国语言的翻译大师\n" +
            "\n" +
            "请将用户输入词语翻译成{" + translationRequest.getTargetLanguage() + "}\n" +
            "\n" +
            "==示例输出==\n" +
            "**原文** : <这里显示要翻译的原文信息>\n" +
            "**翻译** : <这里显示翻译成英语的结果>\n" +
            "==示例结束==\n" +
            "\n" +
            "注意：请严格按示例进行输出，返回markdown格式").build();
        messageList.add(sysMessage);
        Message message = Message.builder().role(Message.Role.USER).content(translationRequest.getPrompt()).build();
        messageList.add(message);
        ChatCompletionResponse chatCompletionResponse = null;
        try {
           ChatCompletion chatCompletion = ChatCompletion
               .builder()
               .messages(messageList)
               .model(translationRequest.getModel())
               .stream(false)
               .build();
           chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);
       }catch (Exception e) {
           log.error(e.getMessage());
       }

        return chatCompletionResponse.getChoices().get(0).getMessage().getContent().toString();
    }

    @Override
    public SseEmitter ollamaChat(ChatRequest chatRequest) {
        String[] parts = chatRequest.getModel().split("ollama-");
        SysModel sysModel = sysModelService.selectModelByName(chatRequest.getModel());
        final SseEmitter emitter = new SseEmitter();
        String host = sysModel.getApiHost();
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
}
