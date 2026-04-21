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

        log.info("【AI服务启动】调用唯一标识符: {}", invocationId);
        log.info("【AI服务启动】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【AI服务启动】调用的方法名: {}", aiServiceMethodName);
        log.info("【AI服务启动】系统消息: {}", systemMessage.orElse(null));
        log.info("【AI服务启动】用户消息: {}", userMessage);
    }
}
