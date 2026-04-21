package org.ruoyi.observability;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义的 ChatModelListener 的监听器。
 * 它监听 ChatModel 的请求、响应和错误事件。
 *
 * @author evo
 */
@Slf4j
public class MyChatModelListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ChatRequest request = requestContext.chatRequest();
        log.info("【ChatModel请求】发送给模型的请求: {}", request);
        log.info("【ChatModel请求】模型提供商: {}", requestContext.modelProvider());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ChatRequest request = responseContext.chatRequest();
        ChatResponse response = responseContext.chatResponse();
        log.info("【ChatModel响应】原始请求: {}", request);
        log.info("【ChatModel响应】收到的响应: {}", response);
        log.info("【ChatModel响应】模型提供商: {}", responseContext.modelProvider());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.error("【ChatModel错误】错误类型: {}", errorContext.error().getClass().getName());
        log.error("【ChatModel错误】错误信息: {}", errorContext.error().getMessage());
        log.error("【ChatModel错误】原始请求: {}", errorContext.chatRequest());
        log.error("【ChatModel错误】模型提供商: {}", errorContext.modelProvider());
    }
}
