package org.ruoyi.common.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * traceId 串联过滤器（P0.7 可观测性门）。
 * <p>
 * 在请求线程入口生成或透传 traceId，写入 SLF4J MDC，使下游 service / mapper / advice
 * 日志与响应体可关联同一请求；chain.doFilter 后立即清理避免线程复用泄漏到下一请求。
 * <p>
 * 优先级：上游若已注入 {@code X-Trace-Id} 头（网关 / 边车 / 链路追踪系统）则优先透传，
 * 保持跨服务 traceId 一致；否则生成 32 位无连字符 UUID。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID_KEY, traceId);
        // 同时写回响应头，便于客户端在浏览器 Network 面板直接核对
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }
}
