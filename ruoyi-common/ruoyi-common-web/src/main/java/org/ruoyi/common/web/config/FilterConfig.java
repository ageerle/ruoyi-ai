package org.ruoyi.common.web.config;

import jakarta.servlet.DispatcherType;
import org.ruoyi.common.web.config.properties.XssProperties;
import org.ruoyi.common.web.filter.RepeatableFilter;
import org.ruoyi.common.web.filter.XssFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Filter配置
 *
 * @author Lion Li
 */
@AutoConfiguration
@EnableConfigurationProperties(XssProperties.class)
public class FilterConfig {

    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    @FilterRegistration(
        name = "xssFilter",
        urlPatterns = "/*",
        order = FilterRegistrationBean.HIGHEST_PRECEDENCE + 1,
        dispatcherTypes = DispatcherType.REQUEST
    )
    public XssFilter xssFilter() {
        return new XssFilter();
    }

    @Bean
    @FilterRegistration(name = "repeatableFilter", urlPatterns = "/*")
    public RepeatableFilter repeatableFilter() {
        return new RepeatableFilter();
    }

}
