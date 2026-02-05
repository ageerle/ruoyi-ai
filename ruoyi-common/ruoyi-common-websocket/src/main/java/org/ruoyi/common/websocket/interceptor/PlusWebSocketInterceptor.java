package org.ruoyi.common.websocket.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.utils.ServletUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static org.ruoyi.common.websocket.constant.WebSocketConstants.LOGIN_USER_KEY;

/**
 * WebSocket握手请求的拦截器
 *
 * @author zendwang
 */
@Slf4j
public class PlusWebSocketInterceptor implements HandshakeInterceptor {

    /**
     * WebSocket握手之前执行的前置处理方法
     *
     * @param request    WebSocket握手请求
     * @param response   WebSocket握手响应
     * @param wsHandler  WebSocket处理程序
     * @param attributes 与WebSocket会话关联的属性
     * @return 如果允许握手继续进行，则返回true；否则返回false
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            // 检查是否登录 是否有token
            LoginUser loginUser = LoginHelper.getLoginUser();

            // 解决 ws 不走 mvc 拦截器问题(cloud 版本不受影响)
            // 检查 header 与 param 里的 clientid 与 token 里的是否一致
            String headerCid = ServletUtils.getRequest().getHeader(LoginHelper.CLIENT_KEY);
            String paramCid = ServletUtils.getParameter(LoginHelper.CLIENT_KEY);
            String clientId = StpUtil.getExtra(LoginHelper.CLIENT_KEY).toString();
            if (!StringUtils.equalsAny(clientId, headerCid, paramCid)) {
                // token 无效
                throw NotLoginException.newInstance(StpUtil.getLoginType(),
                    "-100", "客户端ID与Token不匹配",
                    StpUtil.getTokenValue());
            }

            attributes.put(LOGIN_USER_KEY, loginUser);
            return true;
        } catch (NotLoginException e) {
            log.error("WebSocket 认证失败'{}',无法访问系统资源", e.getMessage());
            return false;
        }
    }

    /**
     * WebSocket握手成功后执行的后置处理方法
     *
     * @param request   WebSocket握手请求
     * @param response  WebSocket握手响应
     * @param wsHandler WebSocket处理程序
     * @param exception 握手过程中可能出现的异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 在这个方法中可以执行一些握手成功后的后续处理逻辑，比如记录日志或者其他操作
    }

}
