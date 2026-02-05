package org.ruoyi.workflow.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.common.translation.annotation.Translation;
import org.ruoyi.workflow.common.constant.FlowConstant;
import org.ruoyi.workflow.domain.FlowCategory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 流程分类视图对象 wf_category
 *
 * @author may
 * @date 2023-06-27
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = FlowCategory.class)
public class FlowCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程分类ID
     */
    @ExcelProperty(value = "流程分类ID")
    private Long categoryId;

    /**
     * 父级分类id
     */
    private Long parentId;

    /**
     * 父级分类名称
     */
    @Translation(type = FlowConstant.CATEGORY_ID_TO_NAME, mapper = "parentId")
    private String parentName;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 流程分类名称
     */
    @ExcelProperty(value = "流程分类名称")
    private String categoryName;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = "显示顺序")
    private Long orderNum;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
