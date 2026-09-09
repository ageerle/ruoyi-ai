package org.ruoyi.observability;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.listener.AiServiceStartedListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

/**
 * 自定义的 AiServiceStartedEvent 的监听器。
 * 它表示在 AI 服务调用开始时发生的事件。
 *
 * @author evo
 */
@Slf4j
public class MyAiServiceStartedListener implements AiServiceStartedListener {

    @Override
    public void onEvent(AiServiceStartedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        Optional<SystemMessage> systemMessage = event.systemMessage();
        UserMessage userMessage = event.userMessage();

        int messageCount = (systemMessage.isPresent() ? 1 : 0) + (userMessage == null ? 0 : 1);
        log.info("ai_service_started invocationId={} interfaceType={} method={} status=STARTED messageCount={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName, messageCount);
    }
}
