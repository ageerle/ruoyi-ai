package org.ruoyi.common.encrypt.filter;

import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.common.core.constant.HttpStatus;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.encrypt.annotation.ApiEncrypt;
import org.ruoyi.common.encrypt.properties.ApiDecryptProperties;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;


/**
 * Crypto 过滤器
 *
 * @author wdhcr
 */
public class CryptoFilter implements Filter {
    private final ApiDecryptProperties properties;

    public CryptoFilter(ApiDecryptProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        // 获取加密注解
        ApiEncrypt apiEncrypt = this.getApiEncryptAnnotation(servletRequest);
        boolean responseFlag = apiEncrypt != null && apiEncrypt.response();
        ServletRequest requestWrapper = null;
        ServletResponse responseWrapper = null;
        EncryptResponseBodyWrapper responseBodyWrapper = null;

        // 是否为 put 或者 post 请求
        if (HttpMethod.PUT.matches(servletRequest.getMethod()) || HttpMethod.POST.matches(servletRequest.getMethod())) {
            // 是否存在加密标头
            String headerValue = servletRequest.getHeader(properties.getHeaderFlag());
            if (StringUtils.isNotBlank(headerValue)) {
                // 请求解密
                requestWrapper = new DecryptRequestBodyWrapper(servletRequest, properties.getPrivateKey(), properties.getHeaderFlag());
            } else {
                // 是否有注解，有就报错，没有放行
                if (ObjectUtil.isNotNull(apiEncrypt)) {
                    HandlerExceptionResolver exceptionResolver = SpringUtils.getBean("handlerExceptionResolver", HandlerExceptionResolver.class);
                    exceptionResolver.resolveException(
                        servletRequest, servletResponse, null,
                        new ServiceException("没有访问权限，请联系管理员授权", HttpStatus.FORBIDDEN));
                    return;
                }
            }
        }

        // 判断是否响应加密
        if (responseFlag) {
            responseBodyWrapper = new EncryptResponseBodyWrapper(servletResponse);
            responseWrapper = responseBodyWrapper;
        }

        chain.doFilter(
            ObjectUtil.defaultIfNull(requestWrapper, request),
            ObjectUtil.defaultIfNull(responseWrapper, response));

        if (responseFlag) {
            servletResponse.reset();
            // 对原始内容加密
            String encryptContent = responseBodyWrapper.getEncryptContent(
                servletResponse, properties.getPublicKey(), properties.getHeaderFlag());
            // 对加密后的内容写出
            servletResponse.getWriter().write(encryptContent);
        }
    }

    /**
     * 获取 ApiEncrypt 注解
     */
    private ApiEncrypt getApiEncryptAnnotation(HttpServletRequest servletRequest) {
        RequestMappingHandlerMapping handlerMapping = SpringUtils.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        // 获取注解
        try {
            HandlerExecutionChain mappingHandler = handlerMapping.getHandler(servletRequest);
            if (ObjectUtil.isNotNull(mappingHandler)) {
                Object handler = mappingHandler.getHandler();
                if (ObjectUtil.isNotNull(handler)) {
                    // 从handler获取注解
                    if (handler instanceof HandlerMethod handlerMethod) {
                        return handlerMethod.getMethodAnnotation(ApiEncrypt.class);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public void destroy() {
    }
}
