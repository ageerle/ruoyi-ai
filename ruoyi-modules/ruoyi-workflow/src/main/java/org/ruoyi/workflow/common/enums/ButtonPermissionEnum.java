package org.ruoyi.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 按钮权限枚举
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum ButtonPermissionEnum implements NodeExtEnum {

    /**
     * 是否弹窗选人
     */
    POP("是否弹窗选人", "pop", false),

    /**
     * 是否能委托
     */
    TRUST("是否能委托", "trust", false),

    /**
     * 是否能转办
     */
    TRANSFER("是否能转办", "transfer", false),

    /**
     * 是否能抄送
     */
    COPY("是否能抄送", "copy", true),

    /**
     * 是否显示退回
     */
    BACK("是否显示退回", "back", true),

    /**
     * 是否能加签
     */
    ADD_SIGN("是否能加签", "addSign", false),

    /**
     * 是否能减签
     */
    SUB_SIGN("是否能减签", "subSign", false),

    /**
     * 是否能终止
     */
    TERMINATION("是否能终止", "termination", true),

    /**
     * 是否能上传附件
     */
    FILE("是否能上传附件", "file", true);

    private final String label;
    private final String value;
    private final boolean selected;

}

