package com.xmzs.common.chat.holder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketSession 用于保存当前所有在线的会话信息
 *
 * @author zendwang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebSocketSessionHolder {

    private static final Map<String, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    public static void addSession(String sessionKey, WebSocketSession session) {
        USER_SESSION_MAP.put(sessionKey, session);
    }

    public static void removeSession(String sessionKey) {
        if (USER_SESSION_MAP.containsKey(sessionKey)) {
            USER_SESSION_MAP.remove(sessionKey);
        }
    }

    public static WebSocketSession getSessions(Long sessionKey) {
        return USER_SESSION_MAP.get(sessionKey);
    }

    public static Boolean existSession(Long sessionKey) {
        return USER_SESSION_MAP.containsKey(sessionKey);
    }
}
