package org.ruoyi.common.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.ruoyi.common.web.utils.SafeRequestLogUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Web request timing interceptor. Request payloads, form fields and query strings are
 * deliberately outside this logging boundary.
 *
 * @author Lion Li
 * @since 3.3.0
 */
@Slf4j
public class PlusWebInvokeTimeInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<StopWatch> KEY_CACHE = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        StopWatch stopWatch = new StopWatch();
        KEY_CACHE.set(stopWatch);
        stopWatch.start();
        log.info("[PLUS] request_started method={} path={}", request.getMethod(), safePath(request));
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        // Completion metadata is emitted once from afterCompletion.
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        StopWatch stopWatch = KEY_CACHE.get();
        try {
            if (stopWatch == null) {
                return;
            }
            if (stopWatch.isStarted() && !stopWatch.isStopped()) {
                stopWatch.stop();
            }
            log.info("[PLUS] request_completed method={} path={} status={} elapsedMs={} outcome={} errorType={}",
                request.getMethod(), safePath(request), response.getStatus(),
                stopWatch.getDuration().toMillis(), ex == null ? "SUCCESS" : "FAILED",
                ex == null ? "none" : ex.getClass().getName());
        } finally {
            KEY_CACHE.remove();
        }
    }

    private static String safePath(HttpServletRequest request) {
        return SafeRequestLogUtils.sanitizeUri(request.getRequestURI());
    }
}
