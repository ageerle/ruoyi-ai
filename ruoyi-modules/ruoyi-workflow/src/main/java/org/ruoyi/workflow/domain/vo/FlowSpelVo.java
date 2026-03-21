package org.ruoyi.workflow.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import org.ruoyi.common.excel.annotation.ExcelDictFormat;
import org.ruoyi.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.workflow.domain.FlowSpel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 流程spel达式定义视图对象 flow_spel
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FlowSpel.class)
public class FlowSpelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelProperty(value = "主键id")
    private Long id;

    /**
     * 组件名称
     */
    @ExcelProperty(value = "组件名称")
    private String componentName;

    /**
     * 方法名
     */
    @ExcelProperty(value = "方法名")
    private String methodName;

    /**
     * 参数
     */
    @ExcelProperty(value = "参数")
    private String methodParams;

    /**
     * 预览spel值
     */
    @ExcelProperty(value = "预览spel值")
    private String viewSpel;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
