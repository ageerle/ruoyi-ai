package org.ruoyi.domain.vo.knowledge;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;

import java.io.Serial;
import java.io.Serializable;



/**
 * 知识库附件视图对象 knowledge_attach
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeAttach.class)
public class KnowledgeAttachVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 知识库ID
     */
    @ExcelProperty(value = "知识库ID")
    private Long knowledgeId;

    /**
     * 文档ID-用于关联文本块信息
     */
    @ExcelProperty(value = "文档ID")
    private String docId;

    /**
     * 附件名称
     */
    @ExcelProperty(value = "附件名称")
    private String name;

    /**
     * 附件类型
     */
    @ExcelProperty(value = "附件类型")
    private String type;

    /**
     * 对象存储ID
     */
    @ExcelProperty(value = "对象存储ID")
    private Long ossId;


    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
