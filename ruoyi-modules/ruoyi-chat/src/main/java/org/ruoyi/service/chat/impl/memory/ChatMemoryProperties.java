package org.ruoyi.service.chat.impl.memory;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 聊天长期记忆配置属性
 * 支持通过 application.yml 配置长期记忆行为
 *
 * @author ageerle@163.com
 * @date 2026/01/10
 */
@Data
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryProperties {

    /**
     * 是否启用长期记忆功能（默认启用）
     */
    private Boolean enabled = true;

    /**
     * 内存管理策略（默认 token）
     * - message: 固定消息数量
     * - token: 基于 Token 数量
     * - hybrid: 混合策略（Token + 摘要）
     */
    private String strategy = "token";

    /**
     * 消息窗口大小 - 最多保留的消息条数（默认20）
     * 仅当 strategy=message 时生效
     */
    private Integer maxMessages = 20;

    /**
     * 最大 Token 数
     * 仅当 strategy=token 或 hybrid 时生效
     * 如果为空，则根据模型自动获取
     */
    private Integer maxTokens;

    /**
     * 预留给回复的 Token 数（默认 2000）
     */
    private Integer reservedForReply = 2000;

    /**
     * 摘要触发阈值 - Token 使用比例（默认 0.7，即 70%）
     * 当 Token 使用量超过此比例时，对旧消息进行摘要
     * 例如: maxTokens=128000, 比例=0.7, 则 Token>89600 时触发摘要
     * 建议值: 0.6-0.8 (60%-80%)
     */
    private Double summarizeTokenRatio = 0.7;

    /**
     * 摘要触发阈值 - 消息数量（默认 10）
     * 当消息数超过此值时才考虑摘要（避免消息太少时摘要无意义）
     */
    private Integer summarizeThreshold = 10;

    /**
     * 是否保留系统消息（默认 true）
     * 系统消息不会被截断
     */
    private Boolean preserveSystemMessages = true;

    /**
     * 未知模型是否回退到消息数量策略（默认启用）
     * 当模型不在 Token 限制列表中时，自动使用固定消息数量策略
     * 关闭后，未知模型将使用默认 Token 限制 (4096)
     */
    private Boolean fallbackToMessageStrategy = true;

    /**
     * 未知模型回退时的消息数量（默认20）
     * 仅当 fallbackToMessageStrategy=true 时生效
     */
    private Integer fallbackMaxMessages = 20;

    /**
     * 是否使用策略框架（默认启用）
     * 启用后，使用 CompressionStrategyManager 进行压缩
     * 禁用后，使用原有硬编码逻辑
     */
    private Boolean useStrategyFramework = true;

    /**
     * 摘要模型策略（默认使用当前对话模型）
     * - current: 使用当前对话模型进行摘要（质量高，成本高）
     * - smart: 智能映射到轻量级模型（成本低，如 gpt-4o-mini、glm-4-flash）
     * - custom: 使用自定义模型（需配置 summarizerCustomModel）
     */
    private String summarizerStrategy = "current";

    /**
     * 自定义摘要模型名称
     * 仅当 summarizerStrategy=custom 时生效
     * 例如: "gpt-4o-mini", "glm-4-flash", "qwen-turbo"
     */
    private String summarizerCustomModel;

    /**
     * 获取格式化的配置信息
     */
    @Override
    public String toString() {
        return "ChatMemoryProperties{" +
                "enabled=" + enabled +
                ", strategy='" + strategy + '\'' +
                ", maxMessages=" + maxMessages +
                ", maxTokens=" + maxTokens +
                ", reservedForReply=" + reservedForReply +
                ", summarizeTokenRatio=" + summarizeTokenRatio +
                ", summarizeThreshold=" + summarizeThreshold +
                ", preserveSystemMessages=" + preserveSystemMessages +
                ", fallbackToMessageStrategy=" + fallbackToMessageStrategy +
                ", fallbackMaxMessages=" + fallbackMaxMessages +
                ", useStrategyFramework=" + useStrategyFramework +
                ", summarizerStrategy='" + summarizerStrategy + '\'' +
                ", summarizerCustomModel='" + summarizerCustomModel + '\'' +
                '}';
    }
}
