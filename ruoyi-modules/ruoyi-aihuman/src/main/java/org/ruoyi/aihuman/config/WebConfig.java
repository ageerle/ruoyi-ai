package org.ruoyi.aihuman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射/voice/**路径到classpath:/voice/目录
        registry.addResourceHandler("/voice/**")
                .addResourceLocations("classpath:/voice/")
                .setCachePeriod(3600);
    }
}