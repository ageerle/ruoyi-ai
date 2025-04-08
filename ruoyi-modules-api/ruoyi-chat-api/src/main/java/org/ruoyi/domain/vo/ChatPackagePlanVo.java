package org.ruoyi.domain.vo;

import java.math.BigDecimal;
import org.ruoyi.system.domain.ChatPackagePlan;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;




/**
 * 套餐管理视图对象 chat_package_plan
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatPackagePlan.class)
public class ChatPackagePlanVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 套餐名称
     */
    @ExcelProperty(value = "套餐名称")
    private String name;

    /**
     * 套餐价格
     */
    @ExcelProperty(value = "套餐价格")
    private BigDecimal price;

    /**
     * 有效时间
     */
    @ExcelProperty(value = "有效时间")
    private Long duration;

    /**
     * 计划详情
     */
    @ExcelProperty(value = "计划详情")
    private String planDetail;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
