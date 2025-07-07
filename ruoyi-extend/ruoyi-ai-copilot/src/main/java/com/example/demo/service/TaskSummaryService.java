package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class TaskSummaryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskSummaryService.class);

    private static final Pattern[] ACTION_PATTERNS = {
        Pattern.compile("(?i)creating?\\s+(?:a\\s+)?(?:new\\s+)?(.{1,50}?)(?:\\s+file|\\s+directory|\\s+project)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)writing?\\s+(?:to\\s+)?(.{1,50}?)(?:\\s+file)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)reading?\\s+(?:from\\s+)?(.{1,50}?)(?:\\s+file)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)editing?\\s+(.{1,50}?)(?:\\s+file)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)listing?\\s+(?:the\\s+)?(.{1,50}?)(?:\\s+directory)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)analyzing?\\s+(.{1,50}?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)generating?\\s+(.{1,50}?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)building?\\s+(.{1,50}?)", Pattern.CASE_INSENSITIVE)
    };
    
    private static final String[] ACTION_VERBS = {
        "创建", "写入", "读取", "编辑", "列出", "分析", "生成", "构建",
        "creating", "writing", "reading", "editing", "listing", "analyzing", "generating", "building"
    };
    
    /**
     * 从AI响应中提取任务摘要
     */
    public String extractTaskSummary(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return "处理中...";
        }
        
        // 清理响应文本
        String cleanResponse = aiResponse.replaceAll("```[\\s\\S]*?```", "").trim();
        
        // 尝试匹配具体操作
        for (Pattern pattern : ACTION_PATTERNS) {
            Matcher matcher = pattern.matcher(cleanResponse);
            if (matcher.find()) {
                String action = matcher.group(0).trim();
                if (action.length() > 50) {
                    action = action.substring(0, 47) + "...";
                }
                return action;
            }
        }
        
        // 查找动作词汇
        String lowerResponse = cleanResponse.toLowerCase();
        for (String verb : ACTION_VERBS) {
            if (lowerResponse.contains(verb.toLowerCase())) {
                // 提取包含动作词的句子
                String[] sentences = cleanResponse.split("[.!?\\n]");
                for (String sentence : sentences) {
                    if (sentence.toLowerCase().contains(verb.toLowerCase())) {
                        String summary = sentence.trim();
                        if (summary.length() > 60) {
                            summary = summary.substring(0, 57) + "...";
                        }
                        return summary;
                    }
                }
            }
        }
        
        // 如果没有找到具体操作，返回通用描述
        if (cleanResponse.length() > 60) {
            return cleanResponse.substring(0, 57) + "...";
        }
        
        return cleanResponse.isEmpty() ? "处理中..." : cleanResponse;
    }
    
    /**
     * 估算任务复杂度和预期轮数
     */
    public int estimateTaskComplexity(String initialMessage) {
        if (initialMessage == null) return 1;
        
        String lowerMessage = initialMessage.toLowerCase();
        int complexity = 1;
        
        // 基于关键词估算复杂度
        if (lowerMessage.contains("project") || lowerMessage.contains("项目")) complexity += 3;
        if (lowerMessage.contains("complete") || lowerMessage.contains("完整")) complexity += 2;
        if (lowerMessage.contains("multiple") || lowerMessage.contains("多个")) complexity += 2;
        if (lowerMessage.contains("full-stack") || lowerMessage.contains("全栈")) complexity += 4;
        if (lowerMessage.contains("website") || lowerMessage.contains("网站")) complexity += 2;
        if (lowerMessage.contains("api") || lowerMessage.contains("接口")) complexity += 2;
        
        // 基于文件操作数量估算
        long fileOperations = lowerMessage.chars()
            .mapToObj(c -> String.valueOf((char) c))
            .filter(s -> s.matches(".*(?:create|write|edit|file|directory).*"))
            .count();
        
        complexity += (int) Math.min(fileOperations / 2, 5);
        
        return Math.min(complexity, 15); // 最大15轮
    }
    
    /**
     * 生成当前状态的用户友好描述
     */
    public String generateStatusDescription(String status, String currentAction, int currentTurn, int totalTurns) {
        StringBuilder desc = new StringBuilder();
        
        switch (status) {
            case "RUNNING":
                if (currentAction != null && !currentAction.trim().isEmpty()) {
                    desc.append("🔄 ").append(currentAction);
                } else {
                    desc.append("🤔 AI正在思考...");
                }
                
                if (totalTurns > 1) {
                    desc.append(String.format(" (第%d/%d轮)", currentTurn, totalTurns));
                }
                break;
                
            case "COMPLETED":
                desc.append("✅ 任务完成");
                if (totalTurns > 1) {
                    desc.append(String.format(" (共%d轮)", currentTurn));
                }
                break;
                
            case "ERROR":
                desc.append("❌ 执行出错");
                break;
                
            default:
                desc.append("⏳ 处理中...");
        }
        
        return desc.toString();
    }
}