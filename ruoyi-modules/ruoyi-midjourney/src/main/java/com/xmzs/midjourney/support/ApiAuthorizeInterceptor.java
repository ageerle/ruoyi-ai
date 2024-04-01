package com.xmzs.midjourney.support;

import com.xmzs.common.chat.constant.OpenAIConst;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.system.service.IChatService;
import com.xmzs.system.service.ISseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Objects;

@Component
public class ApiAuthorizeInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ApiAuthorizeInterceptor.class);
    private static final String API_SECRET_HEADER = "mj-api-secret";

    @Value("${chat.apiKey}")
    private String API_SECRET_VALUE;
    @Value("${chat.apiHost}")
    private String apiHost;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IChatService chatService;

    @Autowired
    private ISseService sseService;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        // 判断是否是MidJourney的请求
        if (isMidJourneyRequest(request)) {
            try {
                // 处理请求，例如费用扣除和任务查询
                processRequest(request);
                // 转发请求到目标服务器
                forwardRequest(request, response);
            } catch (Exception e) {
                // 记录错误日志，包括异常堆栈信息
                log.error("转发请求时发生错误", e);
                // 设置HTTP状态码和响应信息
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                try {
                    // 向客户端返回错误信息
                    response.getWriter().write("绘图失败！" + e.getMessage());
                } catch (Exception ex) {
                    log.error("设置错误响应时发生错误", ex);
                }
                // 中断请求处理流程
                return false;
            }
            // 中断正常的请求处理流程，因为请求已被转发
            return false;
        }
        // 如果不是MidJourney的请求，则继续正常处理
        return true;
    }

    private boolean isMidJourneyRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/mj") &&
            !uri.matches(".*/\\d+/fetch") &&
            !uri.matches("/mj/insight-face/swap") &&
            !uri.matches("/mj/submit/action");
    }

    private void processRequest(HttpServletRequest request) {
        // 处理付费用户的请求，包括费用扣除和任务查询
        sseService.checkUserGrade();
        String uri = request.getRequestURI();
        if (uri.matches("/mj/submit/describe") || uri.matches("/mj/submit/shorten")) {
            chatService.mjTaskDeduct(uri.endsWith("describe") ? "图生文" : "prompt分析", OpenAIConst.MJ_COST_TYPE2);
        } else if (uri.endsWith("image-seed") || uri.endsWith("list-by-condition")) {
            chatService.mjTaskDeduct(uri.endsWith("image-seed") ? "获取种子" : "任务查询", OpenAIConst.MJ_COST_TYPE3);
        } else if (uri.matches("/mj/submit/.*")) {
            chatService.mjTaskDeduct("文生图", OpenAIConst.MJ_COST_TYPE1);
        }
    }

    private void forwardRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String targetUrl = buildTargetUrl(request);
        HttpEntity<String> entity = new HttpEntity<>(readRequestBody(request), copyHeaders(request));
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(targetUrl, method, entity, byte[].class);
        copyResponseBack(response, responseEntity);
    }

    private String buildTargetUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        log.info("Forwarding URL: {}", uri);
        return apiHost + uri + (queryString != null ? "?" + queryString : "");
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(API_SECRET_HEADER, API_SECRET_VALUE);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase(API_SECRET_HEADER) &&
                !headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH) &&
                !headerName.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                headers.set(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    private String readRequestBody(HttpServletRequest request) throws Exception {
        if (request.getContentLengthLong() > 0) {
            try (InputStream inputStream = request.getInputStream()) {
                return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private void copyResponseBack(HttpServletResponse response, ResponseEntity<byte[]> responseEntity) throws Exception {
        HttpHeaders responseHeaders = responseEntity.getHeaders();
        responseHeaders.forEach((key, values) -> {
            if (!key.equalsIgnoreCase(API_SECRET_HEADER)) {
                response.addHeader(key, String.join(",", values));
            }
        });
        // 设置响应内容类型为UTF-8，防止乱码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        HttpStatus status = HttpStatus.resolve(responseEntity.getStatusCode().value());
        response.setStatus(Objects.requireNonNullElse(status, HttpStatus.INTERNAL_SERVER_ERROR).value());
        if (responseEntity.getBody() != null) {
            StreamUtils.copy(responseEntity.getBody(), response.getOutputStream());
        }
    }
}
