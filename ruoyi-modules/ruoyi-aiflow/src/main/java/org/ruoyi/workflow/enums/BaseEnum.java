package org.ruoyi.workflow.enums;

import com.baomidou.mybatisplus.annotation.IEnum;

public interface BaseEnum extends IEnum<Integer> {
    /**
     * 获取对应名称
     *
     * @return String
     */
    String getDesc();
}
