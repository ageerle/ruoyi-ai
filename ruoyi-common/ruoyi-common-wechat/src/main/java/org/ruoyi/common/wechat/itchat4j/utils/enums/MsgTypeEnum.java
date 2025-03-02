package org.ruoyi.common.wechat.itchat4j.utils.enums;


/**
 * 消息类型枚举类
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年5月13日 下午11:53:00
 * @version 1.0
 *
 */
public enum MsgTypeEnum {
	TEXT("Text", "文本消息"),
	MAP("MAP", "地理位置"),
	PIC("Pic", "图片消息"),
	VOICE("Voice", "语音消息"),
	VIEDO("Viedo", "小视频消息"),
	NAMECARD("NameCard", "名片消息"),
	SYS("Sys", "系统消息"),
	VERIFYMSG("VerifyMsg", "添加好友"),

	// 地址分享
	MEDIA("app", "文件消息");

	private String type;
	private String code;

	MsgTypeEnum(String type, String code) {
		this.type = type;
		this.code = code;
	}

	public String getType() {
		return type;
	}

	public String getCode() {
		return code;
	}

}
