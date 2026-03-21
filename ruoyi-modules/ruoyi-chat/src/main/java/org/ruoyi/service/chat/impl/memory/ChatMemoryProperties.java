package org.ruoyi.service.chat.impl.memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天长期记忆配置属性
 * 支持通过 application.yml 配置长期记忆行为
 *
 * @author ageerle@163.com
 * @date 2026/01/10
 */
@Data
@Component
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

    /**
     * 是否启用长期记忆功能（默认启用）
     */
    private Boolean enabled = true;

    /**
     * 消息窗口大小 - 最多保留的消息条数（默认20）
     * 用于控制每次聊天请求中包含的历史消息数量
     */
    private Integer maxMessages = 20;

    /**
     * 是否启用消息持久化（默认启用）
     * 关闭后消息仅保存在内存中，重启后丢失
     */
    private Boolean persistenceEnabled = true;

    /**
     * 自动清理过期消息的时间间隔（天数，默认不清理）
     * 设为 0 表示禁用自动清理
     */
    private Integer autoCleanupDays = 0;

    /**
     * 消息摘要是否启用（默认禁用）
     * 启用后，超过消息窗口的旧消息会被摘要处理
     */
    private Boolean summarizeEnabled = false;

    /**
     * 摘要缓冲区大小 - 触发摘要的消息数量阈值（默认50）
     */
    private Integer summarizeThreshold = 50;

    /**
     * 是否在日志中记录内存加载情况（默认启用，用于调试）
     */
    private Boolean debugLoggingEnabled = true;

    /**
     * 数据库查询超时时间（毫秒，默认5000）
     */
    private Integer queryTimeoutMs = 5000;

    /**
     * 最大并发内存访问数（默认100）
     */
    private Integer maxConcurrentMemories = 100;

    /**
     * 获取格式化的配置信息
     */
    @Override
    public String toString() {
        return "ChatMemoryProperties{" +
                "enabled=" + enabled +
                ", maxMessages=" + maxMessages +
                ", persistenceEnabled=" + persistenceEnabled +
                ", autoCleanupDays=" + autoCleanupDays +
                ", summarizeEnabled=" + summarizeEnabled +
                ", summarizeThreshold=" + summarizeThreshold +
                ", debugLoggingEnabled=" + debugLoggingEnabled +
                ", queryTimeoutMs=" + queryTimeoutMs +
                ", maxConcurrentMemories=" + maxConcurrentMemories +
                '}';
    }
}
