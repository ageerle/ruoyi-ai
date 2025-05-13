package org.ruoyi.chat.service.chat.impl;

import io.github.imfangs.dify.client.DifyClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.model.DifyConfig;
import io.github.imfangs.dify.client.model.chat.ChatMessage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * dify 聊天管理
 *
 * @author ageer
 */
@Service
@Slf4j
public class DifyServiceImpl implements IChatService {

    @Autowired
    private IChatModelService chatModelService;

    @Override
    public SseEmitter chat(ChatRequest chatRequest, SseEmitter emitter) {
        ChatModelVo chatModelVo = chatModelService.selectModelByName(chatRequest.getModel());

        // 使用自定义配置创建客户端
        DifyConfig config = DifyConfig.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .connectTimeout(5000)
                .readTimeout(60000)
                .writeTimeout(30000)
                .build();
        DifyClient chatClient = DifyClientFactory.createClient(config);

        // 创建聊天消息
        ChatMessage message = ChatMessage.builder()
                .query(chatRequest.getPrompt())
                .user(chatRequest.getUserId().toString())
                .responseMode(ResponseMode.STREAMING)
                .build();

        // 发送流式消息
        try {
            chatClient.sendChatMessageStream(message, new ChatStreamCallback() {
                @SneakyThrows
                @Override
                public void onMessage(MessageEvent event) {
                    emitter.send(event.getAnswer());
                    log.info("收到消息片段: {}", event.getAnswer());
                }

                @Override
                public void onMessageEnd(MessageEndEvent event) {
                    emitter.complete();
                    log.info("消息结束，完整消息ID: {}", event.getMessageId());
                }

                @Override
                public void onError(ErrorEvent event) {
                    System.err.println("错误: " + event.getMessage());
                }

                @Override
                public void onException(Throwable throwable) {
                    System.err.println("异常: " + throwable.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("dify请求失败：{}", e.getMessage());
        }

        return emitter;
    }

    @Override
    public String getCategory() {
        return ChatModeType.DIFY.getCode();
    }

}
