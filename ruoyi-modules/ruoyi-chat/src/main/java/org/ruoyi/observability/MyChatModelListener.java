package org.ruoyi.observability;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义的 ChatModelListener 的监听器。
 * 它监听 ChatModel 的请求、响应和错误事件。
 *
 * @author evo
 */
@Slf4j
public class MyChatModelListener implements ChatModelListener {

    private static final String START_NANOS_ATTRIBUTE =
        MyChatModelListener.class.getName() + ".startNanos";

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ChatRequest request = requestContext.chatRequest();
        requestContext.attributes().put(START_NANOS_ATTRIBUTE, System.nanoTime());
        log.info("【ChatModel请求】提供商[{}] 模型[{}] 消息数[{}] 工具数[{}] 最大输出Token[{}]",
            requestContext.modelProvider(), request.modelName(), messageCount(request),
            toolCount(request), request.maxOutputTokens());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ChatRequest request = responseContext.chatRequest();
        ChatResponse response = responseContext.chatResponse();
        var usage = response.tokenUsage();
        log.info("【ChatModel响应】提供商[{}] 模型[{}] 消息数[{}] 工具数[{}] "
                + "输入Token[{}] 输出Token[{}] 完成原因[{}] 耗时[{}]毫秒",
            responseContext.modelProvider(), response.modelName(), messageCount(request),
            toolCount(request), usage == null ? null : usage.inputTokenCount(),
            usage == null ? null : usage.outputTokenCount(), response.finishReason(),
            elapsedMillis(responseContext.attributes().get(START_NANOS_ATTRIBUTE)));
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        ChatRequest request = errorContext.chatRequest();
        log.error("【ChatModel错误】提供商[{}] 模型[{}] 消息数[{}] 工具数[{}] "
                + "状态[FAILED] 错误类型[{}] 耗时[{}]毫秒",
            errorContext.modelProvider(), request == null ? null : request.modelName(),
            messageCount(request), toolCount(request), errorContext.error().getClass().getName(),
            elapsedMillis(errorContext.attributes().get(START_NANOS_ATTRIBUTE)));
    }

    private int messageCount(ChatRequest request) {
        return request == null || request.messages() == null ? 0 : request.messages().size();
    }

    private int toolCount(ChatRequest request) {
        return request == null || request.toolSpecifications() == null
            ? 0 : request.toolSpecifications().size();
    }

    private long elapsedMillis(Object startNanos) {
        return startNanos instanceof Number start
            ? Math.max(0L, (System.nanoTime() - start.longValue()) / 1_000_000L)
            : -1L;
    }
}
