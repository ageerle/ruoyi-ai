package org.ruoyi.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.domin.ReceiveMessage;

/**
 * @author https://www.wdbyte.com
 */
public class WeixinMsgUtil {

    // 事件-关注
    private static String EVENT_SUBSCRIBE = "subscribe";

    /**
     * 微信消息转对象
     *
     * @param xml
     * @return
     */
    public static ReceiveMessage msgToReceiveMessage(String xml) {
        JSONObject jsonObject = JSON.parseObject(XmlUtil.xml2json(xml));
        ReceiveMessage receiveMessage = new ReceiveMessage();
        receiveMessage.setToUserName(jsonObject.getString("ToUserName"));
        receiveMessage.setFromUserName(jsonObject.getString("FromUserName"));
        receiveMessage.setCreateTime(jsonObject.getString("CreateTime"));
        receiveMessage.setMsgType(jsonObject.getString("MsgType"));
        receiveMessage.setContent(jsonObject.getString("Content"));
        receiveMessage.setMsgId(jsonObject.getString("MsgId"));
        receiveMessage.setEvent(jsonObject.getString("Event"));
        receiveMessage.setTicket(jsonObject.getString("Ticket"));
        return receiveMessage;
    }

    /**
     * 是否是订阅事件
     *
     * @param receiveMessage
     * @return
     */
    public static boolean isEventAndSubscribe(ReceiveMessage receiveMessage) {
        return StringUtils.equals(receiveMessage.getEvent(), EVENT_SUBSCRIBE);
    }

    /**
     * 是否是二维码扫描事件
     *
     * @param receiveMessage
     * @return
     */
    public static boolean isScanQrCode(ReceiveMessage receiveMessage) {
        return StringUtils.isNotEmpty(receiveMessage.getTicket());
    }

    /**
     * 获取扫描的二维码 Ticket
     *
     * @param receiveMessage
     * @return
     */
    public static String getQrCodeTicket(ReceiveMessage receiveMessage) {
        return receiveMessage.getTicket();
    }

}
