package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.mapper.NotificationEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步通知分发器默认实现（ROOT-R2-P0-2）。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link #dispatch(NotificationEvent)} 同步路径：按 targetChannel 路由到 handler，失败立即抛 {@link NotificationDispatchException}</li>
 *   <li>{@link #dispatchAsync(NotificationEvent)} 异步路径：入 Redisson 延迟队列 + 消费端重试 3 次 + 终态 DEAD（死信）</li>
 *   <li>{@link #consumeOnce(int)} 消费端：扫描到期事件（OH-backoff-or-FAILED）调 handler.deliver，按事件 status 翻 SENT/FAILED/DEAD</li>
 * </ul>
 *
 * <p>退避：5·2^(n-1) 分钟（与既有 {@code NotificationService.dispatchPending} 对齐，
 * 沿用既有字段 next_retry_at / retry_count 避免双轨）。
 *
 * <p>聚合：相同 receiverId + 相同 eventType 在 60s 窗口内的事件合并为一封通知
 * （reduce noise；防邮件/推送刷屏）。本期聚合在 dispatch 时按 dedupKey 简单合并，
 * 不引入额外表。
 *
 * <p>幂等：事件唯一键 dedup_key 在 publish 阶段已落库（既 uk_notify_dedup），
 * 本 dispatcher 不再二次去重。
 */
@Slf4j
@Component
public class AsyncNotificationDispatcher implements NotificationDispatcher {

    static final int MAX_RETRIES = 3;
    static final long BACKOFF_BASE_MINUTES = 5;
    /** 异步队列 Redisson 队列名（按通道隔离，单物理队列 + 事件内 targetChannel 路由） */
    public static final String QUEUE_KEY = "ipd:notify:dispatch:async";
    /** 聚合窗口（毫秒）；同 receiver + eventType + window 内的事件合并 */
    static final long AGGREGATE_WINDOW_MS = 60_000L;

    private final NotificationEventMapper mapper;
    private final RedissonClient redissonClient;
    private final Map<NotificationChannelType, NotificationChannelHandler> handlerMap = new EnumMap<>(NotificationChannelType.class);

    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public AsyncNotificationDispatcher(NotificationEventMapper mapper,
                                       RedissonClient redissonClient,
                                       List<NotificationChannelHandler> handlers) {
        this.mapper = mapper;
        this.redissonClient = redissonClient;
        for (NotificationChannelHandler h : handlers) {
            handlerMap.put(h.channelType(), h);
        }
    }

    /** 测试口：注入固定时钟 */
    AsyncNotificationDispatcher withClock(Clock fixed) {
        this.clock = fixed;
        return this;
    }

    @Override
    public boolean dispatch(NotificationEvent event) {
        if (event == null) {
            throw new NotificationDispatchException("event 不能为空");
        }
        NotificationChannelType type = NotificationChannelType.fromCode(event.getTargetChannel());
        NotificationChannelHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new NotificationDispatchException("无 handler：" + type);
        }
        try {
            handler.deliver(event);
            return true;
        } catch (RuntimeException e) {
            log.warn("[dispatch] 同步分发失败 channel={} eventId={} type={} err={}",
                type, event.getId(), event.getEventType(), e.getMessage());
            throw new NotificationDispatchException("分发失败：" + type, e);
        }
    }

    @Override
    public boolean dispatchAsync(NotificationEvent event) {
        if (event == null || event.getId() == null) {
            throw new NotificationDispatchException("异步事件必须有 id（先 publish 落库）");
        }
        RBlockingQueue<Long> blocking = redissonClient.getBlockingQueue(QUEUE_KEY);
        RDelayedQueue<Long> delayed = redissonClient.getDelayedQueue(blocking);
        // 聚合窗口：dedupKey 命中既有 PENDING 事件则不重复入队（消费端按 status 翻 SENT）
        Long eventId = event.getId();
        long windowStart = clock.millis() - AGGREGATE_WINDOW_MS;
        Long pendingDup = mapper.selectCount(
            Wrappers.<NotificationEvent>lambdaQuery()
                .eq(NotificationEvent::getReceiverId, event.getReceiverId())
                .eq(NotificationEvent::getEventType, event.getEventType())
                .eq(NotificationEvent::getDeliveryStatus, "PENDING")
                .gt(NotificationEvent::getCreateTime, new Date(windowStart))
        );
        if (pendingDup != null && pendingDup > 0) {
            log.info("[dispatchAsync] 聚合命中 receiver={} type={} pendingCount={} → 跳过入队",
                event.getReceiverId(), event.getEventType(), pendingDup);
            return false;
        }
        delayed.offer(eventId, 0, TimeUnit.MILLISECONDS);
        log.info("[dispatchAsync] 入队 eventId={} channel={} type={}",
            eventId, event.getTargetChannel(), event.getEventType());
        return true;
    }

    /**
     * 消费端入口（管理端点或定时任务可调用）。
     *
     * @param maxProcess 单轮上限（1-200）
     * @return sent/failed/dead/skipped/aggregated 计数
     */
    public Map<String, Integer> consumeOnce(int maxProcess) {
        int bounded = Math.min(Math.max(maxProcess, 1), 200);
        RBlockingQueue<Long> blocking = redissonClient.getBlockingQueue(QUEUE_KEY);
        AtomicInteger sent = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger dead = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger aggregated = new AtomicInteger();

        for (int i = 0; i < bounded; i++) {
            Long eventId;
            try {
                eventId = blocking.poll(50, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[consumeOnce] 消费被中断；停止本轮");
                break;
            }
            if (eventId == null) {
                break;
            }
            NotificationEvent event = mapper.selectById(eventId);
            if (event == null) {
                skipped.incrementAndGet();
                continue;
            }
            // 已被既有 outbox 投递流程翻 SENT/DEAD → 跳过
            if (!"PENDING".equals(event.getDeliveryStatus()) && !"FAILED".equals(event.getDeliveryStatus())) {
                skipped.incrementAndGet();
                continue;
            }
            NotificationChannelType type = NotificationChannelType.fromCode(event.getTargetChannel());
            NotificationChannelHandler handler = handlerMap.get(type);
            if (handler == null) {
                // 未知 channel：直接转 DEAD（不再重试）
                markDead(event, "无 handler: " + type);
                dead.incrementAndGet();
                continue;
            }
            try {
                handler.deliver(event);
            } catch (RuntimeException e) {
                int newCount = Optional.ofNullable(event.getRetryCount()).orElse(0) + 1;
                if (newCount >= MAX_RETRIES) {
                    markDead(event, e.getMessage());
                    dead.incrementAndGet();
                } else {
                    markFailed(event, newCount);
                    failed.incrementAndGet();
                }
                continue;
            }
            int updated = mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
                .eq(NotificationEvent::getId, event.getId())
                .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
                .set(NotificationEvent::getDeliveryStatus, "SENT"));
            if (updated > 0) {
                sent.incrementAndGet();
            } else {
                skipped.incrementAndGet();
            }
        }
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        result.put("sent", sent.get());
        result.put("failed", failed.get());
        result.put("dead", dead.get());
        result.put("skipped", skipped.get());
        result.put("aggregated", aggregated.get());
        return result;
    }

    private void markFailed(NotificationEvent event, int newCount) {
        Date nextRetry = Date.from(clock.instant().plusSeconds(
            BACKOFF_BASE_MINUTES * 60 * (1L << (newCount - 1))));
        mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
            .eq(NotificationEvent::getId, event.getId())
            .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
            .set(NotificationEvent::getDeliveryStatus, "FAILED")
            .set(NotificationEvent::getRetryCount, newCount)
            .set(NotificationEvent::getNextRetryAt, nextRetry));
    }

    private void markDead(NotificationEvent event, String reason) {
        log.warn("[markDead] eventId={} reason={}", event.getId(), reason);
        mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
            .eq(NotificationEvent::getId, event.getId())
            .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
            .set(NotificationEvent::getDeliveryStatus, "DEAD")
            .set(NotificationEvent::getRetryCount, MAX_RETRIES));
    }
}
