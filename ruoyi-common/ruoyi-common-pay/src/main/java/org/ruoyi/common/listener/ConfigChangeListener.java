package org.ruoyi.common.listener;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import org.ruoyi.common.core.event.ConfigChangeEvent;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 描述：创建一个监听器，用于处理配置变化事件
 *
 * @author ageerle@163.com
 * date 2024/5/19
 */
@Component
public class ConfigChangeListener implements ApplicationListener<ConfigChangeEvent> {
    private final WxPayService wxPayService;
    private final ConfigService configService;

    public ConfigChangeListener(WxPayService wxPayService, ConfigService configService) {
        this.wxPayService = wxPayService;
        this.configService = configService;
    }

    @Override
    public void onApplicationEvent(@NotNull ConfigChangeEvent event) {
        WxPayConfig newConfig = new WxPayConfig();
        newConfig.setAppId(StringUtils.trimToNull(configService.getConfigValue("weixin", "appId")));
        newConfig.setMchId(StringUtils.trimToNull(configService.getConfigValue("weixin", "mchId")));
        newConfig.setMchKey(StringUtils.trimToNull(configService.getConfigValue("weixin", "mchKey")));
        newConfig.setUseSandboxEnv(false);
        wxPayService.setConfig(newConfig);
    }
}
