package org.ruoyi.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.ruoyi.domin.ReceiveMessage;
import org.ruoyi.service.WeixinUserService;
import org.ruoyi.util.WeixinMsgUtil;
import org.ruoyi.util.WeixinQrCodeCacheUtil;
import org.springframework.stereotype.Service;

import java.util.Arrays;


@Slf4j
@Service
public class WeixinUserServiceImpl implements WeixinUserService {

    private String token = "panda";

    @Override
    public void checkSignature(String signature, String timestamp, String nonce) {
        String[] arr = new String[] {token, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder content = new StringBuilder();
        for (String str : arr) {
            content.append(str);
        }
        String tmpStr = DigestUtils.sha1Hex(content.toString());
        if (tmpStr.equals(signature)) {
            log.info("check success");
            return;
        }
        log.error("check fail");
        throw new RuntimeException("check fail");
    }

    @Override
    public String handleWeixinMsg(String requestBody) {
        ReceiveMessage receiveMessage = WeixinMsgUtil.msgToReceiveMessage(requestBody);
        // 扫码登录
        if (WeixinMsgUtil.isScanQrCode(receiveMessage)) {
            return handleScanLogin(receiveMessage);
        }
        // 关注
        if (WeixinMsgUtil.isEventAndSubscribe(receiveMessage)) {
            return receiveMessage.getReplyTextMsg("感谢您的关注！");
        }
        return receiveMessage.getReplyTextMsg("收到（自动回复）");
    }

    /**
     * 处理扫码登录
     *
     * @param receiveMessage
     * @return
     */
    private String handleScanLogin(ReceiveMessage receiveMessage) {
        String qrCodeTicket = WeixinMsgUtil.getQrCodeTicket(receiveMessage);
        if (WeixinQrCodeCacheUtil.get(qrCodeTicket) == null) {
            String openId = receiveMessage.getFromUserName();
            WeixinQrCodeCacheUtil.put(qrCodeTicket, openId);
        }
        return receiveMessage.getReplyTextMsg("你已成功登录！");
    }
}
