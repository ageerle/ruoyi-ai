package org.ruoyi.common.wechat.itchat4j.utils.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息类型
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月23日 下午12:15:00
 * @version 1.0
 *
 */
public enum MsgCodeEnum {

	MSGTYPE_TEXT(1, "文本消息"),
	MSGTYPE_IMAGE(3, "图片消息"),
	MSGTYPE_VOICE(34, "语音消息"),
	MSGTYPE_VERIFYMSG(37, "好友请求"),
	MSGTYPE_POSSIBLEFRIEND_MSG(40, "POSSIBLEFRIEND_MSG"),
	MSGTYPE_SHARECARD(42, "分享名片"),
	MSGTYPE_VIDEO(43, "视频消息"),
	MSGTYPE_EMOTICON(47, "表情消息"),
	MSGTYPE_LOCATION(48, "位置消息"),
	MSGTYPE_MEDIA(49, "分享链接"),
	MSGTYPE_VOIPMSG(50, "VOIPMSG"),
	MSGTYPE_STATUSNOTIFY(51, "状态通知"),
	MSGTYPE_VOIPNOTIFY(52, "VOIPNOTIFY"),
	MSGTYPE_VOIPINVITE(53, "VOIPINVITE"),
	MSGTYPE_MICROVIDEO(62, "短视频消息"),
	MSGTYPE_SYSNOTICE(9999, "SYSNOTICE"),
	MSGTYPE_SYS(10000, "系统消息"),
	MSGTYPE_RECALLED(10002, "撤回消息")

	;

	private static final Map<Integer, MsgCodeEnum> lookup = new HashMap<>();
	static {
		for (MsgCodeEnum s : EnumSet.allOf(MsgCodeEnum.class)){
			lookup.put(s.getCode(), s);
		}
	}
	public static MsgCodeEnum fromCode(int code) {
		return lookup.get(code);
	}

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
