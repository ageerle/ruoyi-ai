package org.ruoyi.observability;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.listener.AiServiceResponseReceivedListener;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 自定义的 AiServiceResponseReceivedEvent 的监听器。
 * 它表示在从 LLM 接收到响应时发生的事件。
 * 在涉及工具或 guardrail 的单个 AI 服务调用期间，可能会被调用多次。
 *
 * @author evo
 */
@Slf4j
public class MyAiServiceResponseReceivedListener implements AiServiceResponseReceivedListener {

    @Override
    public void onEvent(AiServiceResponseReceivedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        ChatRequest request = event.request();
        ChatResponse response = event.response();

        log.info("【响应已接收】调用唯一标识符: {}", invocationId);
        log.info("【响应已接收】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【响应已接收】调用的方法名: {}", aiServiceMethodName);
        log.info("【响应已接收】发送给LLM的请求: {}", request);
        log.info("【响应已接收】从LLM收到的响应: {}", response);
    }
}
