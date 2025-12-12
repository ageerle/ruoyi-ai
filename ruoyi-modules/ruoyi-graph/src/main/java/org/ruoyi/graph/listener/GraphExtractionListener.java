package org.ruoyi.graph.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import org.ruoyi.common.chat.entity.chat.ChatCompletionResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 图谱抽取LLM响应监听器
 * 用于收集完整的LLM响应（非流式）
 *
 * @author ruoyi-ai
 */
@Slf4j
public class GraphExtractionListener extends EventSourceListener {

    private final StringBuilder responseBuilder = new StringBuilder();
    private final CompletableFuture<String> responseFuture;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GraphExtractionListener(CompletableFuture<String> responseFuture) {
        this.responseFuture = responseFuture;
    }

    @Override
    public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
        log.debug("LLM连接已建立");
    }

    @Override
    public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
        try {
            if ("[DONE]".equals(data)) {
                // 响应完成，返回完整内容
                responseFuture.complete(responseBuilder.toString());
                return;
            }

            // 解析响应
            ChatCompletionResponse completionResponse = objectMapper.readValue(data, ChatCompletionResponse.class);
            if (completionResponse != null &&
                    completionResponse.getChoices() != null &&
                    !completionResponse.getChoices().isEmpty()) {

                Object content = completionResponse.getChoices().get(0).getDelta().getContent();
                if (content != null) {
                    responseBuilder.append(content);
                }
            }
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", e.getMessage(), e);
            responseFuture.completeExceptionally(e);
        }
    }

    @Override
    public void onClosed(@NotNull EventSource eventSource) {
        log.debug("LLM连接已关闭");
        // 如果还没有完成，就用当前内容完成
        if (!responseFuture.isDone()) {
            responseFuture.complete(responseBuilder.toString());
        }
    }

    @Override
    public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
        String errorMsg = "LLM调用失败";
        if (response != null && response.body() != null) {
            try {
                errorMsg = response.body().string();
            } catch (Exception e) {
                errorMsg = response.toString();
            }
        }
        log.error("LLM调用失败: {}", errorMsg, t);
        responseFuture.completeExceptionally(
                new RuntimeException(errorMsg, t)
        );
    }
}
