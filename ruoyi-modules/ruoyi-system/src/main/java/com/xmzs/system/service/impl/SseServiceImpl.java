package com.xmzs.system.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.IterableStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.chat.domain.request.ChatRequest;
import com.xmzs.common.chat.domain.request.Dall3Request;
import com.xmzs.common.chat.entity.chat.*;
import com.xmzs.common.chat.entity.images.Image;
import com.xmzs.common.chat.entity.images.ImageResponse;
import com.xmzs.common.chat.entity.images.Item;
import com.xmzs.common.chat.entity.images.ResponseFormat;
import com.xmzs.common.chat.openai.OpenAiStreamClient;
import com.xmzs.common.chat.utils.TikTokensUtil;
import com.xmzs.common.core.domain.model.LoginUser;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.common.core.exception.base.BaseException;

import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.satoken.utils.LoginHelper;
import com.xmzs.common.translation.annotation.Translation;
import com.xmzs.system.domain.SysUser;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.listener.SSEEventSourceListener;
import com.xmzs.system.mapper.SysUserMapper;
import com.xmzs.system.service.ChatService;
import com.xmzs.system.service.IChatMessageService;

import com.xmzs.system.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.azure.ai.openai.models.ImageGenerationOptions;
import com.azure.core.models.ResponseError;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ImageGenerationData;
import com.azure.ai.openai.models.ImageGenerationOptions;
import com.azure.ai.openai.models.ImageGenerations;
import com.azure.core.credential.AzureKeyCredential;
/**
 * 描述：
 *
 * @author https:www.unfbx.com
 * @date 2023-04-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {
    private final OpenAiStreamClient openAiStreamClient;

    private final ChatService chatService;

    private final SysUserMapper sysUserMapper;

    private final IChatMessageService chatMessageService;

    @Value("${transit.apiKey}")
    private String API_KEY;

    @Value("${transit.apiHost}")
    private String API_HOST;

    private static final String DONE_SIGNAL = "[DONE]";

    @Override
    @Transactional
    public SseEmitter sseChat(ChatRequest chatRequest) {
        LocalCache.CACHE.put("userId",getUserId());
        SseEmitter sseEmitter = new SseEmitter(0L);
        SSEEventSourceListener openAIEventSourceListener = new SSEEventSourceListener(sseEmitter);
        checkUserGrade(sseEmitter, chatRequest.getModel());
        // 获取对话消息列表
        List<Message> msgList = chatRequest.getMessages();
        // 图文识别上下文信息
        List<Content> contentList = chatRequest.getContent();
        // 图文识别模型
        if (ChatCompletion.Model.GPT_4_VISION_PREVIEW.getName().equals(chatRequest.getModel())) {
            MessagePicture message = MessagePicture.builder().role(Message.Role.USER.getName()).content(contentList).build();
            ChatCompletionWithPicture chatCompletion = ChatCompletionWithPicture
                .builder()
                .messages(Collections.singletonList(message))
                .model(chatRequest.getModel())
                .temperature(chatRequest.getTemperature())
                .topP(chatRequest.getTop_p())
                .stream(true)
                .build();
            openAiStreamClient.streamChatCompletion(chatCompletion, openAIEventSourceListener);
            // 扣除图文对话费用
            chatService.deductUserBalance(getUserId(),OpenAIConst.GPT4_COST);

            String text = contentList.get(contentList.size() - 1).getText();
            // 保存消息记录
            ChatMessageBo chatMessageBo = new ChatMessageBo();
            chatMessageBo.setUserId(getUserId());
            chatMessageBo.setModelName(chatRequest.getModel());
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
            Message message = msgList.get(msgList.size() - 1);
            // 扣除余额
            int tokens = TikTokensUtil.tokens(chatRequest.getModel(), msgList);
            ChatMessageBo chatMessageBo = new ChatMessageBo();
            chatMessageBo.setUserId(getUserId());
            chatMessageBo.setModelName(chatRequest.getModel());
            chatMessageBo.setContent(message.getContent());
            chatMessageBo.setTotalTokens(tokens);
            chatService.deductToken(chatMessageBo);
        }
        return sseEmitter;
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
            .responseFormat(ResponseFormat.URL.getName())
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
        // 保存扣费记录
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
    public void mjTask() {
        // 检验是否是免费用户
        checkUserGrade(null,"");
        chatService.deductUserBalance(getUserId(),OpenAIConst.MJ_COST);
        // 保存扣费记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName("mj");
        chatMessageBo.setContent("mj绘图");
        chatMessageBo.setDeductCost(OpenAIConst.GPT4_COST);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
    }

    /**
     * 中转接口
     *
     * @param chatRequest
     * @return
     */
    @Override
    public SseEmitter transitChat(ChatRequest chatRequest) {
        // 获取对话消息列表
        List<Message> msgList = chatRequest.getMessages();
        Message message = msgList.get(msgList.size() - 1);
        SseEmitter emitter = new SseEmitter(0L);
        checkUserGrade(emitter, chatRequest.getModel());
        ChatCompletion completion = ChatCompletion
            .builder()
            .messages(chatRequest.getMessages())
            .model(chatRequest.getModel())
            .temperature(chatRequest.getTemperature())
            .topP(chatRequest.getTop_p())
            .stream(true)
            .build();
        // 启动一个新的线程来处理数据流
        new Thread(() -> {
            // 启动一个新的线程来处理数据流
            try {
                ObjectMapper mapper = new ObjectMapper();
                String requestBody = mapper.writeValueAsString(completion);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_HOST + "v1/chat/completions"))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
                // 发送请求并获取响应体作为InputStream
                HttpResponse<InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
                // 使用正确的字符编码将InputStream包装为InputStreamReader，然后创建BufferedReader
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.replace("data: ", "");
                        emitter.send(data, MediaType.TEXT_PLAIN);
                        if (data.equals(DONE_SIGNAL)) {
                            //成功响应
                            emitter.complete();
                        }
                    }
                }
                // 关闭资源
                reader.close();
            } catch (Exception e) {
                emitter.complete();
                throw new ServiceException("调用中转接口失败:"+e.getMessage());
            }
        }).start();
        chatService.deductUserBalance(getUserId(),OpenAIConst.GPT4_COST);
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(chatRequest.getModel());
        chatMessageBo.setContent(message.getContent());
        chatMessageBo.setDeductCost(OpenAIConst.GPT4_COST);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
        return emitter;
    }

    public static void main(String[] args) {
        String azureOpenaiKey = "-";
        String endpoint = "-";
        String deploymentOrModelName = "-";

        OpenAIClient client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(azureOpenaiKey))
            .buildClient();

        ImageGenerationOptions imageGenerationOptions = new ImageGenerationOptions(
            "A drawing of the Seattle skyline in the style of Van Gogh");
        ImageGenerations images = client.getImageGenerations(deploymentOrModelName, imageGenerationOptions);

        for (ImageGenerationData imageGenerationData : images.getData()) {
            System.out.printf(
                "Image location URL that provides temporary access to download the generated image is %s.%n",
                imageGenerationData.getUrl());
        }
    }

    public SseEmitter azureChat(ChatRequest chatRequest) {
        String azureOpenaiKey = "-";
        String endpoint = "-";
        String deploymentOrModelId = "-";
        OpenAIClient client = new OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(new AzureKeyCredential(azureOpenaiKey))
            .buildClient();
        final SseEmitter emitter = new SseEmitter();
        // 使用线程池异步执行
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(() -> {
            try {
                // 获取对话消息列表
                List<Message> chatMessages = chatRequest.getMessages();
                List<ChatRequestMessage> messages = new ArrayList<>();
                chatMessages.forEach(
                    e->{
                        ChatRequestMessage chatMessage;
                        if(Message.Role.SYSTEM.getName().equals(e.getRole())){
                             chatMessage = new ChatRequestSystemMessage(e.getContent());
                        }else {
                            chatMessage = new ChatRequestUserMessage(e.getContent());
                        }
                        messages.add(chatMessage);
                    }
                );
                // 获取流式响应
                IterableStream<ChatCompletions> chatCompletionsStream = client.getChatCompletionsStream(deploymentOrModelId, new ChatCompletionsOptions(messages));

                // 遍历流式响应并发送到客户端
                for (ChatCompletions chatCompletion : chatCompletionsStream) {

                    if(CollectionUtil.isEmpty(chatCompletion.getChoices())){
                        continue;
                    }
                    log.info("json ======{}", JSONUtil.toJsonStr(chatCompletion));
                    emitter.send(chatCompletion);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
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

}
