package org.ruoyi.common.chat.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.config.LocalCache;
import org.ruoyi.common.chat.entity.chat.ChatCompletion;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.holder.WebSocketSessionHolder;
import org.ruoyi.common.chat.listener.WebSocketEventListener;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.utils.WebSocketUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocketHandler 实现类
 *
 * @author zendwang
 */
@Slf4j
public class PlusWebSocketHandler extends AbstractWebSocketHandler {

    /**
     * 连接成功后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSessionHolder.addSession(session.getId(), session);
    }

    /**
     * 处理发送来的文本消息
     *
     * @param session
     * @param message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketEventListener eventSourceListener = new WebSocketEventListener(session);
        String messageContext = (String) LocalCache.CACHE.get(session.getId());
        List<Message> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(messageContext)) {
            messages = JSONUtil.toList(messageContext, Message.class);
            // 上下文长度
            int contextSize=10;
            if (messages.size() >= contextSize) {
                messages = messages.subList(1, contextSize);
            }
            Message currentMessage = Message.builder().content(message.getPayload()).role(Message.Role.USER).build();
            messages.add(currentMessage);
        } else {
            Message currentMessage = Message.builder().content(message.getPayload()).role(Message.Role.USER).build();
            messages.add(currentMessage);
        }
        ChatCompletion chatCompletion = ChatCompletion
            .builder()
            .model(ChatCompletion.Model.GPT_3_5_TURBO.getName())
            .messages(messages)
            .temperature(0.2)
            .stream(true)
            .build();
        OpenAiStreamClient openAiStreamClient=(OpenAiStreamClient) SpringUtils.context().getBean("openAiStreamClient");
        openAiStreamClient.streamChatCompletion(chatCompletion, eventSourceListener);
        LocalCache.CACHE.put(session.getId(), JSONUtil.toJsonStr(messages), LocalCache.TIMEOUT);
    }


    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
    }

    /**
     * 心跳监测的回复
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        WebSocketUtils.sendPongMessage(session);
    }

    /**
     * 连接出错时
     *
     * @param session
     * @param exception
     * @throws Exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[transport error] sessionId: {} , exception:{}", session.getId(), exception.getMessage());
    }

    /**
     * 连接关闭后
     *
     * @param session
     * @param status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketSessionHolder.removeSession(session.getId());
    }

    /**
     * 指示处理程序是否支持接收部分消息
     *
     * @return 如果支持接收部分消息，则返回true；否则返回false
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
