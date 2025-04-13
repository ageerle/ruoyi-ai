package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.math.BigDecimal;

import java.io.Serial;

/**
 * 套餐管理对象 chat_package_plan
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_package_plan")
public class ChatPackagePlan extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 套餐名称
     */
    private String name;

    /**
     * 套餐价格
     */
    private BigDecimal price;

    /**
     * 有效时间
     */
    private Long duration;

    /**
     * 计划详情
     */
    private String planDetail;

    /**
     * 备注
     */
    private String remark;


}
