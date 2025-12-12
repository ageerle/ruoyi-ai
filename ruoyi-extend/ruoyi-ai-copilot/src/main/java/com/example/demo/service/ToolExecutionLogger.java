package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具执行日志记录服务
 * 记录所有工具调用的详细信息，使用中文日志
 */
@Service
public class ToolExecutionLogger {

    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionLogger.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 工具调用计数器
    private final AtomicLong callCounter = new AtomicLong(0);

    // 工具执行统计
    private final Map<String, ToolStats> toolStats = new ConcurrentHashMap<>();

    /**
     * 记录工具调用开始
     */
    public long logToolStart(String toolName, String description, Object parameters) {
        long callId = callCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(formatter);

        logger.info("🚀 [工具调用-{}] 开始执行工具: {}", callId, toolName);
        logger.info("📝 [工具调用-{}] 工具描述: {}", callId, description);
        logger.info("⚙️  [工具调用-{}] 调用参数: {}", callId, formatParameters(parameters));
        logger.info("🕐 [工具调用-{}] 开始时间: {}", callId, timestamp);

        // 更新统计信息
        toolStats.computeIfAbsent(toolName, k -> new ToolStats()).incrementCalls();

        return callId;
    }

    /**
     * 记录工具调用成功
     */
    public void logToolSuccess(long callId, String toolName, String result, long executionTimeMs) {
        String timestamp = LocalDateTime.now().format(formatter);

        logger.info("✅ [工具调用-{}] 工具执行成功: {}", callId, toolName);
        logger.info("📊 [工具调用-{}] 执行结果: {}", callId, truncateResult(result));
        logger.info("⏱️  [工具调用-{}] 执行耗时: {}ms", callId, executionTimeMs);
        logger.info("🕐 [工具调用-{}] 完成时间: {}", callId, timestamp);

        // 更新统计信息
        ToolStats stats = toolStats.get(toolName);
        if (stats != null) {
            stats.incrementSuccess();
            stats.addExecutionTime(executionTimeMs);
        }
    }

    /**
     * 记录工具调用失败
     */
    public void logToolError(long callId, String toolName, String error, long executionTimeMs) {
        String timestamp = LocalDateTime.now().format(formatter);

        logger.error("❌ [工具调用-{}] 工具执行失败: {}", callId, toolName);
        logger.error("🚨 [工具调用-{}] 错误信息: {}", callId, error);
        logger.error("⏱️  [工具调用-{}] 执行耗时: {}ms", callId, executionTimeMs);
        logger.error("🕐 [工具调用-{}] 失败时间: {}", callId, timestamp);

        // 更新统计信息
        ToolStats stats = toolStats.get(toolName);
        if (stats != null) {
            stats.incrementError();
            stats.addExecutionTime(executionTimeMs);
        }
    }

    /**
     * 记录工具调用的详细步骤
     */
    public void logToolStep(long callId, String toolName, String step, String details) {
        logger.debug("🔄 [工具调用-{}] [{}] 执行步骤: {} - {}", callId, toolName, step, details);
    }

    /**
     * 记录文件操作
     */
    public void logFileOperation(long callId, String operation, String filePath, String details) {
        logger.info("📁 [工具调用-{}] 文件操作: {} - 文件: {} - 详情: {}", callId, operation, filePath, details);
    }

    /**
     * 记录项目分析
     */
    public void logProjectAnalysis(long callId, String projectPath, String projectType, String details) {
        logger.info("🔍 [工具调用-{}] 项目分析: 路径={}, 类型={}, 详情={}", callId, projectPath, projectType, details);
    }

    /**
     * 记录项目创建
     */
    public void logProjectCreation(long callId, String projectName, String projectType, String projectPath) {
        logger.info("🏗️  [工具调用-{}] 项目创建: 名称={}, 类型={}, 路径={}", callId, projectName, projectType, projectPath);
    }

    /**
     * 获取工具执行统计
     */
    public void logToolStatistics() {
        logger.info("📈 ========== 工具执行统计 ==========");
        toolStats.forEach((toolName, stats) -> {
            logger.info("🔧 工具: {} | 调用次数: {} | 成功: {} | 失败: {} | 平均耗时: {}ms",
                    toolName, stats.getTotalCalls(), stats.getSuccessCount(),
                    stats.getErrorCount(), stats.getAverageExecutionTime());
        });
        logger.info("📈 ================================");
    }

    /**
     * 格式化参数显示
     */
    private String formatParameters(Object parameters) {
        if (parameters == null) {
            return "无参数";
        }
        String paramStr = parameters.toString();
        return paramStr.length() > 200 ? paramStr.substring(0, 200) + "..." : paramStr;
    }

    /**
     * 截断结果显示
     */
    private String truncateResult(String result) {
        if (result == null) {
            return "无结果";
        }
        return result.length() > 300 ? result.substring(0, 300) + "..." : result;
    }

    /**
     * 工具统计信息内部类
     */
    private static class ToolStats {
        private long totalCalls = 0;
        private long successCount = 0;
        private long errorCount = 0;
        private long totalExecutionTime = 0;

        public void incrementCalls() {
            totalCalls++;
        }

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementError() {
            errorCount++;
        }

        public void addExecutionTime(long time) {
            totalExecutionTime += time;
        }

        public long getTotalCalls() {
            return totalCalls;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public long getAverageExecutionTime() {
            return totalCalls > 0 ? totalExecutionTime / totalCalls : 0;
        }
    }
}
