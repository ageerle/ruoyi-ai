package org.ruoyi.common.websocket.holder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketSession 用于保存当前所有在线的会话信息
 *
 * @author zendwang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebSocketSessionHolder {

    private static final Map<Long, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 将WebSocket会话添加到用户会话Map中
     *
     * @param sessionKey 会话键，用于检索会话
     * @param session    要添加的WebSocket会话
     */
    public static void addSession(Long sessionKey, WebSocketSession session) {
        removeSession(sessionKey);
        USER_SESSION_MAP.put(sessionKey, session);
    }

    /**
     * 从用户会话Map中移除指定会话键对应的WebSocket会话
     *
     * @param sessionKey 要移除的会话键
     */
    public static void removeSession(Long sessionKey) {
        WebSocketSession session = USER_SESSION_MAP.remove(sessionKey);
        try {
            session.close(CloseStatus.BAD_DATA);
        } catch (Exception ignored) {
        }
    }

    /**
     * 根据会话键从用户会话Map中获取WebSocket会话
     *
     * @param sessionKey 要获取的会话键
     * @return 与给定会话键对应的WebSocket会话，如果不存在则返回null
     */
    public static WebSocketSession getSessions(Long sessionKey) {
        return USER_SESSION_MAP.get(sessionKey);
    }

    /**
     * 获取存储在用户会话Map中所有WebSocket会话的会话键集合
     *
     * @return 所有WebSocket会话的会话键集合
     */
    public static Set<Long> getSessionsAll() {
        return USER_SESSION_MAP.keySet();
    }

    /**
     * 检查给定的会话键是否存在于用户会话Map中
     *
     * @param sessionKey 要检查的会话键
     * @return 如果存在对应的会话键，则返回true；否则返回false
     */
    public static Boolean existSession(Long sessionKey) {
        return USER_SESSION_MAP.containsKey(sessionKey);
    }
}
