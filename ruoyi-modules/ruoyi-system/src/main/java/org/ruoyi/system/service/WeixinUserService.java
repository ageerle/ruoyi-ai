package org.ruoyi.system.service;

/**
 * @Author 公众号：程序猿阿朗
 */
public interface WeixinUserService {

    void checkSignature(String signature, String timestamp, String nonce);

    String handleWeixinMsg(String body);

}
