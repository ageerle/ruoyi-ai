package org.ruoyi.mcpserve.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 基础工具类
 *
 * @author OpenX
 */
@Component
public class BasicTools implements McpTool {

    public static final String TOOL_NAME = "basic";

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "获取一个指定前缀的随机数")
    public String add(@ToolParam(description = "字符前缀") String prefix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String format = LocalDate.now().format(formatter);
        String replace = prefix + UUID.randomUUID().toString().replace("-", "");
        return format + replace;
    }

    @Tool(description = "获取当前时间")
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
}
