package org.ruoyi.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatRequestModel;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.utils.Options;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.chat.config.ChatConfig;
import org.ruoyi.common.chat.config.LocalCache;
import org.ruoyi.common.chat.domain.request.ChatRequest;
import org.ruoyi.common.chat.domain.request.Dall3Request;
import org.ruoyi.common.chat.entity.Tts.TextToSpeech;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;
import org.ruoyi.common.chat.entity.chat.Content;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.entity.files.UploadFileResponse;
import org.ruoyi.common.chat.entity.images.Image;
import org.ruoyi.common.chat.entity.images.ImageResponse;
import org.ruoyi.common.chat.entity.images.Item;
import org.ruoyi.common.chat.entity.images.ResponseFormat;
import org.ruoyi.common.chat.entity.whisper.WhisperResponse;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.utils.TikTokensUtil;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.bo.SysModelBo;
import org.ruoyi.system.domain.request.translation.TranslationRequest;
import org.ruoyi.system.domain.vo.SysModelVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.listener.SSEEventSourceListener;
import org.ruoyi.system.service.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.ollama4j.utils.OptionsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements ISseService {

    private OpenAiStreamClient openAiStreamClient;

    private final ChatConfig chatConfig;

    private final IChatCostService chatService;

    private final IChatMessageService chatMessageService;

    private final ISysModelService sysModelService;

    private final ISysUserService userService;

    private final ConfigService configService;

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();

    private final ISysPackagePlanService sysPackagePlanService;


    @Override
    public SseEmitter sseChat(ChatRequest chatRequest, HttpServletRequest request) {
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
        SseEmitter sseEmitter = new SseEmitter(0L);
        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(sseEmitter);
        // 获取对话消息列表
        List<Message> messages = chatRequest.getMessages();
        try {
            if (StpUtil.isLogin()) {
                SysUserVo sysUserVo = userService.selectUserById(getUserId());
//                if (!checkModel(sysUserVo.getUserPlan(), chatRequest.getModel())) {
//                    throw new BaseException("当前套餐不支持此模型!");
//                }
                LocalCache.CACHE.put("userId", getUserId());

                Object content = messages.get(messages.size() - 1).getContent();

                String chatString = "";
                if (content instanceof List<?> listContent) {
                    if (!listContent.isEmpty() && listContent.get(0) instanceof Content) {
                        chatString = ((Content) listContent.get(0)).getText();
                    }
                } else if (content instanceof String) {
                    chatString = (String) content;
                }

                ChatMessageBo chatMessageBo = new ChatMessageBo();
                chatMessageBo.setUserId(getUserId());
                chatMessageBo.setModelName(chatRequest.getModel());

                chatMessageBo.setContent(chatString);
                String configValue = getKey("enabled");
                if (Boolean.parseBoolean(configValue)) {
                    // 判断文本是否合规
                    String type = textReview(chatString);
                    // 审核状态 1 代表合法
                    if (!"1".equals(type) && StringUtils.isNotEmpty(type)) {
                        throw new BaseException("文本不合规,请修改!");
                    }
                }
                //根据模型名称查询模型信息
                SysModelBo sysModelBo = new SysModelBo();
                // 如果是gpts系列模型
                if (chatRequest.getModel().startsWith("gpt-4-gizmo")) {
                    sysModelBo.setModelName("gpt-4-gizmo");
                } else {
                    sysModelBo.setModelName(chatRequest.getModel());
                }
                List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);

                if (CollectionUtil.isEmpty(sysModelList)) {
                    // 如果模型不存在默认使用token扣费方式
                    processByToken(chatRequest.getModel(), chatString, chatMessageBo);
                } else {
                    openAiStreamClient = chatConfig.createOpenAiStreamClient(sysModelList.get(0).getApiHost(), sysModelList.get(0).getApiKey());
                    // 模型设置默认提示词
                    SysModelVo firstModel = sysModelList.get(0);
                    if (StringUtils.isNotEmpty(firstModel.getSystemPrompt())) {
                        Message sysMessage = Message.builder().content(firstModel.getSystemPrompt()).role(Message.Role.SYSTEM).build();
                        messages.add(sysMessage);
                    }
                    // 计费类型: 1 token扣费 2 次数扣费
                    if ("2".equals(firstModel.getModelType())) {
                        processByModelPrice(firstModel, chatMessageBo);
                    } else {
                       processByToken(chatRequest.getModel(), chatString, chatMessageBo);
                    }
                }
            }

//            else {
//
//                    // 初始请求次数
//                    int number = 1;
//                    // 获取请求IP
//                    String realIp = getClientIpAddress(request);
//                    // 根据IP获取次数
//                    Integer requestNumber = RedisUtils.getCacheObject(realIp);
//                    if (requestNumber == null) {
//                        // 记录ip使用次数
//                        RedisUtils.setCacheObject(realIp, number);
//                    } else {
//                        String configValue = configService.getConfigValue("mail", "free");
//                        if (requestNumber > Integer.parseInt(configValue)) {
//                            throw new BaseException("剩余次数不足，请充值后使用");
//                        }
//                        RedisUtils.setCacheObject(realIp, requestNumber + 1);
//                    }
//
//            }
            ChatCompletion completion = ChatCompletion
                .builder()
                .messages(messages)
                .model(chatRequest.getModel())
                .temperature(chatRequest.getTemperature())
                .topP(chatRequest.getTop_p())
                .stream(true)
                .build();
            openAiStreamClient.streamChatCompletion(completion, openAIEventSourceListener);
        } catch (Exception e) {
            String message = e.getMessage();
            sendErrorEvent(sseEmitter, message);
            return sseEmitter;
        }
        return sseEmitter;
    }


//    /**
//     * 查当前用户是否可以调用此模型
//     *
//     * @param planId
//     * @return
//     */
//    public Boolean checkModel(String planId, String modelName) {
//        SysPackagePlanBo sysPackagePlanBo = new SysPackagePlanBo();
//        if (modelName.startsWith("gpt-4-gizmo")) {
//            modelName = "gpt-4-gizmo";
//        }
//        if (StringUtils.isEmpty(planId)) {
//            sysPackagePlanBo.setName("Visitor");
//        } else if ("Visitor".equals(planId) || "Free".equals(planId)) {
//            sysPackagePlanBo.setName(planId);
//        } else {
//            // sysPackagePlanBo.setId(Long.valueOf(planId));
//            return true;
//        }
//
//        SysPackagePlanVo sysPackagePlanVo = sysPackagePlanService.queryList(sysPackagePlanBo).get(0);
//        // 将字符串转换为数组
//        String[] array = sysPackagePlanVo.getPlanDetail().split(",");
//        return Arrays.asList(array).contains(modelName);
//    }

    /**
     * 根据次数扣除余额
     *
     * @param model         模型信息
     * @param chatMessageBo 对话信息
     */
    private void processByModelPrice(SysModelVo model, ChatMessageBo chatMessageBo) {
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
//        chatService.deductUserBalance(Long.valueOf(userId), 0.01);
//        // 保存消息记录
//        ChatMessageBo chatMessageBo = new ChatMessageBo();
//        chatMessageBo.setUserId(Long.valueOf(userId));
//        chatMessageBo.setModelName(ChatCompletion.Model.GPT_3_5_TURBO.getName());
//        chatMessageBo.setContent(chatRequest.getPrompt());
//        chatMessageBo.setDeductCost(0.01);
//        chatMessageBo.setTotalTokens(0);
//        chatMessageService.insertByBo(chatMessageBo);
//
//        openAiStreamClient = chatConfig.getOpenAiStreamClient();
//        Message message = Message.builder().role(Message.Role.USER).content(chatRequest.getPrompt()).build();
//        ChatCompletion chatCompletion = ChatCompletion
//            .builder()
//            .messages(Collections.singletonList(message))
//            .model(chatRequest.getModel())
//            .build();
//        ChatCompletionResponse chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);
//        return chatCompletionResponse.getChoices().get(0).getMessage().getContent();
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
        SysModelBo sysModelBo = new SysModelBo();
        sysModelBo.setModelName(request.getModel());
        List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);
        //chatService.deductUserBalance(getUserId(),sysModelList.get(0).getModelPrice());
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(Image.Model.DALL_E_3.getName());
        chatMessageBo.setContent(request.getPrompt());
        chatMessageBo.setDeductCost(sysModelList.get(0).getModelPrice());
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
        SysModelBo sysModelBo = new SysModelBo();
        sysModelBo.setModelName("dall3");
        List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);
        chatService.deductUserBalance(Long.valueOf(userId), 0.3);
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(Image.Model.DALL_E_3.getName());
        chatMessageBo.setContent(prompt);
        chatMessageBo.setDeductCost(sysModelList.get(0).getModelPrice());
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

        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(translationRequest.getModel());
        chatMessageBo.setContent(translationRequest.getPrompt());
        chatMessageBo.setDeductCost(0.01);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);

        openAiStreamClient = chatConfig.getOpenAiStreamClient();

        List<Message> messageList = new ArrayList<>();

        Message sysMessage = Message.builder().role(Message.Role.SYSTEM).content("作为英汉翻译，您的任务是准确地在两种语言之间翻译文本。翻译时，请注意上下文，准确解释成语和谚语。如果连续收到多个英文单词，请默认将其翻译成中文句子。但如果前面有'phrase:’，则应翻译为短语;如果有'norma!:'，则翻译为多个无关的单词。您的翻译应接近母语者的水平，并考虑用户要求的特定语言风格或语气。避免使用冒犯性词汇，必要时用x替换。提供翻译时，请用中文解释每句话的时态、从句、主语、谓语、宾语、特殊短语和谚语对于需要翻译的短语或单词，请提供来源(词典)。如果要求翻译多个短语，请用|符号分隔。请记住:您是英汉翻译，不是汉汉翻译或英英翻译。提交前请仔细检查和修订答案,回复控制在50字以内").build();
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
        final SseEmitter emitter = new SseEmitter();
        String host = "http://localhost:11434/";

        List<Message> msgList = chatRequest.getMessages();
        Message message = msgList.get(msgList.size() - 1);

        OllamaAPI ollamaAPI = new OllamaAPI(host);

        ollamaAPI.setRequestTimeoutSeconds(100);

        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance("qwen2.5:7b");

        OllamaChatRequestModel requestModel = builder
            .withMessage(OllamaChatMessageRole.USER,
                message.getContent().toString())
            .build();


        // 异步执行 Ollama API 调用
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
                ollamaAPI.chat(requestModel, streamHandler);
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
