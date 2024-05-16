package com.xmzs.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.xmzs.system.domain.SysModel;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 系统模型视图对象 sys_model
 *
 * @author Lion Li
 * @date 2024-04-04
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysModel.class)
public class SysModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 模型名称
     */
    @ExcelProperty(value = "模型名称")
    private String modelName;



    /**
     * 模型描述
     */
    @ExcelProperty(value = "模型描述")
    private String modelDescribe;

    /**
     * 模型价格
     */
    @ExcelProperty(value = "模型价格")
    private double modelPrice;

    /**
     * 计费类型
     */
    @ExcelProperty(value = "计费类型")
    private String modelType;

    /**
     * 是否显示
     */
    private String modelShow;


    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}
