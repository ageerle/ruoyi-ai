package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.mapper.NotificationEventMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 站内通知与可执行待办事件服务（OPS-05；AC-TEAM/GATE/DEL 通知场景底座）。
 *
 * <p>outbox 语义：业务方在业务事务内 {@link #publish}（dedup_key 幂等，重复发布返回既有行，
 * 同一事件对同一接收者只投一次）；{@link #dispatchPending} 为消费端——扫描到期行经
 * {@link NotificationChannel} 投递，成功以条件 UPDATE（status IN (PENDING,FAILED)）翻 SENT，
 * 并发双消费者以该乐观守卫仲裁；失败按 5·2^(n-1) 分钟指数退避，重试达 {@link #MAX_RETRIES}
 * 转 DEAD（死信可观察，不静默丢失）。调度接线（定时轮询）待 OPS-04 scheduler 合入主树，
 * 当前可由管理端点手动轮询。
 *
 * <p>kind 严格分流：FYI=跨组知会（如 AC-DEL-04 协同组组长知会）与 ACTION=可执行审批行动
 * （如 Gate 双签、删除初审待办）——同一业务动作两类事件各自成行，前端待办列表只取 ACTION。
 * 收件箱按 receiver_id 强隔离（AC-TEAM-01：未被邀标的研发 PM 看不到该通知）。
 */
@Service
public class NotificationService {

    /** 跨组知会 */
    public static final String KIND_FYI = "FYI";
    /** 可执行审批行动 */
    public static final String KIND_ACTION = "ACTION";
    private static final Set<String> KINDS = Set.of(KIND_FYI, KIND_ACTION);

    /** 重试上限：第 1/2/3 次失败仍 FAILED，此后转 DEAD（不再被扫中） */
    static final int MAX_RETRIES = 3;
    /** 退避基数（分钟）：第 n 次失败后等待 5·2^(n-1) 分钟 */
    static final long BACKOFF_BASE_MINUTES = 5;

    /**
     * 事件类型目录（与验收清单 AC 对齐；实际发布接线在各业务下游卡）：
     * BID_INVITED（AC-TEAM-01 邀标）/ BID_WON / BID_LOST（AC-TEAM-05 中标与落选，双向）
     * / BID_EXPIRING_SOON（AC-TEAM-06 到期前 3 天）/ BID_SELECT_OVERDUE（AC-TEAM-07 遴选超期升级组长）
     * / BID_CONDITIONS_CHANGED（AC-TEAM-13 条件变更知会已应标者）
     * / GATE_REJECTED（AC-GATE-05 双方收通知）/ GATE_SIGN_SOON（AC-GATE-09 签署期限前 1 天）
     * / G5_REVIEW_TODO（AC-GATE-13 上市 +90 天复盘待办）/ GATE_CONDITION_OVERDUE（AC-GATE-17 条件关闭逾期提醒）
     * / DEL_CROSS_GROUP_CC（AC-DEL-04 协同组组长知会，FYI）/ DEL_REJECTED（AC-DEL-05 初审驳回）
     * / DEL_REVIEW_OVERDUE（AC-DEL-07 审核超期提醒并升级）。
     */
    public static final class Types {
        public static final String BID_INVITED = "BID_INVITED";
        public static final String BID_WON = "BID_WON";
        public static final String BID_LOST = "BID_LOST";
        public static final String BID_EXPIRING_SOON = "BID_EXPIRING_SOON";
        public static final String BID_SELECT_OVERDUE = "BID_SELECT_OVERDUE";
        public static final String BID_CONDITIONS_CHANGED = "BID_CONDITIONS_CHANGED";
        public static final String GATE_REJECTED = "GATE_REJECTED";
        public static final String GATE_SIGN_SOON = "GATE_SIGN_SOON";
        public static final String G5_REVIEW_TODO = "G5_REVIEW_TODO";
        public static final String GATE_CONDITION_OVERDUE = "GATE_CONDITION_OVERDUE";
        public static final String DEL_CROSS_GROUP_CC = "DEL_CROSS_GROUP_CC";
        public static final String DEL_REJECTED = "DEL_REJECTED";
        public static final String DEL_REVIEW_OVERDUE = "DEL_REVIEW_OVERDUE";
        public static final String ACTION_OVERDUE = "ACTION_OVERDUE";
        /** P2-3.3 AC-TEAM-08 招标到期无人应标提示给市场 PM */
        public static final String BID_EXPIRED_NO_RESPONSE = "BID_EXPIRED_NO_RESPONSE";
        /** P2-5.4 AC-GATE-07 第 3 轮起双方产品组长自动列席 */
        public static final String GATE_ROUND_OBSERVER = "GATE_ROUND_OBSERVER";
        /** P2-5.4 AC-GATE-07b 第 5 轮起超管介入 */
        public static final String GATE_ADMIN_INTERVENE = "GATE_ADMIN_INTERVENE";
        /** P2-5.4 AC-GATE-08 超期弃权流转结果知会双方 */
        public static final String GATE_ABSTAINED = "GATE_ABSTAINED";
        /** P2-5.4 AC-GATE-10 双PM分歧邀请组长仲裁 */
        public static final String GATE_ARBITRATION_REQUEST = "GATE_ARBITRATION_REQUEST";
        /** P2-5.4 AC-GATE-10 两组长仲裁一致结果知会双方 */
        public static final String GATE_ARBITRATION_RESULT = "GATE_ARBITRATION_RESULT";
        /** P2-5.4 AC-GATE-10 两组不一致升级超管终裁 */
        public static final String GATE_FINAL_RULING_REQUEST = "GATE_FINAL_RULING_REQUEST";
        /** P2-5.4 AC-GATE-10 超管终裁结果知会双方 */
        public static final String GATE_FINAL_RULING_RESULT = "GATE_FINAL_RULING_RESULT";
        /** P3-8.2 AC-INC-40：负反馈认定执行后知会主责/连带 PM（ACTION） */
        public static final String NEGATIVE_FEEDBACK_EXECUTED = "NEGATIVE_FEEDBACK_EXECUTED";
        /** P3-8.2：负反馈解除恢复 bonusEligible 知会主责/连带 PM（FYI） */
        public static final String NEGATIVE_FEEDBACK_LIFTED = "NEGATIVE_FEEDBACK_LIFTED";
        /** HIGH-4.1：KPI 月度截止日前 1 天提醒产品组长（FYI，不升级） */
        public static final String KPI_DUE_SOON = "KPI_DUE_SOON";

        private Types() {
        }
    }

    private final NotificationEventMapper mapper;
    private final NotificationChannel channel;
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    public NotificationService(NotificationEventMapper mapper, NotificationChannel channel) {
        this.mapper = mapper;
        this.channel = channel;
    }

    /** 测试口：注入固定时钟（退避/已读时间断言）；生产走系统时钟。 */
    NotificationService withClock(java.time.Clock fixed) {
        this.clock = fixed;
        return this;
    }

    /**
     * 发布事件（幂等）：dedup_key = source_type:event_type:source_id:receiver_id 撞库时
     * 返回既有行不重发（MySQL 下捕获 DuplicateKey 不污染事务）。同一事件发给多个接收者
     * 各自成行（receiver 维度独立去重，如 AC-TEAM-05 中标 1 人 + 落选 2 人）。
     *
     * @return 落库行（重复发布时为既有行）
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationEvent publish(Long receiverId, String eventType, String kind,
                                     String sourceType, Long sourceId, String title,
                                     String content, String actionUrl) {
        return doPublish(receiverId, eventType, kind, sourceType, sourceId, title, content, actionUrl,
            sourceType + ":" + eventType + ":" + sourceId + ":" + receiverId);
    }

    /**
     * P1-4.4 每日提醒专用：dedupKey 追加自然日（yyyyMMdd）。
     * 同日重扫不重发（重复扫描不多通知）；次日可再提醒（AC-IPD-12 每日提醒）。
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationEvent publishDaily(Long receiverId, String eventType, String kind,
                                          String sourceType, Long sourceId, String title,
                                          String content, String actionUrl, java.util.Date day) {
        String dayStamp = new java.text.SimpleDateFormat("yyyyMMdd").format(day);
        return doPublish(receiverId, eventType, kind, sourceType, sourceId, title, content, actionUrl,
            sourceType + ":" + eventType + ":" + sourceId + ":" + receiverId + ":" + dayStamp);
    }

    private NotificationEvent doPublish(Long receiverId, String eventType, String kind,
                                         String sourceType, Long sourceId, String title,
                                         String content, String actionUrl, String dedupKey) {
        requireArg(receiverId != null, "receiverId 必填");
        requireArg(eventType != null && !eventType.isBlank(), "eventType 必填");
        requireArg(kind != null && KINDS.contains(kind), "kind 仅允许 FYI|ACTION");
        requireArg(sourceType != null && !sourceType.isBlank(), "sourceType 必填");
        requireArg(sourceId != null, "sourceId 必填");
        requireArg(title != null && !title.isBlank(), "title 必填");
        requireArg(title.length() <= 200, "title 超长（≤200）");

        NotificationEvent row = NotificationEvent.builder()
            .receiverId(receiverId).eventType(eventType).kind(kind)
            .sourceType(sourceType).sourceId(sourceId).dedupKey(dedupKey)
            .title(title).content(content).actionUrl(actionUrl)
            .channel(channel.code()).deliveryStatus("PENDING")
            .retryCount(0).readFlag("0")
            .build();
        try {
            mapper.insert(row);
            return row;
        } catch (DuplicateKeyException e) {
            NotificationEvent existing = mapper.selectOne(
                new LambdaQueryWrapper<NotificationEvent>().eq(NotificationEvent::getDedupKey, dedupKey));
            return existing != null ? existing : row;
        }
    }

    /**
     * 本人收件箱（receiver 强隔离：查询恒带 receiver_id 条件）。
     *
     * @param receiverId  接收者
     * @param unreadOnly true=仅未读
     * @return 事件列表（新→旧）
     */
    public List<NotificationEvent> inbox(Long receiverId, boolean unreadOnly) {
        LambdaQueryWrapper<NotificationEvent> q = new LambdaQueryWrapper<NotificationEvent>()
            .eq(NotificationEvent::getReceiverId, receiverId)
            .orderByDesc(NotificationEvent::getCreateTime)
            .orderByDesc(NotificationEvent::getId);
        if (unreadOnly) {
            q.eq(NotificationEvent::getReadFlag, "0");
        }
        return mapper.selectList(q);
    }

    /** 未读计数（页03 站内信红点）。 */
    public long unreadCount(Long receiverId) {
        return mapper.selectCount(new LambdaQueryWrapper<NotificationEvent>()
            .eq(NotificationEvent::getReceiverId, receiverId)
            .eq(NotificationEvent::getReadFlag, "0"));
    }

    /**
     * 标记已读（仅本人；他人 ID 视为不存在，杜绝探测）。
     *
     * @param id         事件 ID
     * @param receiverId 当前会话人
     * @return 更新后行（已读幂等）
     */
    public NotificationEvent markRead(Long id, Long receiverId) {
        NotificationEvent row = mapper.selectById(id);
        if (row == null || !receiverId.equals(row.getReceiverId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if ("1".equals(row.getReadFlag())) {
            return row;
        }
        mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
            .eq(NotificationEvent::getId, id)
            .eq(NotificationEvent::getReceiverId, receiverId)
            .set(NotificationEvent::getReadFlag, "1")
            .set(NotificationEvent::getReadAt, Date.from(clock.instant())));
        row.setReadFlag("1");
        row.setReadAt(Date.from(clock.instant()));
        return row;
    }

    /** 全部已读（仅本人未读行）。 */
    public int markAllRead(Long receiverId) {
        return mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
            .eq(NotificationEvent::getReceiverId, receiverId)
            .eq(NotificationEvent::getReadFlag, "0")
            .set(NotificationEvent::getReadFlag, "1")
            .set(NotificationEvent::getReadAt, Date.from(clock.instant())));
    }

    /**
     * outbox 消费端：扫描到期行（PENDING，或 FAILED 且退避期满）逐条投递。
     * 单条失败不影响后续行；结果计数供运维观察（调用方可为管理端点或后续调度器）。
     *
     * @param limit 单轮上限（1-200）
     * @return sent/failed/dead/skipped 计数
     */
    public Map<String, Integer> dispatchPending(int limit) {
        int bounded = Math.min(Math.max(limit, 1), 200);
        Date now = Date.from(clock.instant());
        List<NotificationEvent> due = mapper.selectList(
            new LambdaQueryWrapper<NotificationEvent>()
                .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
                .and(w -> w.isNull(NotificationEvent::getNextRetryAt)
                    .or().le(NotificationEvent::getNextRetryAt, now))
                .orderByAsc(NotificationEvent::getId)
                .last("limit " + bounded));

        int sent = 0;
        int failed = 0;
        int dead = 0;
        int skipped = 0;
        for (NotificationEvent event : due) {
            try {
                channel.send(event);
            } catch (RuntimeException e) {
                int newCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
                if (newCount >= MAX_RETRIES) {
                    mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
                        .eq(NotificationEvent::getId, event.getId())
                        .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
                        .set(NotificationEvent::getDeliveryStatus, "DEAD")
                        .set(NotificationEvent::getRetryCount, newCount));
                    dead++;
                } else {
                    Date nextRetry = Date.from(clock.instant().plusSeconds(
                        BACKOFF_BASE_MINUTES * 60 * (1L << (newCount - 1))));
                    mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
                        .eq(NotificationEvent::getId, event.getId())
                        .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
                        .set(NotificationEvent::getDeliveryStatus, "FAILED")
                        .set(NotificationEvent::getRetryCount, newCount)
                        .set(NotificationEvent::getNextRetryAt, nextRetry));
                    failed++;
                }
                continue;
            }
            // 条件 UPDATE 乐观守卫：返回 0 = 并发消费者已处理 → 记 skipped 不计 sent
            int updated = mapper.update(null, Wrappers.<NotificationEvent>lambdaUpdate()
                .eq(NotificationEvent::getId, event.getId())
                .in(NotificationEvent::getDeliveryStatus, "PENDING", "FAILED")
                .set(NotificationEvent::getDeliveryStatus, "SENT"));
            if (updated > 0) {
                sent++;
            } else {
                skipped++;
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("sent", sent);
        result.put("failed", failed);
        result.put("dead", dead);
        result.put("skipped", skipped);
        return result;
    }

    private static void requireArg(boolean ok, String message) {
        if (!ok) {
            throw new IpdBusinessException(message);
        }
    }
}
