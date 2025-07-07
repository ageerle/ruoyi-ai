package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDto;
import com.example.demo.service.ContinuousConversationService;
import com.example.demo.service.ToolExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天控制器
 * 处理与AI的对话和工具调用
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private final ContinuousConversationService continuousConversationService;
    private final ToolExecutionLogger executionLogger;

    // 简单的会话存储（生产环境应该使用数据库或Redis）
    private final List<Message> conversationHistory = new ArrayList<>();

    public ChatController(ChatClient chatClient, ContinuousConversationService continuousConversationService, ToolExecutionLogger executionLogger) {
        this.chatClient = chatClient;
        this.continuousConversationService = continuousConversationService;
        this.executionLogger = executionLogger;
    }

    /**
     * 发送消息给AI - 支持连续工具调用
     */
    // 在现有ChatController中修改sendMessage方法
    
    @PostMapping("/message")
    public Mono<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("💬 ========== 新的聊天请求 ==========");
                logger.info("📝 用户消息: {}", request.getMessage());
                logger.info("🕐 请求时间: {}", java.time.LocalDateTime.now());

                // 智能判断是否需要工具调用
                boolean needsToolExecution = continuousConversationService.isLikelyToNeedTools(request.getMessage());
                logger.info("🔍 工具需求分析: {}", needsToolExecution ? "可能需要工具" : "简单对话");

                if (needsToolExecution) {
                    // 需要工具调用的复杂任务 - 使用异步模式
                    String taskId = continuousConversationService.startTask(request.getMessage());
                    logger.info("🆔 任务ID: {}", taskId);

                    // 记录任务开始
                    executionLogger.logToolStatistics(); // 显示当前统计

                    // 异步执行连续对话
                    CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("🚀 开始异步执行连续对话任务: {}", taskId);
                            continuousConversationService.executeContinuousConversation(
                                taskId, request.getMessage(), conversationHistory
                            );
                            logger.info("✅ 连续对话任务完成: {}", taskId);
                        } catch (Exception e) {
                            logger.error("❌ 异步对话执行错误: {}", e.getMessage(), e);
                        }
                    });

                    // 返回异步任务响应
                    ChatResponseDto responseDto = new ChatResponseDto();
                    responseDto.setTaskId(taskId);
                    responseDto.setMessage("任务已启动，正在处理中...");
                    responseDto.setSuccess(true);
                    responseDto.setAsyncTask(true);

                    logger.info("📤 返回响应: taskId={}, 异步任务已启动", taskId);
                    return responseDto;
                } else {
                    // 简单对话 - 使用流式模式
                    logger.info("🔄 执行流式对话处理");

                    // 返回流式响应标识，让前端建立流式连接
                    ChatResponseDto responseDto = new ChatResponseDto();
                    responseDto.setMessage("开始流式对话...");
                    responseDto.setSuccess(true);
                    responseDto.setAsyncTask(false); // 关键：设置为false，表示不是工具任务
                    responseDto.setStreamResponse(true); // 新增：标识为流式响应
                    responseDto.setTotalTurns(1);

                    logger.info("📤 返回流式响应标识");
                    return responseDto;
                }
                
            } catch (Exception e) {
                logger.error("Error processing chat message", e);
                ChatResponseDto errorResponse = new ChatResponseDto();
                errorResponse.setMessage("Error: " + e.getMessage());
                errorResponse.setSuccess(false);
                return errorResponse;
            }
        });
    }
    


    /**
     * 流式聊天 - 真正的流式实现
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestBody ChatRequestDto request) {
        logger.info("🌊 开始流式对话: {}", request.getMessage());

        return Flux.create(sink -> {
            try {
                UserMessage userMessage = new UserMessage(request.getMessage());
                conversationHistory.add(userMessage);

                // 使用Spring AI的流式API
                Flux<String> contentStream = chatClient.prompt()
                    .messages(conversationHistory)
                    .stream()
                    .content();

                // 订阅流式内容并转发给前端
                contentStream
                    .doOnNext(content -> {
                        logger.debug("📨 流式内容片段: {}", content);
                        // 发送SSE格式的数据
                        sink.next("data: " + content + "\n\n");
                    })
                    .doOnComplete(() -> {
                        logger.info("✅ 流式对话完成");
                        sink.next("data: [DONE]\n\n");
                        sink.complete();
                    })
                    .doOnError(error -> {
                        logger.error("❌ 流式对话错误: {}", error.getMessage());
                        sink.error(error);
                    })
                    .subscribe();

            } catch (Exception e) {
                logger.error("❌ 流式对话启动失败: {}", e.getMessage());
                sink.error(e);
            }
        });
    }

    /**
     * 清除对话历史
     */
    @PostMapping("/clear")
    public Mono<Map<String, String>> clearHistory() {
        conversationHistory.clear();
        logger.info("Conversation history cleared");
        return Mono.just(Map.of("status", "success", "message", "Conversation history cleared"));
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Mono<List<MessageDto>> getHistory() {
        List<MessageDto> history = conversationHistory.stream()
            .map(message -> {
                MessageDto dto = new MessageDto();
                dto.setContent(message.getText());
                dto.setRole(message instanceof UserMessage ? "user" : "assistant");
                return dto;
            })
            .toList();

        return Mono.just(history);
    }

    // 注意：Spring AI 1.0.0 使用不同的函数调用方式
    // 函数需要在配置中注册，而不是在运行时动态创建

    public static class ChatResponseDto {
        private String taskId;
        private String message;
        private boolean success;
        private boolean asyncTask;
        private boolean streamResponse; // 新增：标识是否为流式响应
        private int totalTurns;
        private boolean reachedMaxTurns;
        private String stopReason;
        private long totalDurationMs;

      public String getTaskId() {
        return taskId;
      }

      public void setTaskId(String taskId) {
        this.taskId = taskId;
      }

      public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

      public boolean isAsyncTask() {
        return asyncTask;
      }

      public void setAsyncTask(boolean asyncTask) {
        this.asyncTask = asyncTask;
      }

      public boolean isStreamResponse() {
        return streamResponse;
      }

      public void setStreamResponse(boolean streamResponse) {
        this.streamResponse = streamResponse;
      }

      public int getTotalTurns() { return totalTurns; }
        public void setTotalTurns(int totalTurns) { this.totalTurns = totalTurns; }

        public boolean isReachedMaxTurns() { return reachedMaxTurns; }
        public void setReachedMaxTurns(boolean reachedMaxTurns) { this.reachedMaxTurns = reachedMaxTurns; }

        public String getStopReason() { return stopReason; }
        public void setStopReason(String stopReason) { this.stopReason = stopReason; }

        public long getTotalDurationMs() { return totalDurationMs; }
        public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }
    }

    public static class MessageDto {
        private String content;
        private String role;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
