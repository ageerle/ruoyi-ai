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

        int messageCount = request == null || request.messages() == null ? 0 : request.messages().size();
        int toolCount = request == null || request.toolSpecifications() == null
            ? 0 : request.toolSpecifications().size();
        var usage = response == null ? null : response.tokenUsage();
        log.info("ai_service_response_received invocationId={} interfaceType={} method={} status=RECEIVED "
                + "messageCount={} toolCount={} inputTokens={} outputTokens={} finishReason={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName, messageCount, toolCount,
            usage == null ? null : usage.inputTokenCount(),
            usage == null ? null : usage.outputTokenCount(),
            response == null ? null : response.finishReason());
    }
}
