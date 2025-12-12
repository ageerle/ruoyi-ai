package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 日志事件基类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEvent {

    private String type;
    private String taskId;
    private String message;
    private String timestamp;

    // Constructors
    public LogEvent() {
    }

    public LogEvent(String type, String taskId, String message, String timestamp) {
        this.type = type;
        this.taskId = taskId;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Static factory methods
    public static LogEvent createConnectionEvent(String taskId) {
        LogEvent event = new LogEvent();
        event.setType("CONNECTION_ESTABLISHED");
        event.setTaskId(taskId);
        event.setMessage("SSE连接已建立");
        event.setTimestamp(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return event;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "LogEvent{" +
                "type='" + type + '\'' +
                ", taskId='" + taskId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
