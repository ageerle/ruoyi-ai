package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 工具日志事件类
 * 继承自LogEvent，添加工具相关的字段
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolLogEvent extends LogEvent {

    private String toolName;
    private String filePath;
    private String icon;
    private String status; // RUNNING, SUCCESS, ERROR
    private Long executionTime; // 执行时间(毫秒)
    private String summary; // 操作摘要

    // Constructors
    public ToolLogEvent() {
        super();
    }

    public ToolLogEvent(String type, String taskId, String toolName, String filePath,
                        String message, String timestamp, String icon, String status) {
        super(type, taskId, message, timestamp);
        this.toolName = toolName;
        this.filePath = filePath;
        this.icon = icon;
        this.status = status;
    }

    // Getters and Setters
    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public String toString() {
        return "ToolLogEvent{" +
                "toolName='" + toolName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", icon='" + icon + '\'' +
                ", status='" + status + '\'' +
                ", executionTime=" + executionTime +
                ", summary='" + summary + '\'' +
                "} " + super.toString();
    }
}
