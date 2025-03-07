package org.ruoyi.knowledge.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.knowledge.domain.KnowledgeInfo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库视图对象 knowledge_info
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = KnowledgeInfo.class)
public class KnowledgeInfoVo implements Serializable {

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
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long uid;

    /**
     * 知识库名称
     */
    @ExcelProperty(value = "知识库名称")
    private String kname;

    /**
     * 知识库名称
     */
    private String share;

    /**
     * 描述
     */
    @ExcelProperty(value = "描述")
    private String description;

    /**
     * 知识分隔符
     */
    @ExcelProperty(value = "知识分隔符")
    private String knowledgeSeparator;

    /**
     * 提问分隔符
     */
    @ExcelProperty(value = "提问分隔符")
    private String questionSeparator;

    /**
     * 重叠字符数
     */
    @ExcelProperty(value = "重叠字符数")
    private Integer overlapChar;

    /**
     * 知识库中检索的条数
     */
    @ExcelProperty(value = "知识库中检索的条数")
    private Integer retrieveLimit;

    /**
     * 文本块大小
     */
    @ExcelProperty(value = "文本块大小")
    private Integer textBlockSize;

    /**
     * 向量库
     */
    @ExcelProperty(value = "向量库")
    private String vector;

    /**
     * 向量模型
     */
    @ExcelProperty(value = "向量模型")
    private String vectorModel;
}
