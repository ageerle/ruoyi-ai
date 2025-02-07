package org.ruoyi.system.service.impl;

import org.ruoyi.system.domain.model.ReceiveMessage;
import org.ruoyi.system.service.WeixinUserService;
import org.ruoyi.system.util.WeixinMsgUtil;
import org.ruoyi.system.util.WeixinQrCodeCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
            return receiveMessage.getReplyTextMsg("欢迎关注【熊猫办公助手】,请访问https://web.pandarobot.chat/使用AI助手");
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
