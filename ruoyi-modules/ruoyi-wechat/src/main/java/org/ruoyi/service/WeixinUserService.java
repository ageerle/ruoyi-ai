package org.ruoyi.service;


public interface WeixinUserService {

    void checkSignature(String signature, String timestamp, String nonce);

    String handleWeixinMsg(String body);

}
