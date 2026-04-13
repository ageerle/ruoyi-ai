package org.ruoyi.common.web.config;

import org.ruoyi.common.web.aspectj.DemoAspect;
import org.ruoyi.common.web.config.properties.DemoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 演示模式自动配置
 *
 * @author ruoyi
 */
@Configuration
@EnableConfigurationProperties(DemoProperties.class)
public class DemoAutoConfiguration {

    /**
     * 注册演示模式切面
     */
    @Bean
    public DemoAspect demoAspect(DemoProperties demoProperties) {
        return new DemoAspect(demoProperties);
    }
}