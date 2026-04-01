package org.ruoyi.observability;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.observability.api.event.AiServiceRequestIssuedEvent;
import dev.langchain4j.observability.api.listener.AiServiceRequestIssuedListener;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 自定义的 AiServiceRequestIssuedEvent 的监听器。
 * 它表示在向 LLM 发送请求之前发生的事件。
 *
 * @author evo
 */
@Slf4j
public class MyAiServiceRequestIssuedListener implements AiServiceRequestIssuedListener {

    @Override
    public void onEvent(AiServiceRequestIssuedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        ChatRequest request = event.request();

        log.info("【请求已发出】调用唯一标识符: {}", invocationId);
        log.info("【请求已发出】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【请求已发出】调用的方法名: {}", aiServiceMethodName);
        log.info("【请求已发出】发送给LLM的请求: {}", request);
    }
}
