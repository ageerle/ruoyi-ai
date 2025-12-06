package org.ruoyi.graph.service.llm.impl;

import io.github.imfangs.dify.client.DifyClient;
import io.github.imfangs.dify.client.DifyClientFactory;
import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.model.DifyConfig;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.graph.service.llm.IGraphLLMService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dify 图谱LLM服务实现
 * 支持 Dify 平台的对话模型
 * <p>
 * 注意：Dify 使用流式调用，通过 CompletableFuture 实现同步等待
 *
 * @author ruoyi
 * @date 2025-10-11
 */
@Slf4j
@Service
public class DifyGraphLLMServiceImpl implements IGraphLLMService {

    @Override
    public String extractGraph(String prompt, ChatModelVo chatModel) {
        log.info("Dify模型调用: model={}, apiHost={}, 提示词长度={}",
                chatModel.getModelName(), chatModel.getApiHost(), prompt.length());

        try {
            // 创建 Dify 客户端配置
            DifyConfig config = DifyConfig.builder()
                    .baseUrl(chatModel.getApiHost())
                    .apiKey(chatModel.getApiKey())
                    .connectTimeout(5000)
                    .readTimeout(120000)  // 2分钟超时
                    .writeTimeout(30000)
                    .build();

            DifyClient chatClient = DifyClientFactory.createClient(config);

            // 创建聊天消息（使用流式模式）
            ChatMessage message = ChatMessage.builder()
                    .query(prompt)
                    .user("graph-system")  // 图谱系统用户
                    .responseMode(ResponseMode.STREAMING)  // 流式模式
                    .build();

            // 用于收集完整响应
            StringBuilder fullResponse = new StringBuilder();
            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            // 发送流式消息
            long startTime = System.currentTimeMillis();
            chatClient.sendChatMessageStream(message, new ChatStreamCallback() {
                @Override
                public void onMessage(MessageEvent event) {
                    fullResponse.append(event.getAnswer());
                }

                @Override
                public void onMessageEnd(MessageEndEvent event) {
                    long duration = System.currentTimeMillis() - startTime;
                    String responseText = fullResponse.toString();
                    log.info("Dify模型响应成功: 耗时={}ms, 响应长度={}, messageId={}",
                            duration, responseText.length(), event.getMessageId());
                    responseFuture.complete(responseText);
                }

                @Override
                public void onError(ErrorEvent event) {
                    log.error("Dify模型调用错误: {}", event.getMessage());
                    responseFuture.completeExceptionally(
                            new RuntimeException("Dify调用错误: " + event.getMessage())
                    );
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("Dify模型调用异常: {}", throwable.getMessage(), throwable);
                    responseFuture.completeExceptionally(throwable);
                }
            });

            // 同步等待结果（最多2分钟）
            return responseFuture.get(2, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("Dify模型调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("Dify模型调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCategory() {
        return "dify";  // 对应 ChatModel 表中的 category 字段
    }
}

