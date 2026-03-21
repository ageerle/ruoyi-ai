package org.ruoyi.workflow.common.enums;

/**
 * 节点扩展属性枚举
 *
 * @author AprilWind
 */
public interface NodeExtEnum {

    /**
     * 选项label
     *
     * @return 选项label
     */
    String getLabel();

    /**
     * 选项值
     *
     * @return 选项值
     */
    String getValue();

    /**
     * 是否默认选中
     *
     * @return 是否默认选中
     */
    boolean isSelected();

}

