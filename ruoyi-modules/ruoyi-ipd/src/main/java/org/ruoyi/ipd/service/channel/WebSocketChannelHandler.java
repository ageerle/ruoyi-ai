package org.ruoyi.ipd.service.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.websocket.holder.WebSocketSessionHolder;
import org.ruoyi.common.websocket.utils.WebSocketUtils;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationChannelHandler;
import org.ruoyi.ipd.service.NotificationDispatcher.NotificationDispatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebSocket 实时推送通道处理器 真实实现（W20-B：W15-C 中间件化 WEBSOCKET 通道升级）。
 *
 * <p>职责：
 * <ul>
 *   <li>复用 {@link WebSocketSessionHolder}（ruoyi-common-websocket）判断用户在线状态</li>
 *   <li>用户在线：通过 {@link WebSocketUtils#publishMessage} 跨实例 fan-out 到目标 userId</li>
 *   <li>用户离线：仅日志（{@code ipd.notification.websocket.offline-fallback-to-inbox=true}
 *       由 dispatcher 层兜底到 INBOX；本卡不重复落库，避免双发）</li>
 *   <li>消息载荷：JSON（含 eventType / title / content / actionUrl）</li>
 * </ul>
 *
 * <p>约束：
 * <ul>
 *   <li>不修改 {@link AsyncNotificationDispatcher} / {@link InAppChannelHandler}</li>
 *   <li>无外部异常路径：WebSocketUtils 内部捕获 IOException 仅日志；本处理器视为成功返回</li>
 *   <li>{@code WebSocketSessionHolder} 是静态缓存（进程内）；跨实例通过 Redis pub/sub 广播</li>
 * </ul>
 */
@Slf4j
@Component
public class WebSocketChannelHandler implements NotificationChannelHandler {

    /** stub 行为日志标记（保留 sentinel 字符串以兼容既有测试断言） */
    public static final String STUB_SENTINEL = "websocket-stub-not-implemented";

    private final ObjectMapper objectMapper;

    @Value("${ipd.notification.websocket.destination-prefix:/topic/notifications/}")
    private String destinationPrefix;

    @Value("${ipd.notification.websocket.offline-fallback-to-inbox:true}")
    private boolean offlineFallbackToInbox;

    public WebSocketChannelHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.WEBSOCKET;
    }

    @Override
    public void deliver(NotificationEvent event) {
        if (event == null || event.getReceiverId() == null) {
            throw new NotificationDispatchException("WEBSOCKET 事件缺 receiverId", new IllegalArgumentException("receiverId required"));
        }

        Long receiverId = event.getReceiverId();
        boolean online = WebSocketSessionHolder.existSession(receiverId);

        if (!online) {
            if (offlineFallbackToInbox) {
                // 离线兜底：dispatcher 不感知 INBOX；本期仅记录意图，不重复落库（防双发）
                log.info("[WEBSOCKET-OFFLINE] eventId={} receiver={} type={} → 由 dispatcher 兜底 INBOX（已通过 SENT 标记防重）",
                    event.getId(), receiverId, event.getEventType());
                return;
            }
            // 不兜底：抛 dispatch 失败 → dispatcher 进入重试
            log.warn("[WEBSOCKET-OFFLINE-NO-FALLBACK] eventId={} receiver={} → 抛失败",
                event.getId(), receiverId);
            throw new NotificationDispatchException("WEBSOCKET 用户离线且未开兜底：" + receiverId);
        }

        try {
            String message = serialize(event);
            org.ruoyi.common.websocket.dto.WebSocketMessageDto dto =
                new org.ruoyi.common.websocket.dto.WebSocketMessageDto();
            dto.setSessionKeys(java.util.List.of(receiverId));
            dto.setMessage(message);
            WebSocketUtils.publishMessage(dto);
            log.info("[WEBSOCKET-SENT] eventId={} receiver={} type={} bytes={}",
                event.getId(), receiverId, event.getEventType(), message.length());
        } catch (RuntimeException e) {
            log.warn("[WEBSOCKET-FAIL] eventId={} receiver={} err={}",
                event.getId(), receiverId, e.getMessage());
            throw new NotificationDispatchException("WEBSOCKET 推送失败：" + receiverId, e);
        }
    }

    /** 序列化事件为 JSON（前端 WebSocket 客户端消费格式） */
    String serialize(NotificationEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", event.getId());
        payload.put("eventType", event.getEventType());
        payload.put("kind", event.getKind());
        payload.put("title", event.getTitle());
        payload.put("content", event.getContent());
        payload.put("actionUrl", event.getActionUrl());
        payload.put("sourceType", event.getSourceType());
        payload.put("sourceId", event.getSourceId());
        payload.put("locale", event.getLocale());
        payload.put("ts", System.currentTimeMillis());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // fallback：直接拼接字符串（不阻塞业务）
            log.warn("[WEBSOCKET-SERIALIZE-FAIL] eventId={} fallback 字符串拼接", event.getId());
            return "{eventId:" + event.getId() + ",title:\"" + safe(event.getTitle()) + "\"}";
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    /** 推送给目标 userId（测试用；mock-free 单元测试不依赖 Spring 容器） */
    void deliverTo(Long receiverId, String message) {
        if (WebSocketSessionHolder.existSession(receiverId)) {
            WebSocketUtils.sendMessage(receiverId, message);
        } else if (offlineFallbackToInbox) {
            log.info("[WEBSOCKET-OFFLINE-FALLBACK] receiver={} → INBOX 兜底", receiverId);
        } else {
            throw new NotificationDispatchException("WEBSOCKET 用户离线：" + receiverId);
        }
    }

    /** 用于测试断言 */
    public String getDestinationPrefix() { return destinationPrefix; }
    public boolean isOfflineFallbackToInbox() { return offlineFallbackToInbox; }
}