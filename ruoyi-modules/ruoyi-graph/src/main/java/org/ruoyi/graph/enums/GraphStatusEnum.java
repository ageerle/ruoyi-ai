package org.ruoyi.graph.enums;

import lombok.Getter;

/**
 * 图谱构建状态枚举
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Getter
public enum GraphStatusEnum {

    /**
     * 未构建
     */
    NOT_BUILT(0, "未构建", "NOT_BUILT"),

    /**
     * 构建中
     */
    BUILDING(10, "构建中", "BUILDING"),

    /**
     * 已完成
     */
    COMPLETED(20, "已完成", "COMPLETED"),

    /**
     * 失败
     */
    FAILED(30, "失败", "FAILED");

    private final Integer code;
    private final String description;
    private final String statusKey;

    GraphStatusEnum(Integer code, String description, String statusKey) {
        this.code = code;
        this.description = description;
        this.statusKey = statusKey;
    }

    /**
     * 根据code获取枚举
     */
    public static GraphStatusEnum getByCode(Integer code) {
        for (GraphStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据前端状态字符串获取状态码
     */
    public static Integer getCodeByStatusKey(String statusKey) {
        if (statusKey == null || statusKey.trim().isEmpty()) {
            return null;
        }
        for (GraphStatusEnum status : values()) {
            if (status.getStatusKey().equals(statusKey)) {
                return status.getCode();
            }
        }
        return null;
    }

    /**
     * 根据状态码获取前端状态字符串
     */
    public static String getStatusKeyByCode(Integer code) {
        GraphStatusEnum status = getByCode(code);
        return status != null ? status.getStatusKey() : "NOT_BUILT";
    }
}
