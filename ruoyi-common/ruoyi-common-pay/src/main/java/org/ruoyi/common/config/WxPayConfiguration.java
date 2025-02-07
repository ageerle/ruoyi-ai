package org.ruoyi.common.config;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;

import org.ruoyi.common.core.service.ConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Binary Wang
 */
@Configuration
@RequiredArgsConstructor
public class WxPayConfiguration {

    private final ConfigService configService;

    private WxPayService wxPayService;

    @PostConstruct
    public void init() {
        WxPayConfig payConfig = new WxPayConfig();
        payConfig.setAppId(StringUtils.trimToNull(getKey("appId")));
        payConfig.setMchId(StringUtils.trimToNull(getKey("mchId")));
        payConfig.setMchKey(StringUtils.trimToNull(getKey("appSecret")));
        payConfig.setUseSandboxEnv(false);
        wxPayService = new WxPayServiceImpl();
        wxPayService.setConfig(payConfig);
    }

    @Bean
    @ConditionalOnMissingBean
    public WxPayService wxService() {
        return wxPayService;
    }

    public String getKey(String key) {
        return configService.getConfigValue("weixin", key);
    }

}
