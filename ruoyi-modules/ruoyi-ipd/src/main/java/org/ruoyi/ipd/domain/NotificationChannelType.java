package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知路由通道枚举（ROOT-R2-P0-2：NotificationService 中间件化）。
 *
 * <p>与既有 {@code service.NotificationChannel}（投递处理器接口，已实现
 * {@code MockNotificationChannel}）语义不同：
 * <ul>
 *   <li>本枚举描述"事件要发到哪个通道"（业务侧路由决策，落 {@code notification_events.target_channel}）</li>
 *   <li>{@code NotificationChannel} 描述"谁来实际执行投递"（基础设施侧，MOCK/EmailSender/WebSocketPusher）</li>
 * </ul>
 *
 * <p>三类通道分别对接：
 * <ul>
 *   <li>{@link #INBOX} — 站内信（默认；收件箱查询主入口，权限码 OPERATION_NOTIFICATION_READ）</li>
 *   <li>{@link #EMAIL} — 邮件（外部 SMTP，stun-by-配 邮件推送 stub，本卡仅落库占位）</li>
 *   <li>{@link #WEBSOCKET} — 实时推送（基于 ruoyi-common-websocket，本卡仅落库占位）</li>
 * </ul>
 *
 * <p>枚举值采用 {@code @EnumValue} 注解支持 MyBatis-Plus 直接读写 VARCHAR。
 * 既有通知行 {@code target_channel=NULL} 视为 INBOX 默认通道（兼容升级）。
 */
@Getter
@AllArgsConstructor
public enum NotificationChannelType {

    /** 站内信：收件箱主入口，前端"我的通知"列表 */
    INBOX("INBOX", "站内信", true),

    /** 邮件：外部 SMTP/邮件推送 */
    EMAIL("EMAIL", "邮件", false),

    /** WebSocket 实时推送：ruoyi-common-websocket */
    WEBSOCKET("WEBSOCKET", "实时推送", false);

    @EnumValue
    private final String code;
    private final String displayName;
    /** 是否为本系统主通道（用于 dispatcher 默认路由） */
    private final boolean primary;

    /**
     * 安全反查：未知 code 返回 INBOX（默认通道，兼容既有数据 NULL）。
     */
    public static NotificationChannelType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return INBOX;
        }
        for (NotificationChannelType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return INBOX;
    }
}
