package com.example.demo.controller;

import com.example.demo.dto.ChatRequestDto;
import com.example.demo.service.ContinuousConversationService;
import com.example.demo.service.ToolExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * 流式聊天 - 直接返回流式数据
     */
    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestBody ChatRequestDto request) {
        logger.info("📨 开始流式聊天: {}", request.getMessage());

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
                            // 发送内容片段（SSE格式会自动添加 "data: " 前缀）
                            sink.next(content);
                        })
                        .doOnComplete(() -> {
                            logger.info("✅ 流式聊天完成");
                            // 发送完成标记
                            sink.next("[DONE]");
                            sink.complete();
                        })
                        .doOnError(error -> {
                            logger.error("❌ 流式聊天错误: {}", error.getMessage());
                            sink.error(error);
                        })
                        .subscribe();

            } catch (Exception e) {
                logger.error("❌ 流式聊天启动失败: {}", e.getMessage());
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

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

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

        public int getTotalTurns() {
            return totalTurns;
        }

        public void setTotalTurns(int totalTurns) {
            this.totalTurns = totalTurns;
        }

        public boolean isReachedMaxTurns() {
            return reachedMaxTurns;
        }

        public void setReachedMaxTurns(boolean reachedMaxTurns) {
            this.reachedMaxTurns = reachedMaxTurns;
        }

        public String getStopReason() {
            return stopReason;
        }

        public void setStopReason(String stopReason) {
            this.stopReason = stopReason;
        }

        public long getTotalDurationMs() {
            return totalDurationMs;
        }

        public void setTotalDurationMs(long totalDurationMs) {
            this.totalDurationMs = totalDurationMs;
        }
    }

    public static class MessageDto {
        private String content;
        private String role;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
