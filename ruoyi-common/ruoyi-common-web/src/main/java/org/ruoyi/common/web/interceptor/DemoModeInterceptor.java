package org.ruoyi.common.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.config.RuoYiConfig;
import org.ruoyi.common.core.exception.DemoModeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 演示模式拦截器
 * 全局拦截所有编辑操作（POST、PUT、DELETE、PATCH）
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class DemoModeInterceptor implements HandlerInterceptor {

    @Autowired
    private RuoYiConfig ruoYiConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果演示模式未开启，直接放行
        if (!ruoYiConfig.isDemoEnabled()) {
            return true;
        }

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        // 拦截所有编辑操作
        if (isEditOperation(method)) {
            // 排除一些特殊的只读操作
            if (isExcludedPath(requestURI)) {
                return true;
            }

            log.warn("演示模式拦截: {} {}", method, requestURI);
            throw new DemoModeException();
        }

        return true;
    }

    /**
     * 判断是否为编辑操作
     */
    private boolean isEditOperation(String method) {
        return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "DELETE".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method);
    }

    /**
     * 判断是否为排除的路径（这些路径即使是POST等方法也不拦截）
     */
    private boolean isExcludedPath(String requestURI) {
        // 排除登录相关
        if (requestURI.contains("/auth/login") || requestURI.contains("/auth/logout")) {
            return true;
        }

        // 排除导出操作（虽然是POST但是只读操作）
        if (requestURI.contains("/export")) {
            return true;
        }

        // 排除查询操作（一些复杂查询使用POST）
        if (requestURI.contains("/list") || requestURI.contains("/query") || requestURI.contains("/search")) {
            return true;
        }

        // 排除聊天接口（核心功能）
        if (requestURI.contains("/chat/send") || requestURI.contains("/chat/upload")) {
            return true;
        }

        // 排除文件上传预览等只读操作
        if (requestURI.contains("/upload") || requestURI.contains("/preview")) {
            return true;
        }

        // 排除支付回调
        if (requestURI.contains("/pay/returnUrl") || requestURI.contains("/pay/notifyUrl")) {
            return true;
        }

        // 排除重置密码（用户自己的操作）
        return requestURI.contains("/auth/reset/password");
    }
}
