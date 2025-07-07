package com.example.demo.tools;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 工具执行结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult {
    
    private final boolean success;
    private final String llmContent;
    private final Object returnDisplay;
    private final String errorMessage;

    private ToolResult(boolean success, String llmContent, Object returnDisplay, String errorMessage) {
        this.success = success;
        this.llmContent = llmContent;
        this.returnDisplay = returnDisplay;
        this.errorMessage = errorMessage;
    }

    // 静态工厂方法
    public static ToolResult success(String llmContent) {
        return new ToolResult(true, llmContent, llmContent, null);
    }

    public static ToolResult success(String llmContent, Object returnDisplay) {
        return new ToolResult(true, llmContent, returnDisplay, null);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, "Error: " + errorMessage, "Error: " + errorMessage, errorMessage);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getLlmContent() { return llmContent; }
    public Object getReturnDisplay() { return returnDisplay; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        if (success) {
            return "ToolResult{success=true, content='" + llmContent + "'}";
        } else {
            return "ToolResult{success=false, error='" + errorMessage + "'}";
        }
    }
}

/**
 * 文件差异结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class FileDiff {
    private final String fileDiff;
    private final String fileName;

    public FileDiff(String fileDiff, String fileName) {
        this.fileDiff = fileDiff;
        this.fileName = fileName;
    }

    public String getFileDiff() { return fileDiff; }
    public String getFileName() { return fileName; }

    @Override
    public String toString() {
        return "FileDiff{fileName='" + fileName + "'}";
    }
}

/**
 * 工具确认详情
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ToolConfirmationDetails {
    private final String type;
    private final String title;
    private final String description;
    private final Object details;

    public ToolConfirmationDetails(String type, String title, String description, Object details) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.details = details;
    }

    public static ToolConfirmationDetails edit(String title, String fileName, String fileDiff) {
        return new ToolConfirmationDetails("edit", title, "File edit confirmation", 
            new FileDiff(fileDiff, fileName));
    }

    public static ToolConfirmationDetails exec(String title, String command) {
        return new ToolConfirmationDetails("exec", title, "Command execution confirmation", command);
    }

    // Getters
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Object getDetails() { return details; }
}
