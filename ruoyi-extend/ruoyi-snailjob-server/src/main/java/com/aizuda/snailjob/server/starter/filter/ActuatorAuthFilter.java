package com.aizuda.snailjob.server.starter.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ActuatorAuthFilter implements Filter {

    private final String username;
    private final String password;

    public ActuatorAuthFilter(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            // 如果没有提供 Authorization 或者格式不对，则返回 401
            response.setHeader("WWW-Authenticate", "Basic realm=\"realm\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        // 解码 Base64 编码的用户名和密码
        String base64Credentials = authHeader.substring("Basic ".length());
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);
        String[] split = credentials.split(":");
        if (split.length != 2) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"realm\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        // 验证用户名和密码
        if (!username.equals(split[0]) || !password.equals(split[1])) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"realm\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        // 如果认证成功，继续处理请求
        filterChain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}
