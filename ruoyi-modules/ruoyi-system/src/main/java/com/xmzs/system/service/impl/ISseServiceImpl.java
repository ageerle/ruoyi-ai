package com.xmzs.system.service.impl;

import cn.hutool.core.date.DateUtil;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.domain.request.MjTaskRequest;
import com.xmzs.common.chat.entity.Tts.TextToSpeech;
import com.xmzs.common.chat.entity.chat.*;
import com.xmzs.common.chat.entity.images.Image;
import com.xmzs.common.chat.entity.images.ImageResponse;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.chat.entity.whisper.WhisperResponse;
import com.xmzs.common.chat.openai.OpenAiStreamClient;
import com.xmzs.common.chat.utils.TikTokensUtil;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.system.domain.SysUser;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.listener.SSEEventSourceListener;
import com.xmzs.system.mapper.SysUserMapper;
import com.xmzs.system.service.IChatService;
import com.xmzs.system.service.IChatMessageService;

import com.xmzs.system.service.ISseService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.core.io.InputStreamResource;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.springframework.http.MediaType;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import okhttp3.ResponseBody;

/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @date 2023-04-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ISseServiceImpl implements ISseService {

    private final OpenAiStreamClient openAiStreamClient;


    private final IChatService IChatService;

    private final SysUserMapper sysUserMapper;

    private final IChatMessageService chatMessageService;

    @Override
    public SseEmitter sseChat(ChatRequest chatRequest) {
        LocalCache.CACHE.put("userId",getUserId());
        SseEmitter sseEmitter = new SseEmitter(0L);
        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(sseEmitter);
        checkUserGrade(sseEmitter, chatRequest.getModel());
        // 获取对话消息列表
        List<Message> msgList = chatRequest.getMessages();
        // 图文识别上下文信息
        List<Content> contentList = chatRequest.getContent();
        // 消息记录
        Message message = msgList.get(msgList.size() - 1);
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(chatRequest.getModel());
        chatMessageBo.setContent(message.getContent());

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
            // 扣除图文对话费用
            IChatService.deductUserBalance(getUserId(),OpenAIConst.GPT4_COST);
            String text = contentList.get(contentList.size() - 1).getText();
            // 保存消息记录
            chatMessageBo.setContent(text);
            chatMessageBo.setDeductCost(OpenAIConst.GPT4_COST);
            chatMessageBo.setTotalTokens(0);
            chatMessageService.insertByBo(chatMessageBo);
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

            if("gpt-4-all".equals(chatRequest.getModel())
                || chatRequest.getModel().startsWith("gpt-4-gizmo")
                || chatRequest.getModel().startsWith("net")
            ){
                chatMessageBo.setDeductCost(0.0);
                // 保存消息记录
                chatMessageService.insertByBo(chatMessageBo);
            }else {
                // 扣除余额
                int tokens = TikTokensUtil.tokens(chatRequest.getModel(), msgList);
                chatMessageBo.setTotalTokens(tokens);
                IChatService.deductToken(chatMessageBo);
            }
        }
        return sseEmitter;
    }

    /**
     * 文字转语音
     *
     */
    @Override
    public ResponseEntity<Resource> textToSpeed(TextToSpeech textToSpeech) {
        ResponseBody body = openAiStreamClient.textToSpeechClone(textToSpeech);
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
        checkUserGrade(null,"");
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
            IChatService.deductUserBalance(getUserId(),OpenAIConst.DALL3_HD_COST);
        }else {
            IChatService.deductUserBalance(getUserId(),OpenAIConst.DALL3_COST);
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

    @Override
    public void mjTask(MjTaskRequest mjTaskRequest) {
        // 检验是否是付费用户
        checkUserGrade(null,"");
        //扣除费用
        IChatService.deductUserBalance(getUserId(),0.5);
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName("mj");
        chatMessageBo.setContent(mjTaskRequest.getPrompt());
        chatMessageBo.setDeductCost(0.5);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
    }

    /**
     * 判断用户是否付费
     */
    public void checkUserGrade(SseEmitter emitter, String model) {
        SysUser sysUser = sysUserMapper.selectById(getUserId());
        if(StringUtils.isEmpty(model)){
            if("0".equals(sysUser.getUserGrade())){
                throw new ServiceException("免费用户暂时不支持此模型,请切换gpt-3.5-turbo模型或者点击《进入市场选购您的商品》充值后使用!",500);
            }
        }
        // TODO 添加枚举
        if ("0".equals(sysUser.getUserGrade()) && !ChatCompletion.Model.GPT_3_5_TURBO.getName().equals(model)) {
            // 创建并发送一个名为 "error" 的事件，带有错误消息和状态码
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("error") // 客户端将监听这个事件名
                .data("免费用户暂时不支持此模型,请切换gpt-3.5-turbo模型或者点击《进入市场选购您的商品》充值后使用!");
            try {
                emitter.send(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            emitter.complete();
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

    public static double calculateMjCost(String speed) {
        return switch (speed) {
            case "mj-relax" -> 0.2; // Handles null and "mj-relax"
            case "mj-fast" -> 0.5;
            case "mj-turbo" -> 1.0;
            default -> 0.5; // Default cost if none of the above speeds match
        };
    }

}
