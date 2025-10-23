package org.ruoyi.graph.enums;

import lombok.Getter;

/**
 * 构建任务状态枚举
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Getter
public enum TaskStatusEnum {

    /**
     * 待执行
     */
    PENDING(1, "待执行"),

    /**
     * 执行中
     */
    RUNNING(2, "执行中"),

    /**
     * 成功
     */
    SUCCESS(3, "成功"),

    /**
     * 失败
     */
    FAILED(4, "失败");

    private final Integer code;
    private final String description;

    TaskStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskStatusEnum getByCode(Integer code) {
        for (TaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
