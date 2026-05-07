package org.ruoyi.service.chat.impl.memory.strategy;



/**
 * 内存压缩策略接口
 * 定义消息压缩的抽象行为，支持多种压缩算法
 *
 * @author yang
 * @date 2026-04-29
 */
public interface MemoryCompressionStrategy {

    /**
     * 获取策略名称
     *
     * @return 策略名称（如 truncation, summarization）
     */
    String getName();

    /**
     * 判断是否需要压缩
     *
     * @param context 压缩上下文
     * @return true 表示需要压缩
     */
    boolean needsCompression(CompressionContext context);

    /**
     * 执行压缩
     *
     * @param context 压缩上下文
     * @return 压缩结果
     */
    CompressionResult compress(CompressionContext context);

    /**
     * 获取策略优先级（数值越小优先级越高）
     * 用于多策略组合时的执行顺序
     *
     * @return 优先级（默认 100）
     */
    default int getPriority() {
        return 100;
    }

    /**
     * 是否支持与其他策略组合使用
     *
     * @return true 表示可以组合
     */
    default boolean isComposable() {
        return false;
    }
}
