package org.ruoyi.common.wechat.itchat4j.beans;

import org.ruoyi.common.wechat.itchat4j.utils.enums.SendMsgType;

import java.io.Serializable;

/**
 * 发送消息体
 * @author WesleyOne
 * @create 2019/1/7
 */
public class SendMsg implements Serializable {

    private String userName;
    private String nickName;
    private String message;
    private SendMsgType msgType;
    private boolean isGroup;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SendMsgType getMsgType() {
        return msgType;
    }

    public void setMsgType(SendMsgType msgType) {
        this.msgType = msgType;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }
}
