package com.xmzs.common.wechat.utils.enums;

public enum RetCodeEnum {

	NORMAL("0", "普通"),
	LOGIN_OUT("1102", "退出"),
	LOGIN_OTHERWHERE("1101", "其它地方登陆"),
	MOBILE_LOGIN_OUT("1102", "移动端退出"),
	UNKOWN("9999", "未知")

	;


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
