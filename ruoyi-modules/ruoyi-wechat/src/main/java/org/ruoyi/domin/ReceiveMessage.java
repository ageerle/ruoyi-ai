package org.ruoyi.domin;

import lombok.Data;

@Data
public class ReceiveMessage {
    /**
     * 消息ID 64位
     */
    String msgId;
    /**
     * 开发者微信号
     */
    private String toUserName;
    /**
     * 发送方账号(一个openid）
     */
    private String fromUserName;
    /**
     * 消息创建时间（整形）
     */
    private String createTime;
    /**
     * 消息类型
     */
    private String msgType;
    /**
     * 文本消息内容
     */
    private String content;
    /**
     * 消息的数据ID 消息来自文章才有
     */
    private String msgDataId;
    /**
     * 多图文时第几篇文章，从1开始 消息如果来自文章才有
     */
    private String idx;
    /**
     * 订阅事件 subscribe 订阅 unsbscribe 取消订阅
     */
    private String event;
    /**
     * 扫码 - ticket
     */
    private String ticket;

    public String getReplyTextMsg(String msg) {
        String xml = "<xml>\n"
                + "       <ToUserName><![CDATA[" + getFromUserName() + "]]></ToUserName>\n"
                + "       <FromUserName><![CDATA[" + getToUserName() + "]]></FromUserName>\n"
                + "       <CreateTime>" + System.currentTimeMillis() + "</CreateTime>\n"
                + "       <MsgType><![CDATA[text]]></MsgType>\n"
                + "       <Content><![CDATA[" + msg + "]]></Content>\n"
                + "     </xml>";
        return xml;
    }
}
