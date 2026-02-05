package org.ruoyi.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum UserStatusEnum implements BaseEnum {

    WAIT_CONFIRM(1, "待验证"),
    NORMAL(2, "正常"),
    FREEZE(3, "冻结");

    private final Integer value;
    private final String desc;

    public static UserStatusEnum getByValue(Integer val) {
        return Arrays.stream(UserStatusEnum.values()).filter(item -> item.value.equals(val)).findFirst().orElse(null);
    }

}
