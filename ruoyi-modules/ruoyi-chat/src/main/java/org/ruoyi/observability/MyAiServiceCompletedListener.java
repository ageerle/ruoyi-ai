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

        long elapsedMillis = Math.max(0L, java.time.Duration.between(eventTimestamp, Instant.now()).toMillis());
        String resultType = result.map(value -> value.getClass().getName()).orElse("none");
        log.info("ai_service_completed invocationId={} interfaceType={} method={} status=COMPLETED "
                + "argumentCount={} resultType={} elapsedMs={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName,
            aiServiceMethodArgs == null ? 0 : aiServiceMethodArgs.size(), resultType, elapsedMillis);
    }
}
