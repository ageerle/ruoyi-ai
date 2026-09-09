package org.ruoyi.observability;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.ToolExecutedEventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 自定义的 ToolExecutedEvent 的监听器。
 * 它表示在工具执行完成后发生的事件。
 * 在单个 AI 服务调用期间，可能会被调用多次。
 *
 * @author evo
 */
@Slf4j
public class MyToolExecutedEventListener implements ToolExecutedEventListener {

    @Override
    public void onEvent(ToolExecutedEvent event) {
        InvocationContext invocationContext = event.invocationContext();
        UUID invocationId = invocationContext.invocationId();
        String aiServiceInterfaceName = invocationContext.interfaceName();
        String aiServiceMethodName = invocationContext.methodName();
        ToolExecutionRequest request = event.request();
        String resultText = event.resultText();

        log.info("tool_executed invocationId={} interfaceType={} method={} status=COMPLETED "
                + "toolType={} argumentChars={} resultChars={}",
            invocationId, aiServiceInterfaceName, aiServiceMethodName,
            request == null ? "unknown" : request.name(),
            request == null || request.arguments() == null ? 0 : request.arguments().length(),
            resultText == null ? 0 : resultText.length());
    }
}
