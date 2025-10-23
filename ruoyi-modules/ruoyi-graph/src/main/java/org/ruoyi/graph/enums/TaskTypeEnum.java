package org.ruoyi.graph.enums;

import lombok.Getter;

/**
 * 构建任务类型枚举
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Getter
public enum TaskTypeEnum {

    /**
     * 全量构建
     */
    FULL_BUILD(1, "全量构建"),

    /**
     * 增量更新
     */
    INCREMENTAL_UPDATE(2, "增量更新"),

    /**
     * 重建
     */
    REBUILD(3, "重建");

    private final Integer code;
    private final String description;

    TaskTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     */
    public static TaskTypeEnum getByCode(Integer code) {
        for (TaskTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
