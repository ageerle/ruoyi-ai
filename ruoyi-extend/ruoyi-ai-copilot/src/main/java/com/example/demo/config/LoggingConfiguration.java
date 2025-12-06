package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志配置类
 * 确保日志目录存在并记录应用启动信息
 */
@Configuration
public class LoggingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 确保日志目录存在
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            if (created) {
                logger.info("📁 创建日志目录: {}", logsDir.getAbsolutePath());
            }
        }

        // 记录应用启动信息
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info("🎉 ========================================");
        logger.info("🚀 (♥◠‿◠)ﾉﾞ  AI Copilot启动成功   ლ(´ڡ`ლ)ﾞ");
        logger.info("🕐 启动时间: {}", startTime);
        logger.info("📝 日志级别: DEBUG (工具调用详细日志已启用)");
        logger.info("📁 日志文件: logs/copilot-file-ops.log");
        logger.info("🔧 支持的工具:");
        logger.info("   📖 read_file - 读取文件内容");
        logger.info("   ✏️  write_file - 写入文件内容");
        logger.info("   📝 edit_file - 编辑文件内容");
        logger.info("   🔍 analyze_project - 分析项目结构");
        logger.info("   🏗️  scaffold_project - 创建项目脚手架");
        logger.info("   🧠 smart_edit - 智能编辑项目");
        logger.info("🎉 ========================================");
    }
}
