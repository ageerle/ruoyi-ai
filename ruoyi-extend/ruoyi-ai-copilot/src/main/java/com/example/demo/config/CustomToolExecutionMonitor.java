package com.example.demo.config;

import com.example.demo.service.ToolExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 自定义工具执行监听器
 * 提供中文日志和详细的文件操作信息记录
 * 
 * 注意：Spring AI 1.0.0使用@Tool注解来定义工具，不需要ToolCallbackProvider接口
 * 这个类主要用于工具执行的日志记录和监控
 */
@Component
public class CustomToolExecutionMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomToolExecutionMonitor.class);
    
    @Autowired
    private ToolExecutionLogger executionLogger;
    
    /**
     * 记录工具执行开始
     */
    public long logToolStart(String toolName, String description, String parameters) {
        String fileInfo = extractFileInfo(toolName, parameters);
        long callId = executionLogger.logToolStart(toolName, description, 
            String.format("参数: %s | 文件信息: %s", parameters, fileInfo));
        
        logger.debug("🚀 [Spring AI] 开始执行工具: {} | 文件/目录: {}", toolName, fileInfo);
        return callId;
    }
    
    /**
     * 记录工具执行成功
     */
    public void logToolSuccess(long callId, String toolName, String result, long executionTime, String parameters) {
        String fileInfo = extractFileInfo(toolName, parameters);
        logger.debug("✅ [Spring AI] 工具执行成功: {} | 耗时: {}ms | 文件/目录: {}", 
            toolName, executionTime, fileInfo);
        executionLogger.logToolSuccess(callId, toolName, result, executionTime);
    }
    
    /**
     * 记录工具执行失败
     */
    public void logToolError(long callId, String toolName, String errorMessage, long executionTime, String parameters) {
        String fileInfo = extractFileInfo(toolName, parameters);
        logger.error("❌ [Spring AI] 工具执行失败: {} | 耗时: {}ms | 文件/目录: {} | 错误: {}", 
            toolName, executionTime, fileInfo, errorMessage);
        executionLogger.logToolError(callId, toolName, errorMessage, executionTime);
    }
    
    /**
     * 提取文件信息用于日志记录
     */
    private String extractFileInfo(String toolName, String arguments) {
        try {
            switch (toolName) {
                case "readFile":
                case "read_file":
                    return extractPathFromArgs(arguments, "absolutePath", "filePath");
                case "writeFile":
                case "write_file":
                    return extractPathFromArgs(arguments, "filePath");
                case "editFile":
                case "edit_file":
                    return extractPathFromArgs(arguments, "filePath");
                case "listDirectory":
                    return extractPathFromArgs(arguments, "directoryPath", "path");
                case "analyzeProject":
                case "analyze_project":
                    return extractPathFromArgs(arguments, "projectPath");
                case "scaffoldProject":
                case "scaffold_project":
                    return extractPathFromArgs(arguments, "projectPath");
                case "smartEdit":
                case "smart_edit":
                    return extractPathFromArgs(arguments, "projectPath");
                default:
                    return "未知文件路径";
            }
        } catch (Exception e) {
            return "解析文件路径失败: " + e.getMessage();
        }
    }
    
    /**
     * 从参数中提取路径
     */
    private String extractPathFromArgs(String arguments, String... pathKeys) {
        for (String key : pathKeys) {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(arguments);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "未找到路径参数";
    }
}
