package com.xmzs.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游客登录类型
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum LoginUserType {

    PC("1", "PC端用户"),

    XCX("2", "小程序用户");

    private final String code;
    private final String content;
}
