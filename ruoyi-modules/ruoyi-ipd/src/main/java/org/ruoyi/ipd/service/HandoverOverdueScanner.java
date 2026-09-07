package org.ruoyi.ipd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 移交超期 15 日升级 + 每日提醒调度器（P2-7.4；AC-HAND-02）。
 *
 * <p>每日 09:00（server 时区）扫描 status=DRAFT 且 deadline_at 已过的移交记录，
 * 给当前在任 SUPER_ADMIN 发 ACTION 通知。升级事件通过 NotificationService 内置
 * dedup_key + escalated_at 守卫双层幂等（同 sourceType:eventType:sourceId:receiverId
 * 不重复发；同 handoverId 仅 first writer 标记 escalated_at）。
 *
 * <p>调度启用条件：本类被 @Component 注册；启用 @Scheduled 需 application 配置
 * {@code @EnableScheduling}（后续 OPS-04 scheduler 卡合入主树时启用；当前为占位入口，
 * 可由 HandoverController.scan-overdue 手动触发）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandoverOverdueScanner {

    private final HandoverService handoverService;

    /**
     * 每日 09:00 扫描并升级 + 提醒（生产启用 @EnableScheduling 后生效）。
     * 当前被注释（无 @EnableScheduling）以避免无调度器时的 NoSuchMethodError；
     * 测试覆盖走 HandoverController.scan-overdue 直接调用。
     *
     * <p>手动触发路径：POST /api/v1/handovers/scan-overdue（仅 SUPER_ADMIN）。
     */
    // @Scheduled(cron = "0 0 9 * * ?")
    public void dailyOverdueScanJob() {
        IpdActor systemActor = new IpdActor(0L, "SYSTEM", "SUPER_ADMIN", null);
        HandoverService.OverdueScanResult r = handoverService.scanOverdueDrafts(systemActor);
        log.info("P2-7.4 dailyOverdueScanJob: escalated={} reminded={}", r.escalated(), r.reminded());
    }
}