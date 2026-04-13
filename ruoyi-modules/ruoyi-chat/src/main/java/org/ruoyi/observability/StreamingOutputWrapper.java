package org.ruoyi.observability;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialThinkingContext;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.PartialToolCallContext;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.ModelProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 包装 StreamingChatModel，同时实现 ChatModel 接口。
 *
 * 当 AI Service 方法返回 String 时，LangChain4j 使用 ChatModel.chat()
 * 当返回 TokenStream 时，使用 StreamingChatModel.chat()
 *
 * 此包装器同时实现两个接口，将同步调用转换为流式调用并收集结果，
 * 同时拦截每个 token 推送到 OutputChannel。
 *
 * @author ageerle@163.com
 * @date 2025/04/10
 */
@Slf4j
public class StreamingOutputWrapper implements StreamingChatModel, ChatModel {

    private final StreamingChatModel streamingDelegate;
    private final OutputChannel channel;

    /**
     * 包装 StreamingChatModel
     */
    public StreamingOutputWrapper(StreamingChatModel delegate, OutputChannel channel) {
        this.streamingDelegate = delegate;
        this.channel = channel;
    }

    // ==================== 解决接口默认方法冲突 ====================

    @Override
    public Set<Capability> supportedCapabilities() {
        return streamingDelegate.supportedCapabilities();
    }

    // ==================== ChatModel 接口实现（同步调用） ====================

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("【StreamingOutputWrapper】chat() 被调用，开始流式处理");
        // 用于收集完整响应
        AtomicReference<ChatResponse> responseRef = new AtomicReference<>();
        CompletableFuture<Void> future = new CompletableFuture<>();

        // 调用流式模型，拦截每个 token
        streamingDelegate.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                // 推送到 channel
                channel.send(token);
                log.debug("【流式Token】{}", token);
            }

            @Override
            public void onPartialResponse(PartialResponse pr, PartialResponseContext ctx) {
                channel.send(pr.text());
                log.debug("【流式PartialResponse】{}", pr.text());
            }

            @Override
            public void onPartialThinking(PartialThinking thinking) {
                channel.send("[思考] " + thinking.text());
                log.debug("【流式思考】{}", thinking.text());
            }

            @Override
            public void onPartialThinking(PartialThinking thinking, PartialThinkingContext ctx) {
                channel.send("[思考] " + thinking.text());
            }

            @Override
            public void onPartialToolCall(PartialToolCall toolCall) {
               // channel.send("[工具参数生成中] " + toolCall);
            }

            @Override
            public void onPartialToolCall(PartialToolCall toolCall, PartialToolCallContext ctx) {
             //   channel.send("[工具参数生成中] " + toolCall);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                responseRef.set(response);
                if (response.metadata() != null && response.metadata().tokenUsage() != null) {
                    var usage = response.metadata().tokenUsage();
//                    channel.send("\n[Token统计] input=" + usage.inputTokenCount()
//                        + " output=" + usage.outputTokenCount());
                }
                log.info("【StreamingOutputWrapper】流式处理完成");
                future.complete(null);
            }

            @Override
            public void onError(Throwable error) {
                channel.send("\n[错误] " + error.getMessage());
                channel.completeWithError(error);
                future.completeExceptionally(error);
                log.error("【StreamingOutputWrapper】流式处理出错", error);
            }
        });

        // 等待流式完成
        future.join();

        // 返回收集的响应
        return responseRef.get();
    }

    // ==================== StreamingChatModel 接口实现（流式调用） ====================

    @Override
    public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
        StreamingChatResponseHandler wrapped = wrapHandler(handler);
        streamingDelegate.chat(request, wrapped);
    }

    private StreamingChatResponseHandler wrapHandler(StreamingChatResponseHandler original) {
        return new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String token) {
                channel.send(token);
                original.onPartialResponse(token);
            }

            @Override
            public void onPartialResponse(PartialResponse pr, PartialResponseContext ctx) {
                channel.send(pr.text());
                original.onPartialResponse(pr, ctx);
            }

            @Override
            public void onPartialThinking(PartialThinking thinking) {
                channel.send("[思考] " + thinking.text());
                original.onPartialThinking(thinking);
            }

            @Override
            public void onPartialThinking(PartialThinking thinking, PartialThinkingContext ctx) {
                channel.send("[思考] " + thinking.text());
                original.onPartialThinking(thinking, ctx);
            }

            @Override
            public void onPartialToolCall(PartialToolCall toolCall) {
                //channel.send("[工具参数生成中] " + toolCall);
                original.onPartialToolCall(toolCall);
            }

            @Override
            public void onPartialToolCall(PartialToolCall toolCall, PartialToolCallContext ctx) {
                //channel.send("[工具参数生成中] " + toolCall);
                original.onPartialToolCall(toolCall, ctx);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                if (response.metadata() != null && response.metadata().tokenUsage() != null) {
                    var usage = response.metadata().tokenUsage();
//                    channel.send("\n[Token统计] input=" + usage.inputTokenCount()
//                        + " output=" + usage.outputTokenCount());
                }
                original.onCompleteResponse(response);
            }

            @Override
            public void onError(Throwable error) {
                channel.send("\n[错误] " + error.getMessage());
                channel.completeWithError(error);
                original.onError(error);
            }
        };
    }

    // ==================== 共用接口方法 ====================

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return streamingDelegate.defaultRequestParameters();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return streamingDelegate.listeners();
    }

    @Override
    public ModelProvider provider() {
        return streamingDelegate.provider();
    }
}
