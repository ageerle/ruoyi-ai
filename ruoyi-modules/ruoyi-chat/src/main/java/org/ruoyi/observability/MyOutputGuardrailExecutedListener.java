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

        log.info("【输出Guardrail已执行】调用唯一标识符: {}", invocationId);
        log.info("【输出Guardrail已执行】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【输出Guardrail已执行】调用的方法名: {}", aiServiceMethodName);
        log.info("【输出Guardrail已执行】Guardrail类名: {}", guardrailClass.getName());
        log.info("【输出Guardrail已执行】输出Guardrail请求: {}", request);
        log.info("【输出Guardrail已执行】输出Guardrail结果: {}", result);
        log.info("【输出Guardrail已执行】执行耗时: {}ms", duration.toMillis());
    }
}
