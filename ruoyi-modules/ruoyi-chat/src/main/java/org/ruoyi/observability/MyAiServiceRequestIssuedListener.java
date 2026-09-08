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

        int messageCount = request == null || request.messages() == null ? 0 : request.messages().size();
        int toolCount = request == null || request.toolSpecifications() == null
            ? 0 : request.toolSpecifications().size();
        log.info("ai_service_request_issued invocationId={} interfaceType={} method={} status=ISSUED "
                + "messageCount={} toolCount={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName, messageCount, toolCount);
    }
}
