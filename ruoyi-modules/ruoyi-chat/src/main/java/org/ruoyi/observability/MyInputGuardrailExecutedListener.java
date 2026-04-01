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

        log.info("【输入Guardrail已执行】调用唯一标识符: {}", invocationId);
        log.info("【输入Guardrail已执行】AI服务接口名: {}", aiServiceInterfaceName);
        log.info("【输入Guardrail已执行】调用的方法名: {}", aiServiceMethodName);
        log.info("【输入Guardrail已执行】Guardrail类名: {}", guardrailClass.getName());
        log.info("【输入Guardrail已执行】输入Guardrail请求: {}", request);
        log.info("【输入Guardrail已执行】输入Guardrail结果: {}", result);
        log.info("【输入Guardrail已执行】重写后的用户消息: {}", rewrittenUserMessage);
        log.info("【输入Guardrail已执行】执行耗时: {}ms", duration.toMillis());
    }
}
