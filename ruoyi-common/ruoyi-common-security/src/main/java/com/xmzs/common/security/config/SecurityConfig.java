package com.xmzs.common.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.xmzs.common.core.utils.SpringUtils;
import com.xmzs.common.security.config.properties.SecurityProperties;
import com.xmzs.common.security.handler.AllUrlHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 权限安全配置
 *
 * @author Lion Li
 */

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    /**
     * 注册sa-token的拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册路由拦截器，自定义验证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
            AllUrlHandler allUrlHandler = SpringUtils.getBean(AllUrlHandler.class);
            // 登录验证 -- 排除多个路径
            SaRouter
                // 获取所有的
                .match(allUrlHandler.getUrls())
                // 对未排除的路径进行检查
                .check(() -> {
                    // 检查是否登录 是否有token
                    StpUtil.checkLogin();

                    // 有效率影响 用于临时测试
                    // if (log.isDebugEnabled()) {
                    //     log.debug("剩余有效时间: {}", StpUtil.getTokenTimeout());
                    //     log.debug("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
                    // }

                });
        })).addPathPatterns("/**")
            // 排除不需要拦截的路径
            .excludePathPatterns(securityProperties.getExcludes());
    }

}
