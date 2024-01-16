package com.xmzs.common.chat.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.xmzs.common.core.utils.SpringUtils;
import com.xmzs.common.chat.config.LocalCache;
import com.xmzs.common.chat.entity.chat.ChatCompletion;
import com.xmzs.common.chat.holder.WebSocketSessionHolder;
import com.xmzs.common.chat.listener.WebSocketEventListener;
import com.xmzs.common.chat.openai.OpenAiStreamClient;
import com.xmzs.common.chat.entity.chat.Message;
import com.xmzs.common.chat.utils.WebSocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * WebSocketHandler 实现类
 *
 * @author zendwang
 */
@Slf4j
public class PlusWebSocketHandler extends AbstractWebSocketHandler {

    /**
     * 是否开启文本审核
     */
    @Value("${baidu.enabled}")
    private Boolean enabled;

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
//        if(enabled){
//            // 判断文本是否合规
//            TextReviewService textReviewService=(TextReviewService) SpringUtils.context().getBean("textReviewService");
//            String type = textReviewService.textReview(message.getPayload());
//            // 审核状态 1 代表合法
//            String conclusionType = "1";
//            if (!conclusionType.equals(type) && StringUtils.isNotEmpty(type)) {
//                HashMap<Object, Object> msgMap = new HashMap<>(10);
//                msgMap.put("content", "文本不合规,请修改!");
//                String jsonStr = JSONUtil.toJsonStr(msgMap);
//                WebSocketUtils.sendMessage(session, jsonStr);
//                WebSocketUtils.sendMessage(session, "[DONE]");
//                return;
//            }
//        }
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

    /**
     * 根据key获取Value值
     *
     * @Date 2023/7/27
     * @param jsonObject
     * @param key
     * @param defaultValue
     * @return String
     **/
    public String getValue(JSONObject jsonObject,String key,String defaultValue){
        String value = (String)jsonObject.get(key);
        if(StrUtil.isEmpty(value)){
            return defaultValue;
        }
        return value;
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
     * 是否支持分片消息
     *
     * @return
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
