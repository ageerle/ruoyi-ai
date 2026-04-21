package org.ruoyi.observability;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.listener.AiServiceCompletedListener;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 自定义的 AiServiceCompletedEvent 的监听器。
 * 它表示在 AI 服务调用完成时发生的事件。
 *
 * @author evo
 */
@Slf4j
public class MyAiServiceCompletedListener implements AiServiceCompletedListener {

    @Override
    public void onEvent(AiServiceCompletedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        Optional<Object> result = event.result();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        List<Object> aiServiceMethodArgs = invocationContext.methodArguments();
        Object chatMemoryId = invocationContext.chatMemoryId();
        Instant eventTimestamp = invocationContext.timestamp();

        log.info("【AI服务完成】调用唯一标识符: {}", invocationId);
        log.info("【AI服务完成】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【AI服务完成】调用的方法名: {}", aiServiceMethodName);
        log.info("【AI服务完成】AI服务方法参数: {}", aiServiceMethodArgs);
        log.info("【AI服务完成】聊天记忆ID: {}", chatMemoryId);
        log.info("【AI服务完成】调用发生的时间: {}", eventTimestamp);
        log.info("【AI服务完成】调用结果: {}", result);
    }
}
