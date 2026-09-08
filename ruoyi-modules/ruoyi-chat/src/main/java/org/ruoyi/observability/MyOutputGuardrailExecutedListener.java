package org.ruoyi.observability;

import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.listener.OutputGuardrailExecutedListener;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;

/**
 * 自定义的 OutputGuardrailExecutedEvent 的监听器。
 * 它表示在输出 guardrail 验证执行时发生的事件。
 *
 * @author evo
 */
@Slf4j
public class MyOutputGuardrailExecutedListener implements OutputGuardrailExecutedListener {

    @Override
    public void onEvent(OutputGuardrailExecutedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        OutputGuardrailRequest request = event.request();
        OutputGuardrailResult result = event.result();
        Class<OutputGuardrail> guardrailClass = event.guardrailClass();
        Duration duration = event.duration();

        log.info("output_guardrail_executed invocationId={} interfaceType={} method={} status=COMPLETED "
                + "guardrailType={} resultType={} elapsedMs={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName, guardrailClass.getName(),
            result == null ? "none" : result.getClass().getName(),
            duration == null ? -1L : duration.toMillis());
    }
}
