package org.ruoyi.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 变量枚举
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum VariablesEnum implements NodeExtEnum {
    ;
    private final String label;
    private final String value;
    private final boolean selected;

}

