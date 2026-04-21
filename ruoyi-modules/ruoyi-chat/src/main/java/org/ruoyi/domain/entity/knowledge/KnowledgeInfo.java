package org.ruoyi.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识库对象 knowledge_info
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_info")
public class KnowledgeInfo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 是否公开知识库（0 否 1是）
     */
    private Long share;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 知识分隔符
     */
    @TableField(value = "`separator`")
    private String separator;

    /**
     * 重叠字符数
     */
    private Long overlapChar;

    /**
     * 知识库中检索的条数
     */
    private Long retrieveLimit;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold;

    /**
     * 文本块大小
     */
    private Long textBlockSize;

    /**
     * 向量库
     */
    private String vectorModel;

    /**
     * 向量模型
     */
    private String embeddingModel;

    /**
     * 是否启用重排序（0 否 1是）
     */
    private Integer enableRerank;

    /**
     * 重排序模型名称
     */
    private String rerankModel;

    /**
     * 重排序后返回的文档数量
     */
    private Integer rerankTopN;

    /**
     * 重排序相关性分数阈值
     */
    private Double rerankScoreThreshold;

    /**
     * 是否启用混合检索（0 否 1是）
     */
    private Integer enableHybrid;

    /**
     * 混合检索权重 (0.0-1.0)
     */
    private Double hybridAlpha;

    /**
     * 备注
     */
    private String remark;


}
