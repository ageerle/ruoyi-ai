package org.ruoyi.domain.vo;

import org.ruoyi.domain.KnowledgeAttach;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;




/**
 * 知识库附件视图对象 knowledge_attach
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeAttach.class)
public class KnowledgeAttachVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @ExcelProperty(value = "")
    private Long id;

    /**
     * 知识库ID
     */
    @ExcelProperty(value = "知识库ID")
    private String kid;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private String docId;

    /**
     * 文档名称
     */
    @ExcelProperty(value = "文档名称")
    private String docName;

    /**
     * 文档类型
     */
    @ExcelProperty(value = "文档类型")
    private String docType;

    /**
     * 文档内容
     */
    @ExcelProperty(value = "文档内容")
    private String content;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
