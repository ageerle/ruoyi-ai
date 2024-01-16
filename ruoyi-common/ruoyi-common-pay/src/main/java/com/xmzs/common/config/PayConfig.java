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
    public static String pid = "xx";

    /**
     * 支付方式
     */
    public static String type = "wxpay";

    /**
     * 接口地址
     */
    public static String payUrl = "https://pay.bluetuo.com/mapi.php";

    /**
     * 服务器异步通知地址
     */
    public static String notify_url = "http://xx/pay/returnUrl";

    /**
     * 页面跳转通知地址
     */
    public static String return_url = "http://xx/pay/notifyUrl";

    /**
     * 设备类型
     */
    public static String device = "pc";

    /**
     * 加密方式默认MD5
     */

    public static String sign_type = "MD5";

    /**
     * 私钥
     */
    public static String key = "xx";

}
