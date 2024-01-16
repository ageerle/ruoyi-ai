package com.xmzs.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态
 *
 * @author LionLi
 */
@Getter
@AllArgsConstructor
public enum TenantStatus {
    /**
     * 正常
     */
    OK("0", "正常"),
    /**
     * 停用
     */
    DISABLE("1", "停用"),
    /**
     * 删除
     */
    DELETED("2", "删除");

    private final String code;
    private final String info;

}
