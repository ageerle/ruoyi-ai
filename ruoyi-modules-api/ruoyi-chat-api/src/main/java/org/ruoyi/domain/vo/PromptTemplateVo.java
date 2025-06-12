package org.ruoyi.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.PromptTemplate;

import java.io.Serializable;


/**
 * 提示词模板视图对象 prompt_template
 *
 * @author evo
 * @date 2025-06-12
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PromptTemplate.class)
public class PromptTemplateVo implements Serializable {

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 提示词模板名称
     */
    @ExcelProperty(value = "提示词模板名称")
    private String templateName;

    /**
     * 提示词模板内容
     */
    @ExcelProperty(value = "提示词模板内容")
    private String templateContent;

    /**
     * 提示词分类，knowledge 知识库类型，chat 对话类型，draw绘画类型 ...
     */
    @ExcelProperty(value = "提示词分类")
    private String category;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}