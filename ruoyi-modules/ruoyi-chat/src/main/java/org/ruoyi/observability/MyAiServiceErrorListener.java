package org.ruoyi.observability;

import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.listener.AiServiceErrorListener;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 自定义的 AiServiceErrorEvent 的监听器。
 * 它表示在 AI 服务调用失败时发生的事件。
 *
 * @author evo
 */
@Slf4j
public class MyAiServiceErrorListener implements AiServiceErrorListener {

    @Override
    public void onEvent(AiServiceErrorEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        Throwable error = event.error();

        log.error("【AI服务错误】调用唯一标识符: {}", invocationId);
        log.error("【AI服务错误】AI服务接口名: {}", aiServiceInterfaceName);
        log.error("【AI服务错误】调用的方法名: {}", aiServiceMethodName);
        log.error("【AI服务错误】错误类型: {}", error.getClass().getName());
        log.error("【AI服务错误】错误信息: {}", error.getMessage(), error);
    }
}
