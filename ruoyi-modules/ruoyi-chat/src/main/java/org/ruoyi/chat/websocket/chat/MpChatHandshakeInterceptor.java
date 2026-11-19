package org.ruoyi.chat.websocket.chat;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 小程序对话 WS 握手拦截器。
 * <p>
 * Authentication is mandatory. Browser clients pass the Sa-Token value in the legacy
 * {@code Authorization} query parameter because the WebSocket API cannot set custom headers.
 *
 * @author ruoyi team
 */
@Slf4j
@Component
public class MpChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_KEY = "mpChatUserId";
    public static final String LOGIN_USER_KEY = "mpChatLoginUser";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request.getURI());
        if (token == null) {
            log.warn("[mp-chat connect] rejected handshake without credentials");
            return false;
        }
        try {
            StpLogic logic = StpUtil.getStpLogic();
            Object loginId = logic.getLoginIdByTokenNotThinkFreeze(token);
            SaSession tokenSession = logic.getTokenSessionByToken(token, false);
            Object storedPrincipal = tokenSession == null
                ? null : tokenSession.get(LoginHelper.LOGIN_USER_KEY);
            LoginUser loginUser = storedPrincipal instanceof LoginUser candidate ? candidate : null;
            if (loginId == null || loginUser == null || loginUser.getUserId() == null
                || !loginUser.getLoginId().equals(loginId.toString())) {
                log.warn("[mp-chat connect] rejected invalid or expired credentials");
                return false;
            }
            attributes.put(USER_ID_KEY, loginUser.getUserId());
            attributes.put(LOGIN_USER_KEY, loginUser);
            return true;
        } catch (RuntimeException authenticationFailure) {
            log.warn("[mp-chat connect] authentication failed: {}",
                authenticationFailure.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    /**
     * 从握手 URL query 中解析 token。
     * 前端约定以 Authorization=Bearer xxx 形式透传，去掉 Bearer 前缀取真实 token。
     */
    private String resolveToken(URI uri) {
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = pair.substring(0, idx);
            if (!"Authorization".equalsIgnoreCase(key)) {
                continue;
            }
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            if (value == null) {
                return null;
            }
            value = value.trim();
            if (value.startsWith("Bearer ")) {
                value = value.substring(7).trim();
            }
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}
