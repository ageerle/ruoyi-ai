package org.ruoyi.websocket.chat;

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
 * 无权限：握手始终放行。仅尝试从握手 URL 的 Authorization 参数解析登录用户，
 * 解析成功则把 userId 放入 session attributes 供 handler 落库使用；解析失败按匿名处理。
 * <p>
 * 注意：与公共 {@code PlusWebSocketInterceptor} 不同，这里不做 clientid 一致性校验，
 * 也不抛出认证异常——对话端点对未登录用户同样开放。
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
        // 无权限：握手始终放行，且不调用 sa-token（LoginHelper.getLoginUser 会触发 getTokenSessionByToken，
        // 在 is-share:false 下有冻结当前 token 的副作用，导致随后 mvc 请求 401 token 已被冻结。
        // 对话端点本就不依赖登录态，userId 留空，落库跳过）。
        log.info("[mp-chat connect] 匿名对话连接");
        return true;
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
