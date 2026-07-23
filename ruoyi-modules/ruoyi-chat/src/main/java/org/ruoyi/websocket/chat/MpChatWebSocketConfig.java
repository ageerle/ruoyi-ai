package org.ruoyi.websocket.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 小程序对话 WebSocket 端点配置。
 * <p>
 * 独立注册 /chat/ws，无权限（握手拦截器仅做 token 解析、不拦截），
 * 与公共 ruoyi-common-websocket 的 /resource/websocket 互不干扰（后者受 websocket.enabled 控制，默认关闭）。
 *
 * @author ruoyi team
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MpChatWebSocketConfig {

    private final MpChatWebSocketHandler mpChatWebSocketHandler;
    private final MpChatHandshakeInterceptor mpChatHandshakeInterceptor;

    @Bean
    public WebSocketConfigurer mpChatWebSocketConfigurer() {
        return registry -> registry
            .addHandler(mpChatWebSocketHandler, "/chat/ws")
            .addInterceptors(mpChatHandshakeInterceptor)
            .setAllowedOrigins("*");
    }
}
