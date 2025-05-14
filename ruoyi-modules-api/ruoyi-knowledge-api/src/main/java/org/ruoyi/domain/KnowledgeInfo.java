package org.ruoyi.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识库对象 knowledge_info
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_info")
public class KnowledgeInfo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 知识库ID
     */
    private String kid;

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * 知识库名称
     */
    private String kname;

    /**
     * 是否公开知识库（0 否 1是）
     */
    private Integer share;

    /**
     * 描述
     */
    private String description;

    /**
     * 知识分隔符
     */
    private String knowledgeSeparator;

    /**
     * 提问分隔符
     */
    private String questionSeparator;

    /**
     * 重叠字符数
     */
    private Long overlapChar;

    /**
     * 知识库中检索的条数
     */
    private Long retrieveLimit;

    /**
     * 文本块大小
     */
    private Long textBlockSize;

    /**
     * 向量库模型名称
     */
    private String vectorModelName;

    /**
     * 向量化模型名称
     */
    private String embeddingModelName;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 备注
     */
    private String remark;


}
