package com.xmzs.common.config;

/**
 * 支付配置信息
 *
 * @author Admin
 */
public class PayConfig {

    /**
     * 商户ID
     */
    public static String pid = "xxx";

    /**
     * 接口地址
     */
    public static String payUrl = "https://pay-cloud.vip/mapi.php";

    /**
     * 私钥
     */
    public static String key = "xxx";

    /**
     * 服务器异步通知地址
     */
    public static String notify_url = "https://www.pandarobot.chat/pay/returnUrl";

    /**
     * 页面跳转通知地址
     */
    public static String return_url = "https://www.pandarobot.chat/pay/notifyUrl";

    /**
     * 支付方式
     */
    public static String type = "wxpay";

    /**
     * 设备类型
     */
    public static String device = "pc";

    /**
     * 加密方式默认MD5
     */
    public static String sign_type = "MD5";

}
