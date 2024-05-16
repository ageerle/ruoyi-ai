package com.xmzs.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.entity.Tts.TextToSpeech;
import com.xmzs.common.chat.entity.chat.*;
import com.xmzs.common.chat.entity.files.UploadFileResponse;
import com.xmzs.common.chat.entity.images.Image;
import com.xmzs.common.chat.entity.images.ImageResponse;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.chat.entity.whisper.WhisperResponse;
import com.xmzs.common.chat.openai.OpenAiStreamClient;
import com.xmzs.common.chat.utils.TikTokensUtil;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.system.domain.SysUser;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.domain.bo.SysModelBo;
import com.xmzs.system.domain.vo.SysModelVo;
import com.xmzs.system.listener.SSEEventSourceListener;
import com.xmzs.system.mapper.SysUserMapper;
import com.xmzs.system.service.IChatCostService;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.ISseService;
import com.xmzs.system.service.ISysModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @date 2023-04-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements ISseService {

    private final OpenAiStreamClient openAiStreamClient;

    private final IChatCostService chatService;

    private final SysUserMapper sysUserMapper;

    private final IChatMessageService chatMessageService;

    private final ISysModelService sysModelService;

    @Value("${chat.apiKey}")
    private String apiKey;
    @Value("${chat.apiHost}")
    private String apiHost;

    @Override
    public SseEmitter sseChat(ChatRequest chatRequest) {
        LocalCache.CACHE.put("userId",getUserId());
        SseEmitter sseEmitter = new SseEmitter(0L);
        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(sseEmitter);
        // 获取对话消息列表
        List<Message> msgList = chatRequest.getMessages();

        // 图文识别上下文信息
        List<Content> contentList = chatRequest.getImageContent();
        // 消息记录
        Message message = msgList.get(msgList.size() - 1);
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(chatRequest.getModel());
        chatMessageBo.setContent(message.getContent());
        try{
            if(!chatRequest.getModel().startsWith("gpt-3.5")){
                // 判断用户是否付费
                checkUserGrade();
            }
            //根据模型名称查询模型信息
            SysModelBo sysModelBo = new SysModelBo();
            sysModelBo.setModelName(chatRequest.getModel());
            List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);
            if (CollectionUtil.isEmpty(sysModelList)) {
                // 如果模型不存在默认使用token扣费方式
                processByToken(chatRequest.getModel(), msgList, chatMessageBo);
            } else {
                // 模型设置默认提示词
                SysModelVo firstModel = sysModelList.get(0);
                if (StringUtils.isNotEmpty(firstModel.getSystemPrompt())) {
                    Message sysMessage = Message.builder().content(firstModel.getSystemPrompt()).role(Message.Role.SYSTEM).build();
                    // 假设 msgList 不为空并且至少有一个元素
                    if (msgList.get(0).equals(sysMessage)) {
                        // 如果第一个元素与sysMessage相等，替换第一个元素
                        msgList.set(0, sysMessage);
                    } else {
                        // 如果不相等，将sysMessage插入到列表的第一个位置
                        msgList.add(0, sysMessage);
                    }
                }
                // 计费类型: 1 token扣费 2 次数扣费
                if ("2".equals(firstModel.getModelType())) {
                    processByModelPrice(firstModel, chatMessageBo);
                } else {
                    processByToken(chatRequest.getModel(), msgList, chatMessageBo);
                }
            }
        } catch (Exception e) {
            sendErrorEvent(sseEmitter, e.getMessage());
            return sseEmitter;
        }
        // 图文识别模型
        if (ChatCompletion.Model.GPT_4_VISION_PREVIEW.getName().equals(chatRequest.getModel())) {
            MessagePicture messagePicture = MessagePicture.builder().role(Message.Role.USER.getName()).content(contentList).build();
            ChatCompletionWithPicture chatCompletion = ChatCompletionWithPicture
                .builder()
                .messages(Collections.singletonList(messagePicture))
                .model(chatRequest.getModel())
                .temperature(chatRequest.getTemperature())
                .topP(chatRequest.getTop_p())
                .stream(true)
                .build();
            openAiStreamClient.streamChatCompletion(chatCompletion, openAIEventSourceListener);
        } else {
            ChatCompletion completion = ChatCompletion
                .builder()
                .messages(msgList)
                .model(chatRequest.getModel())
                .temperature(chatRequest.getTemperature())
                .topP(chatRequest.getTop_p())
                .stream(true)
                .build();
            openAiStreamClient.streamChatCompletion(completion, openAIEventSourceListener);
        }
        return sseEmitter;
    }

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
     * @param msgList       消息列表
     * @param chatMessageBo 消息记录
     */
    private void processByToken(String modelName, List<Message> msgList, ChatMessageBo chatMessageBo) {
        int tokens = TikTokensUtil.tokens(modelName, msgList);
        chatMessageBo.setTotalTokens(tokens);
        chatService.deductToken(chatMessageBo);
    }

    /**
     * 文字转语音
     *
     */
    @Override
    public ResponseEntity<Resource> textToSpeed(TextToSpeech textToSpeech) {
        try (ResponseBody body = openAiStreamClient.textToSpeech(textToSpeech)) {
            if (body != null) {
                // 将ResponseBody转换为InputStreamResource
                InputStreamResource resource = new InputStreamResource(body.byteStream());

                // 创建并返回ResponseEntity
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/mpeg")) // 假设是MP3文件
                        .body(resource);

            } else {
                // 如果ResponseBody为空，返回404状态码
                return ResponseEntity.notFound().build();
            }
        }
    }

    /**
     * 语音转文字
     *
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
    public String chat(ChatRequest chatRequest) {
        Message message = Message.builder().role(Message.Role.USER).content(chatRequest.getPrompt()).build();
        ChatCompletion chatCompletion = ChatCompletion
            .builder()
            .messages(Collections.singletonList(message))
            .model(chatRequest.getModel())
            .build();
        ChatCompletionResponse chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);
        return chatCompletionResponse.getChoices().get(0).getMessage().getContent();
    }

    /**
     * dall-e-3绘画接口
     *
     * @param request
     * @return
     */
    public List<Item> dall3(Dall3Request request) {
        checkUserGrade();
        // DALL3 绘图模型
        Image image = Image.builder()
            .responseFormat(com.xmzs.common.chat.entity.images.ResponseFormat.URL.getName())
            .model(Image.Model.DALL_E_3.getName())
            .prompt(request.getPrompt())
            .n(1)
            .quality(request.getQuality())
            .size(request.getSize())
            .style(request.getStyle())
            .build();
        ImageResponse imageResponse = openAiStreamClient.genImages(image);

        // 扣除费用
        if(Objects.equals(request.getSize(), "1792x1024") || Objects.equals(request.getSize(), "1024x1792")){
            chatService.deductUserBalance(getUserId(),OpenAIConst.DALL3_HD_COST);
        }else {
            chatService.deductUserBalance(getUserId(),OpenAIConst.DALL3_COST);
        }
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(Image.Model.DALL_E_3.getName());
        chatMessageBo.setContent(request.getPrompt());
        chatMessageBo.setDeductCost(OpenAIConst.GPT4_COST);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
        return imageResponse.getData();
    }

    /**
     * 判断用户是否付费
     */
    @Override
    public void checkUserGrade() {
        SysUser sysUser = sysUserMapper.selectById(getUserId());
        if("0".equals(sysUser.getUserGrade())){
            throw new BaseException("免费用户暂时不支持此模型,请切换gpt-3.5-turbo模型或者点击《进入市场选购您的商品》充值后使用!");
        }
    }

    /**
     * 获取用户Id
     *
     * @return
     */
    public Long getUserId(){
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }

    @Override
    public UploadFileResponse upload(MultipartFile file) {
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
}
