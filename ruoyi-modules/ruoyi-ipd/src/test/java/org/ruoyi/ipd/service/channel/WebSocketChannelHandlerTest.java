package org.ruoyi.ipd.service.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.websocket.holder.WebSocketSessionHolder;
import org.ruoyi.ipd.domain.NotificationChannelType;
import org.ruoyi.ipd.domain.NotificationEvent;
import org.ruoyi.ipd.service.NotificationDispatcher.NotificationDispatchException;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * WebSocket 通道处理器（WebSocketChannelHandler）单测 — W20-B WEBSOCKET 真实化。
 *
 * <p>5 测覆盖：
 * <ol>
 *   <li>用户在线：publishMessage 路径被走到（实际 fan-out 由 WebSocketUtils 负责，本期仅断言不抛）</li>
 *   <li>用户离线 + offlineFallbackToInbox=true：仅日志不抛</li>
 *   <li>用户离线 + offlineFallbackToInbox=false：抛 NotificationDispatchException</li>
 *   <li>序列化：含 eventType/title/content/actionUrl/locale 等字段</li>
 *   <li>channelType 返回 WEBSOCKET；事件缺 receiverId → 抛异常</li>
 * </ol>
 */
@Tag("dev")
@DisplayName("W20-B WebSocketChannelHandler 真实化单测")
@ExtendWith(MockitoExtension.class)
class WebSocketChannelHandlerTest {

    private WebSocketChannelHandler handler;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new WebSocketChannelHandler(mapper);
        // 默认 destination prefix / offline fallback
        setField(handler, "destinationPrefix", "/topic/notifications/");
        setField(handler, "offlineFallbackToInbox", true);
        // 清理上一次用例可能残留的 session
        WebSocketSessionHolder.removeSession(99001L);
    }

    @AfterEach
    void tearDown() {
        WebSocketSessionHolder.removeSession(99001L);
    }

    @Test
    @DisplayName("用户在线：走 publishMessage 路径（cross-instance fan-out）；不抛异常")
    void onlinePublishes() {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        WebSocketSessionHolder.addSession(99001L, mockSession);

        NotificationEvent event = baseEvent(99001L, "GATE_REJECTED", "zh-CN");
        event.setTitle("WebSocket 推送测试");

        // 不应抛异常
        handler.deliver(event);

        // session 仍存在（push 路径走 WebSocketUtils.sendMessage → session.sendMessage，mock session 不报错）
        assertThat(WebSocketSessionHolder.existSession(99001L)).isTrue();
    }

    @Test
    @DisplayName("用户离线 + offlineFallbackToInbox=true：仅日志不抛")
    void offlineWithFallbackDoesNotThrow() {
        WebSocketSessionHolder.removeSession(99002L);
        assertThat(WebSocketSessionHolder.existSession(99002L)).isFalse();
        setField(handler, "offlineFallbackToInbox", true);

        NotificationEvent event = baseEvent(99002L, "DEL_CROSS_GROUP_CC", "zh-CN");
        event.setTitle("离线兜底测试");

        handler.deliver(event); // 应不抛
    }

    @Test
    @DisplayName("用户离线 + offlineFallbackToInbox=false：抛 NotificationDispatchException")
    void offlineWithoutFallbackThrows() {
        WebSocketSessionHolder.removeSession(99003L);
        setField(handler, "offlineFallbackToInbox", false);

        NotificationEvent event = baseEvent(99003L, "DEL_REVIEW_OVERDUE", "zh-CN");
        event.setTitle("离线无兜底");

        assertThatThrownBy(() -> handler.deliver(event))
            .isInstanceOf(NotificationDispatchException.class)
            .hasMessageContaining("WEBSOCKET")
            .hasMessageContaining("99003");
    }

    @Test
    @DisplayName("序列化：JSON 含 eventType/title/content/actionUrl/locale/ts")
    void serializationIncludesKeyFields() {
        NotificationEvent event = baseEvent(99004L, "BID_WON", "en-US");
        event.setTitle("Test Title");
        event.setContent("Test Content");
        event.setActionUrl("/ipd/bids/123");

        String json = handler.serialize(event);

        assertThat(json).contains("\"eventId\"");
        assertThat(json).contains("\"eventType\":\"BID_WON\"");
        assertThat(json).contains("\"title\":\"Test Title\"");
        assertThat(json).contains("\"content\":\"Test Content\"");
        assertThat(json).contains("\"actionUrl\":\"/ipd/bids/123\"");
        assertThat(json).contains("\"locale\":\"en-US\"");
        assertThat(json).contains("\"ts\"");
    }

    @Test
    @DisplayName("channelType 返回 WEBSOCKET")
    void channelTypeReturnsWebSocket() {
        assertThat(handler.channelType()).isEqualTo(NotificationChannelType.WEBSOCKET);
    }

    @Test
    @DisplayName("事件缺 receiverId → 抛 NotificationDispatchException")
    void missingReceiverIdThrows() {
        NotificationEvent event = baseEvent(null, "BID_INVITED", "zh-CN");
        event.setTitle("缺 receiver");

        assertThatThrownBy(() -> handler.deliver(event))
            .isInstanceOf(NotificationDispatchException.class)
            .hasMessageContaining("receiverId");
    }

    private NotificationEvent baseEvent(Long receiverId, String type, String locale) {
        NotificationEvent e = NotificationEvent.builder()
            .id(System.currentTimeMillis())
            .receiverId(receiverId)
            .eventType(type)
            .kind("FYI")
            .sourceType("gates")
            .sourceId(1L)
            .dedupKey("gates:" + type + ":1:" + receiverId)
            .title("WS测试")
            .content("WS内容")
            .actionUrl("/ipd/test")
            .channel("MOCK")
            .targetChannel(NotificationChannelType.WEBSOCKET.getCode())
            .locale(locale)
            .deliveryStatus("PENDING")
            .retryCount(0)
            .readFlag("0")
            .build();
        e.setCreateTime(new Date());
        e.setUpdateTime(new Date());
        return e;
    }

    /** 反射设置私有字段（测试覆盖 @Value 默认值） */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = WebSocketChannelHandler.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("设置字段失败：" + name, e);
        }
    }
}