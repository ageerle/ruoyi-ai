package org.ruoyi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色枚举
 *
 * @author ageerle@163.com
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum RoleType {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    FUNCTION("function"),
    TOOL("tool"),
    ;

    private final String name;

}
