package com.example.demo.controller;

import com.example.demo.model.TaskStatus;
import com.example.demo.service.ContinuousConversationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/task")
@CrossOrigin(origins = "*")
public class TaskStatusController {

    private final ContinuousConversationService conversationService;

    public TaskStatusController(ContinuousConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/status/{taskId}")
    public Mono<TaskStatusDto> getTaskStatus(@PathVariable("taskId") String taskId) {
        return Mono.fromCallable(() -> {
            TaskStatus status = conversationService.getTaskStatus(taskId);
            if (status == null) {
                throw new RuntimeException("Task not found: " + taskId);
            }

            TaskStatusDto dto = new TaskStatusDto();
            dto.setTaskId(status.getTaskId());
            dto.setStatus(status.getStatus());
            dto.setCurrentAction(status.getCurrentAction());
            dto.setSummary(status.getSummary());
            dto.setCurrentTurn(status.getCurrentTurn());
            dto.setTotalEstimatedTurns(status.getTotalEstimatedTurns());
            dto.setProgressPercentage(status.getProgressPercentage());
            dto.setElapsedTime(status.getElapsedTime());
            dto.setErrorMessage(status.getErrorMessage());

            return dto;
        });
    }

    /**
     * 获取对话结果
     */
    @GetMapping("/result/{taskId}")
    public Mono<ConversationResultDto> getConversationResult(@PathVariable("taskId") String taskId) {
        return Mono.fromCallable(() -> {
            ContinuousConversationService.ConversationResult result = conversationService.getConversationResult(taskId);
            if (result == null) {
                throw new RuntimeException("Conversation result not found: " + taskId);
            }

            ConversationResultDto dto = new ConversationResultDto();
            dto.setTaskId(taskId);
            dto.setFullResponse(result.getFullResponse());
            dto.setTurnResponses(result.getTurnResponses());
            dto.setTotalTurns(result.getTotalTurns());
            dto.setReachedMaxTurns(result.isReachedMaxTurns());
            dto.setStopReason(result.getStopReason());
            dto.setTotalDurationMs(result.getTotalDurationMs());

            return dto;
        });
    }

    // DTO类
    public static class TaskStatusDto {
        private String taskId;
        private String status;
        private String currentAction;
        private String summary;
        private int currentTurn;
        private int totalEstimatedTurns;
        private double progressPercentage;
        private long elapsedTime;
        private String errorMessage;

        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCurrentAction() {
            return currentAction;
        }

        public void setCurrentAction(String currentAction) {
            this.currentAction = currentAction;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public int getCurrentTurn() {
            return currentTurn;
        }

        public void setCurrentTurn(int currentTurn) {
            this.currentTurn = currentTurn;
        }

        public int getTotalEstimatedTurns() {
            return totalEstimatedTurns;
        }

        public void setTotalEstimatedTurns(int totalEstimatedTurns) {
            this.totalEstimatedTurns = totalEstimatedTurns;
        }

        public double getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(double progressPercentage) {
            this.progressPercentage = progressPercentage;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

        public void setElapsedTime(long elapsedTime) {
            this.elapsedTime = elapsedTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    // 对话结果DTO类
    public static class ConversationResultDto {
        private String taskId;
        private String fullResponse;
        private java.util.List<String> turnResponses;
        private int totalTurns;
        private boolean reachedMaxTurns;
        private String stopReason;
        private long totalDurationMs;

        // Getters and Setters
        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getFullResponse() {
            return fullResponse;
        }

        public void setFullResponse(String fullResponse) {
            this.fullResponse = fullResponse;
        }

        public java.util.List<String> getTurnResponses() {
            return turnResponses;
        }

        public void setTurnResponses(java.util.List<String> turnResponses) {
            this.turnResponses = turnResponses;
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
}
