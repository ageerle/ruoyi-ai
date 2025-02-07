package org.ruoyi.common.sms.config;

import org.ruoyi.common.sms.config.properties.SmsProperties;
import org.ruoyi.common.sms.core.AliyunSmsTemplate;
import org.ruoyi.common.sms.core.SmsTemplate;
import org.ruoyi.common.sms.core.TencentSmsTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 短信配置类
 *
 * @author Lion Li
 * @version 4.2.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {

    @Configuration
    @ConditionalOnProperty(value = "sms.enabled", havingValue = "true")
    @ConditionalOnClass(com.aliyun.dysmsapi20170525.Client.class)
    static class AliyunSmsConfig {

        @Bean
        public SmsTemplate aliyunSmsTemplate(SmsProperties smsProperties) {
            return new AliyunSmsTemplate(smsProperties);
        }

    }

    @Configuration
    @ConditionalOnProperty(value = "sms.enabled", havingValue = "true")
    @ConditionalOnClass(com.tencentcloudapi.sms.v20190711.SmsClient.class)
    static class TencentSmsConfig {

        @Bean
        public SmsTemplate tencentSmsTemplate(SmsProperties smsProperties) {
            return new TencentSmsTemplate(smsProperties);
        }

    }

}
