package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE日志推送服务
 * 负责将AOP日志实时推送到前端
 */
@Service
public class LogStreamService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogStreamService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 活跃的SSE连接 taskId -> SseEmitter
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    
    // JSON序列化器
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 建立SSE连接
     */
    public SseEmitter createConnection(String taskId) {
        logger.info("🔗 建立SSE连接: taskId={}", taskId);
        
        SseEmitter emitter = new SseEmitter(0L); // 无超时
        
        // 设置连接事件处理
        emitter.onCompletion(() -> {
            logger.info("✅ SSE连接完成: taskId={}", taskId);
            activeConnections.remove(taskId);
        });
        
        emitter.onTimeout(() -> {
            logger.warn("⏰ SSE连接超时: taskId={}", taskId);
            activeConnections.remove(taskId);
        });
        
        emitter.onError((ex) -> {
            logger.error("❌ SSE连接错误: taskId={}, error={}", taskId, ex.getMessage());
            activeConnections.remove(taskId);
        });
        
        // 保存连接
        activeConnections.put(taskId, emitter);
        
        // 发送连接成功消息
        sendLogEvent(taskId, LogEvent.createConnectionEvent(taskId));
        
        return emitter;
    }
    
    /**
     * 关闭SSE连接
     */
    public void closeConnection(String taskId) {
        SseEmitter emitter = activeConnections.remove(taskId);
        if (emitter != null) {
            try {
                emitter.complete();
                logger.info("🔚 关闭SSE连接: taskId={}", taskId);
            } catch (Exception e) {
                logger.error("关闭SSE连接失败: taskId={}, error={}", taskId, e.getMessage());
            }
        }
    }
    
    /**
     * 推送工具开始执行事件
     */
    public void pushToolStart(String taskId, String toolName, String filePath, String message) {
        ToolLogEvent event = new ToolLogEvent();
        event.setType("TOOL_START");
        event.setTaskId(taskId);
        event.setToolName(toolName);
        event.setFilePath(filePath);
        event.setMessage(message);
        event.setTimestamp(LocalDateTime.now().format(formatter));
        event.setIcon(getToolIcon(toolName));
        event.setStatus("RUNNING");
        
        sendLogEvent(taskId, event);
    }
    
    /**
     * 推送工具执行成功事件
     */
    public void pushToolSuccess(String taskId, String toolName, String filePath, String message, long executionTime) {
        ToolLogEvent event = new ToolLogEvent();
        event.setType("TOOL_SUCCESS");
        event.setTaskId(taskId);
        event.setToolName(toolName);
        event.setFilePath(filePath);
        event.setMessage(message);
        event.setTimestamp(LocalDateTime.now().format(formatter));
        event.setIcon(getToolIcon(toolName));
        event.setStatus("SUCCESS");
        event.setExecutionTime(executionTime);
        
        sendLogEvent(taskId, event);
    }
    
    /**
     * 推送工具执行失败事件
     */
    public void pushToolError(String taskId, String toolName, String filePath, String message, long executionTime) {
        ToolLogEvent event = new ToolLogEvent();
        event.setType("TOOL_ERROR");
        event.setTaskId(taskId);
        event.setToolName(toolName);
        event.setFilePath(filePath);
        event.setMessage(message);
        event.setTimestamp(LocalDateTime.now().format(formatter));
        event.setIcon("❌");
        event.setStatus("ERROR");
        event.setExecutionTime(executionTime);
        
        sendLogEvent(taskId, event);
    }
    
    /**
     * 推送任务完成事件
     */
    public void pushTaskComplete(String taskId) {
        LogEvent event = new LogEvent();
        event.setType("TASK_COMPLETE");
        event.setTaskId(taskId);
        event.setMessage("任务执行完成");
        event.setTimestamp(LocalDateTime.now().format(formatter));
        
        sendLogEvent(taskId, event);
        
        // 延迟关闭连接
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒让前端处理完成事件
                closeConnection(taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * 发送日志事件到前端
     */
    private void sendLogEvent(String taskId, Object event) {
        SseEmitter emitter = activeConnections.get(taskId);
        if (emitter != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(event);
                logger.info("📤 准备推送日志事件: taskId={}, type={}, data={}", taskId,
                    event instanceof LogEvent ? ((LogEvent) event).getType() : "unknown", jsonData);

                emitter.send(SseEmitter.event()
                    .name("log")
                    .data(jsonData));

                logger.info("✅ 日志事件推送成功: taskId={}", taskId);
            } catch (IOException e) {
                logger.error("推送日志事件失败: taskId={}, error={}", taskId, e.getMessage());
                activeConnections.remove(taskId);
            }
        } else {
            logger.warn("⚠️ 未找到SSE连接: taskId={}, 无法推送事件", taskId);
        }
    }
    
    /**
     * 获取工具图标
     */
    private String getToolIcon(String toolName) {
        switch (toolName) {
            case "readFile": return "📖";
            case "writeFile": return "✏️";
            case "editFile": return "📝";
            case "listDirectory": return "📁";
            case "analyzeProject": return "🔍";
            case "scaffoldProject": return "🏗️";
            case "smartEdit": return "🧠";
            default: return "⚙️";
        }
    }
    
    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
