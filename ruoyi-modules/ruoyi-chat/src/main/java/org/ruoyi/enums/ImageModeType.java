package org.ruoyi.enums;

import lombok.Getter;

/**
 * 文生图模型分类
 *
 * @author Zengxb
 * @date 2026-02-14
 */
@Getter
public enum ImageModeType {

    TONGYI_WANX("Tongyiwanx", "万相");

    private final String code;
    private final String description;

    ImageModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
