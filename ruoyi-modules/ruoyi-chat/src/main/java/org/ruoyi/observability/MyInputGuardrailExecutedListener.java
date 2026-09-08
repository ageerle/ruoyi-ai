package org.ruoyi.observability;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.listener.InputGuardrailExecutedListener;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;

/**
 * 自定义的 InputGuardrailExecutedEvent 的监听器。
 * 它表示在输入 guardrail 验证执行时发生的事件。
 *
 * @author evo
 */
@Slf4j
public class MyInputGuardrailExecutedListener implements InputGuardrailExecutedListener {

    @Override
    public void onEvent(InputGuardrailExecutedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        InputGuardrailRequest request = event.request();
        InputGuardrailResult result = event.result();
        Class<InputGuardrail> guardrailClass = event.guardrailClass();
        Duration duration = event.duration();
        UserMessage rewrittenUserMessage = event.rewrittenUserMessage();

        log.info("input_guardrail_executed invocationId={} interfaceType={} method={} status=COMPLETED "
                + "guardrailType={} resultType={} rewrittenMessageCount={} elapsedMs={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName, guardrailClass.getName(),
            result == null ? "none" : result.getClass().getName(),
            rewrittenUserMessage == null ? 0 : 1, duration == null ? -1L : duration.toMillis());
    }
}
