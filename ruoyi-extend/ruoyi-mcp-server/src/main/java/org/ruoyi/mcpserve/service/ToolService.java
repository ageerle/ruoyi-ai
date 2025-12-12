package org.ruoyi.mcpserve.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


/**
 * @author ageer
 */
@Service
public class ToolService {

    @Tool(description = "获取一个指定前缀的随机数")
    public String add(@ToolParam(description = "字符前缀") String prefix) {
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        //根据当前时间获取yyMMdd格式的时间字符串
        String format = LocalDate.now().format(formatter);
        //生成随机数
        String replace = prefix + UUID.randomUUID().toString().replace("-", "");
        return format + replace;
    }

    @Tool(description = "获取当前时间")
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
}
