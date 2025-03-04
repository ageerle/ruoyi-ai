package org.ruoyi.common.wechat.itchat4j.utils.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


public enum RetCodeEnum {

	UNKOWN("9999", "未知"),
	SUCCESS("0", "成功"),
	TICKET_ERROR("-14", "ticket错误"),
	PARAM_ERROR("1", "传入参数错误"),
	NOT_LOGIN_WARN("1100", "未登录提示"),
	NOT_LOGIN_CHECK("1101", "未检测到登录"),
	COOKIE_INVALID_ERROR("1102", "cookie值无效"),
	LOGIN_ENV_ERROR("1203", "当前登录环境异常，为了安全起见请不要在web端进行登录"),
	TOO_OFEN("1205", "操作频繁")
	;

	private static final Map<String, RetCodeEnum> lookup = new HashMap<String, RetCodeEnum>();
	static {
		for (RetCodeEnum s : EnumSet.allOf(RetCodeEnum.class)){
			lookup.put(s.getCode(), s);
		}
	}
	public static RetCodeEnum fromCode(String code) {
		return lookup.get(code);
	}

	private String code;
	private String type;

	RetCodeEnum(String code, String type) {
		this.code = code;
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public String getType() {
		return type;
	}



}
