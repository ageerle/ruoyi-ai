package com.xmzs.common.wechat.utils.enums;

/**
 * 消息类型
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月23日 下午12:15:00
 * @version 1.0
 *
 */
public enum MsgCodeEnum {

	// public static final int MSGTYPE_TEXT = 1; // 文本消息类型
	// public static final int MSGTYPE_IMAGE = 3; // 图片消息
	// public static final int MSGTYPE_VOICE = 34; // 语音消息
	// public static final int MSGTYPE_VIDEO = 43; // 小视频消息
	// public static final int MSGTYPE_MICROVIDEO = 62; // 短视频消息
	// public static final int MSGTYPE_EMOTICON = 47; // 表情消息
	// public static final int MSGTYPE_APP = 49;
	// public static final int MSGTYPE_VOIPMSG = 50;
	// public static final int MSGTYPE_VOIPNOTIFY = 52;
	// public static final int MSGTYPE_VOIPINVITE = 53;
	// public static final int MSGTYPE_LOCATION = 48;
	// public static final int MSGTYPE_STATUSNOTIFY = 51;
	// public static final int MSGTYPE_SYSNOTICE = 9999;
	// public static final int MSGTYPE_POSSIBLEFRIEND_MSG = 40;
	// public static final int MSGTYPE_VERIFYMSG = 37;
	// public static final int MSGTYPE_SHARECARD = 42;
	// public static final int MSGTYPE_SYS = 10000;
	// public static final int MSGTYPE_RECALLED = 10002;
	MSGTYPE_TEXT(1, "文本消息类型"),
	MSGTYPE_IMAGE(3, "图片消息"),
	MSGTYPE_VOICE(34, "语音消息"),
	MSGTYPE_VIDEO(43, "小视频消息"),
	MSGTYPE_MICROVIDEO(62, "短视频消息"),
	MSGTYPE_EMOTICON(47, "表情消息"),
	MSGTYPE_MEDIA(49, "多媒体消息"),
	MSGTYPE_VOIPMSG(50, ""),
	MSGTYPE_VOIPNOTIFY(52, ""),
	MSGTYPE_VOIPINVITE(53, ""),
	MSGTYPE_LOCATION(48, ""),
	MSGTYPE_STATUSNOTIFY(51, ""),
	MSGTYPE_SYSNOTICE(9999, ""),
	MSGTYPE_POSSIBLEFRIEND_MSG(40, ""),
	MSGTYPE_VERIFYMSG(37, "好友请求"),
	MSGTYPE_SHARECARD(42, ""),
	MSGTYPE_SYS(10000, "系统消息"),
	MSGTYPE_RECALLED(10002, "")

	;

	private int code;
	private String type;

	MsgCodeEnum(int code, String type) {
		this.code = code;
		this.type = type;
	}

	public int getCode() {
		return code;
	}

	public String getType() {
		return type;
	}

}
