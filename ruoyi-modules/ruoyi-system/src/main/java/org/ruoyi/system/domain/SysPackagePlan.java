package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 套餐管理对象 sys_package_plan
 *
 * @author Lion Li
 * @date 2024-05-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_package_plan")
public class SysPackagePlan extends BaseEntity {

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
