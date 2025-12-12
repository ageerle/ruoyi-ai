package com.example.demo.config;

import com.example.demo.service.LogStreamService;
import com.example.demo.service.ToolExecutionLogger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具调用日志切面
 * 拦截 Spring AI 的工具调用并提供中文日志
 */
@Aspect
@Component
public class ToolCallLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ToolCallLoggingAspect.class);

    @Autowired
    private ToolExecutionLogger executionLogger;

    @Autowired
    private LogStreamService logStreamService;

    /**
     * 拦截使用@Tool注解的方法执行
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object interceptToolAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

        // 详细的参数信息
        String parametersInfo = formatMethodParameters(args);
        String fileInfo = extractFileInfoFromMethodArgs(methodName, args);

        logger.debug("🚀 [Spring AI @Tool] 执行工具: {}.{} | 参数: {} | 文件/目录: {}",
                className, methodName, parametersInfo, fileInfo);

        // 获取当前任务ID (从线程本地变量或其他方式)
        String taskId = getCurrentTaskId();

        // 推送工具开始执行事件
        if (taskId != null) {
            String startMessage = generateStartMessage(methodName, fileInfo);
            logStreamService.pushToolStart(taskId, methodName, fileInfo, startMessage);
        }

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            logger.debug("✅ [Spring AI @Tool] 工具执行成功: {}.{} | 耗时: {}ms | 文件/目录: {} | 参数: {}",
                    className, methodName, executionTime, fileInfo, parametersInfo);

            // 推送工具执行成功事件
            if (taskId != null) {
                String successMessage = generateSuccessMessage(methodName, fileInfo, result, executionTime);
                logStreamService.pushToolSuccess(taskId, methodName, fileInfo, successMessage, executionTime);
            }

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;

            logger.error("❌ [Spring AI @Tool] 工具执行失败: {}.{} | 耗时: {}ms | 文件/目录: {} | 参数: {} | 错误: {}",
                    className, methodName, executionTime, fileInfo, parametersInfo, e.getMessage());

            // 推送工具执行失败事件
            if (taskId != null) {
                String errorMessage = generateErrorMessage(methodName, fileInfo, e.getMessage());
                logStreamService.pushToolError(taskId, methodName, fileInfo, errorMessage, executionTime);
            }

            throw e;
        }
    }

    /**
     * 格式化方法参数为可读字符串
     */
    private String formatMethodParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                String str = (String) arg;
                // 如果字符串太长，截断显示
                if (str.length() > 100) {
                    sb.append("\"").append(str.substring(0, 100)).append("...\"");
                } else {
                    sb.append("\"").append(str).append("\"");
                }
            } else {
                sb.append(arg.toString());
            }
        }
        return sb.toString();
    }

    /**
     * 从方法参数中直接提取文件信息
     */
    private String extractFileInfoFromMethodArgs(String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }

        try {
            switch (methodName) {
                case "readFile":
                    // readFile(String absolutePath, Integer offset, Integer limit)
                    return args.length > 0 && args[0] != null ? args[0].toString() : "未指定路径";

                case "writeFile":
                    // writeFile(String filePath, String content)
                    return args.length > 0 && args[0] != null ? args[0].toString() : "未指定路径";

                case "editFile":
                    // editFile(String filePath, String oldText, String newText)
                    return args.length > 0 && args[0] != null ? args[0].toString() : "未指定路径";

                case "listDirectory":
                    // listDirectory(String directoryPath, Boolean recursive)
                    return args.length > 0 && args[0] != null ? args[0].toString() : "未指定路径";

                case "analyzeProject":
                    // analyzeProject(String projectPath, ...)
                    return args.length > 0 && args[0] != null ? args[0].toString() : "未指定路径";

                case "scaffoldProject":
                    // scaffoldProject(String projectName, String projectType, String projectPath, ...)
                    return args.length > 2 && args[2] != null ? args[2].toString() : "未指定路径";

                case "smartEdit":
                    // smartEdit(String projectPath, ...)
                    return args.length > 0 && args[0] != null ? args[0].toString() : "未指定路径";

                default:
                    // 对于未知方法，尝试从第一个参数中提取路径
                    if (args.length > 0 && args[0] != null) {
                        String firstArg = args[0].toString();
                        if (firstArg.contains("/") || firstArg.contains("\\")) {
                            return firstArg;
                        }
                    }
                    return "未识别的工具类型";
            }
        } catch (Exception e) {
            return "解析参数失败: " + e.getMessage();
        }
    }

    /**
     * 从参数字符串中提取文件信息（备用方法）
     */
    private String extractFileInfoFromArgs(String toolName, String arguments) {
        try {
            switch (toolName) {
                case "readFile":
                case "read_file":
                    return extractPathFromString(arguments, "absolutePath", "filePath");
                case "writeFile":
                case "write_file":
                case "editFile":
                case "edit_file":
                    return extractPathFromString(arguments, "filePath");
                case "listDirectory":
                    return extractPathFromString(arguments, "directoryPath", "path");
                case "analyzeProject":
                case "analyze_project":
                case "scaffoldProject":
                case "scaffold_project":
                case "smartEdit":
                case "smart_edit":
                    return extractPathFromString(arguments, "projectPath");
                default:
                    return "未指定文件路径";
            }
        } catch (Exception e) {
            return "解析文件路径失败";
        }
    }

    /**
     * 从字符串中提取路径
     */
    private String extractPathFromString(String text, String... pathKeys) {
        for (String key : pathKeys) {
            // JSON 格式
            Pattern jsonPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher jsonMatcher = jsonPattern.matcher(text);
            if (jsonMatcher.find()) {
                return jsonMatcher.group(1);
            }

            // 键值对格式
            Pattern kvPattern = Pattern.compile(key + "=([^,\\s\\]]+)");
            Matcher kvMatcher = kvPattern.matcher(text);
            if (kvMatcher.find()) {
                return kvMatcher.group(1);
            }
        }

        return "未找到路径";
    }

    /**
     * 获取当前任务ID
     * 从线程本地变量或请求上下文中获取
     */
    private String getCurrentTaskId() {
        // 这里需要从某个地方获取当前任务ID
        // 可以从ThreadLocal、RequestAttributes或其他方式获取
        try {
            // 临时实现：从线程名或其他方式获取
            return TaskContextHolder.getCurrentTaskId();
        } catch (Exception e) {
            logger.debug("无法获取当前任务ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成工具开始执行消息
     */
    private String generateStartMessage(String toolName, String fileInfo) {
        switch (toolName) {
            case "readFile":
                return "正在读取文件: " + getFileName(fileInfo);
            case "writeFile":
                return "正在写入文件: " + getFileName(fileInfo);
            case "editFile":
                return "正在编辑文件: " + getFileName(fileInfo);
            case "listDirectory":
                return "正在列出目录: " + fileInfo;
            case "analyzeProject":
                return "正在分析项目: " + fileInfo;
            case "scaffoldProject":
                return "正在创建项目脚手架: " + fileInfo;
            case "smartEdit":
                return "正在智能编辑项目: " + fileInfo;
            default:
                return "正在执行工具: " + toolName;
        }
    }

    /**
     * 生成工具执行成功消息
     */
    private String generateSuccessMessage(String toolName, String fileInfo, Object result, long executionTime) {
        String fileName = getFileName(fileInfo);
        switch (toolName) {
            case "readFile":
                return String.format("已读取文件 %s (耗时 %dms)", fileName, executionTime);
            case "writeFile":
                return String.format("已写入文件 %s (耗时 %dms)", fileName, executionTime);
            case "editFile":
                return String.format("已编辑文件 %s (耗时 %dms)", fileName, executionTime);
            case "listDirectory":
                return String.format("已列出目录 %s (耗时 %dms)", fileInfo, executionTime);
            case "analyzeProject":
                return String.format("已分析项目 %s (耗时 %dms)", fileInfo, executionTime);
            case "scaffoldProject":
                return String.format("已创建项目脚手架 %s (耗时 %dms)", fileInfo, executionTime);
            case "smartEdit":
                return String.format("已智能编辑项目 %s (耗时 %dms)", fileInfo, executionTime);
            default:
                return String.format("工具 %s 执行成功 (耗时 %dms)", toolName, executionTime);
        }
    }

    /**
     * 生成工具执行失败消息
     */
    private String generateErrorMessage(String toolName, String fileInfo, String errorMsg) {
        String fileName = getFileName(fileInfo);
        return String.format("工具 %s 执行失败: %s (文件: %s)", toolName, errorMsg, fileName);
    }

    /**
     * 从文件路径中提取文件名
     */
    private String getFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "未知文件";
        }

        // 处理Windows和Unix路径
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            return filePath.substring(lastSlash + 1);
        }

        return filePath;
    }
}
