package org.ruoyi.common.config;

import org.ruoyi.common.core.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 支付配置信息
 *
 * @author Admin
 */
@RequiredArgsConstructor
@Configuration
@Slf4j
public class PayInit {

    private final ConfigService configService;

    private PayConfig payConfig;

    @Bean
    @Scope("singleton")
    public PayConfig payConfig() {
        if (payConfig == null) {
            payConfig = new PayConfig();
            updatePayConfig();
        }
        return payConfig;
    }

    @Scheduled(fixedDelay = 10000)  // 每10秒检查一次
    public void updatePayConfig() {
        payConfig.setType("wxpay");
        payConfig.setDevice("pc");
        payConfig.setSign_type("MD5");
        payConfig.setPid(getKey("pid"));
        payConfig.setKey(getKey("key"));
        payConfig.setPayUrl(getKey("payUrl"));
        payConfig.setNotify_url(getKey("notify_url"));
        payConfig.setReturn_url(getKey("return_url"));
    }

    public String getKey(String key){
        return configService.getConfigValue("pay", key);
    }

}
