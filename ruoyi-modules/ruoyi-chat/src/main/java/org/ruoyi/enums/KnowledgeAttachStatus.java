package org.ruoyi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库附件解析状态枚举
 *
 * @author RobustH
 */
@Getter
@AllArgsConstructor
public enum KnowledgeAttachStatus {

    /**
     * 待解析
     */
    WAITING(0, "待解析"),

    /**
     * 解析中
     */
    PARSING(1, "解析中"),

    /**
     * 已解析
     */
    COMPLETED(2, "已解析"),

    /**
     * 解析失败
     */
    FAILED(3, "解析失败");

    private final Integer code;
    private final String info;

}
