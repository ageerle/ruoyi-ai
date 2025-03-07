package org.ruoyi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 知识库对象 knowledge_info
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Data
@TableName("knowledge_info")
public class KnowledgeInfo implements Serializable {

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
     * 知识库名称
     */
    private String share;

    /**
     * 描述
     */
    private String description;


    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

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
    private Integer overlapChar;

    /**
     * 知识库中检索的条数
     */
    private Integer retrieveLimit;

    /**
     * 文本块大小
     */
    private Integer textBlockSize;

    /**
     * 向量库
     */
    private String vector;

    /**
     * 向量模型
     */
    private String vectorModel;
}
