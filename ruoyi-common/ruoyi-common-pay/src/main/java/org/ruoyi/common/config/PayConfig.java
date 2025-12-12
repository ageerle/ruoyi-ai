package org.ruoyi.common.config;

import lombok.Data;

/**
 * 支付配置信息
 *
 * @author Admin
 */
@Data
public class PayConfig {

    /**
     * 商户ID
     */
    private String pid;

    /**
     * 接口地址
     */
    private String payUrl;

    /**
     * 私钥
     */
    private String key;

    /**
     * 服务器异步通知地址
     */
    private String notify_url;

    /**
     * 页面跳转通知地址
     */
    private String return_url;

    /**
     * 支付方式
     */
    private String type;

    /**
     * 设备类型
     */
    private String device;

    /**
     * 加密方式默认MD5
     */
    private String sign_type;

}
