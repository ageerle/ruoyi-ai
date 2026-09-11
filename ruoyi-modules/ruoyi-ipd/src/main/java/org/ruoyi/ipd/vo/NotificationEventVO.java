package org.ruoyi.ipd.vo;

import org.ruoyi.ipd.domain.NotificationEvent;

import java.util.Date;

/**
 * View: NotificationEvent 的对外暴露视图，移除内部字段
 * （delFlag / dedupKey / retryCount / nextRetryAt / createBy / updateBy / createDept / params）。
 * 对应接口：/api/v1/notifications
 */
public record NotificationEventVO(
    Long id,
    Long receiverId,
    String eventType,
    String kind,
    String sourceType,
    Long sourceId,
    String title,
    String content,
    String actionUrl,
    String channel,
    String deliveryStatus,
    String readFlag,
    Date readAt,
    String targetChannel,
    String locale,
    Date createTime
) {
    public static NotificationEventVO from(NotificationEvent e) {
        return new NotificationEventVO(
            e.getId(),
            e.getReceiverId(),
            e.getEventType(),
            e.getKind(),
            e.getSourceType(),
            e.getSourceId(),
            e.getTitle(),
            e.getContent(),
            e.getActionUrl(),
            e.getChannel(),
            e.getDeliveryStatus(),
            e.getReadFlag(),
            e.getReadAt(),
            e.getTargetChannel(),
            e.getLocale(),
            e.getCreateTime()
        );
    }
}