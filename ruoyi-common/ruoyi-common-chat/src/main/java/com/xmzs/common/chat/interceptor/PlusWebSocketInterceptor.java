package com.xmzs.common.chat.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手请求的拦截器
 *
 * @author zendwang
 */
@Slf4j
public class PlusWebSocketInterceptor implements HandshakeInterceptor {

    /**
     * 握手前
     *
     * @param request    request
     * @param response   response
     * @param wsHandler  wsHandler
     * @param attributes attributes
     * @return 是否握手成功
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        return true;
    }


    /**
     * 握手后
     *
     * @param request   request
     * @param response  response
     * @param wsHandler wsHandler
     * @param exception 异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
