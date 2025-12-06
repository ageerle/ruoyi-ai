package org.ruoyi.common.web.config;


import org.ruoyi.common.web.interceptor.DemoModeInterceptor;
import org.ruoyi.common.web.interceptor.PlusWebInvokeTimeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class ResourcesConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private DemoModeInterceptor demoModeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局访问性能拦截
        registry.addInterceptor(new PlusWebInvokeTimeInterceptor());

        // 演示模式拦截器
        if (demoModeInterceptor != null) {
            registry.addInterceptor(demoModeInterceptor)
                    .addPathPatterns("/**")  // 拦截所有路径
                    .excludePathPatterns(
                            // 排除静态资源
                            "/css/**",
                            "/js/**",
                            "/images/**",
                            "/fonts/**",
                            "/favicon.ico",
                            // 排除错误页面
                            "/error",
                            // 排除API文档
                            "/*/api-docs/**",
                            "/swagger-ui/**",
                            "/webjars/**",
                            // 排除监控端点
                            "/actuator/**"
                    );
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 设置访问源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}
