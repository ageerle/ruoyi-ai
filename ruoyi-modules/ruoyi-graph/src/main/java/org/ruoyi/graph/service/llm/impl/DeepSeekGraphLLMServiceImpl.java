package org.ruoyi.graph.service.llm.impl;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.graph.service.llm.IGraphLLMService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek 图谱LLM服务实现
 * 支持 DeepSeek 系列模型
 * <p>
 * 注意：使用 langchain4j 的 OpenAiStreamingChatModel，通过 CompletableFuture 转换为同步调用
 * 参考 DeepSeekChatImpl 的实现，但改为同步模式
 *
 * @author ruoyi
 * @date 2025-10-13
 */
@Slf4j
@Service
public class DeepSeekGraphLLMServiceImpl implements IGraphLLMService {

    @Override
    public String extractGraph(String prompt, ChatModelVo chatModel) {
        log.info("DeepSeek模型调用: model={}, apiHost={}, 提示词长度={}",
                chatModel.getModelName(), chatModel.getApiHost(), prompt.length());

        try {
            // 使用 langchain4j 的 OpenAiStreamingChatModel（参考 DeepSeekChatImpl）
            StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                    .baseUrl(chatModel.getApiHost())
                    .apiKey(chatModel.getApiKey())
                    .modelName(chatModel.getModelName())
                    .temperature(0.8)
                    .logRequests(false)
                    .logResponses(false)
                    .build();

            // 用于收集完整响应
            StringBuilder fullResponse = new StringBuilder();
            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            // 发送流式消息，但通过 CompletableFuture 转换为同步
            long startTime = System.currentTimeMillis();
            streamingModel.chat(prompt, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    fullResponse.append(partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    long duration = System.currentTimeMillis() - startTime;
                    String responseText = fullResponse.toString();
                    log.info("DeepSeek模型响应成功: 耗时={}ms, 响应长度={}", duration, responseText.length());
                    responseFuture.complete(responseText);
                }

                @Override
                public void onError(Throwable error) {
                    log.error("DeepSeek模型调用错误: {}", error.getMessage());
                    responseFuture.completeExceptionally(error);
                }
            });

            // 同步等待结果（最多2分钟）
            return responseFuture.get(2, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("DeepSeek模型调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("DeepSeek模型调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCategory() {
        return "deepseek";  // 对应 ChatModel 表中的 category 字段
    }
}
