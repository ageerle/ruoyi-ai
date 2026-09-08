package org.ruoyi.ipd.service.ai;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * R-NEW S-7：promptLen bucket 切换日志聚合器。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>桶切换触发</b>：仅当本次 bucket ≠ 上次 bucket 时落 log.warn；同桶连续请求不写日志
 *       （噪声消除核心）。</li>
 *   <li><b>计数携带</b>：切换日志携带自上次切换以来累计请求数（counter），便于排障
 *       「这段时间请求密度」。</li>
 *   <li><b>实例隔离</b>：当前为 AiChatClient 内字段（非 Spring bean），不同 AiChatClient 实例
 *       不共享计数（与 Spring bean 生命周期一致）。</li>
 *   <li><b>线程安全</b>：{@link AtomicReference} + {@link AtomicLong}，CAS 无锁。</li>
 *   <li><b>桶相等短路</b>：同 bucket 走 fast path，仅 increment counter，无 log。</li>
 * </ul>
 *
 * <p>不抛异常——日志聚合器失败不影响主请求。
 */
@Slf4j
public class PromptLenBucketLogger {

    private final AtomicReference<PromptLenBucket> current = new AtomicReference<>(PromptLenBucket.LT_100);
    private final AtomicLong countSinceTransition = new AtomicLong(0);

    /**
     * 喂入一条 promptLen，返回本次是否触发切换日志。
     *
     * @return true 表示发生了桶切换并已落日志；false 表示同桶 fast path
     */
    public boolean record(int promptLen) {
        PromptLenBucket next = PromptLenBucket.of(promptLen);
        long count = countSinceTransition.incrementAndGet();
        PromptLenBucket prev = current.get();
        if (prev == next) {
            return false;
        }
        // CAS 切换；多线程并发切换时仅第一次落日志
        if (!current.compareAndSet(prev, next)) {
            return false;
        }
        log.warn("[AI] promptLen bucket transition: prev={}({}) next={}({}) countSinceTransition={}",
            prev.label(), prev.min(), next.label(), next.min(), count);
        // 切完重置计数（保持「自上次切换以来」语义）
        countSinceTransition.set(0);
        return true;
    }

    /** 测试/观测 helper：当前桶。 */
    public PromptLenBucket currentBucket() { return current.get(); }

    /** 测试/观测 helper：自上次切换以来的累计请求数。 */
    public long countSinceTransition() { return countSinceTransition.get(); }
}
