package org.ruoyi.domain.bo.knowledge;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.domain.entity.knowledge.KnowledgeInfo;

/**
 * 知识库业务对象 knowledge_info
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeInfo.class, reverseConvertGenerate = false)
public class KnowledgeInfoBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空", groups = { AddGroup.class, EditGroup.class })
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
     * 重排模型
     */
    private String rerankModel;

    /**
     * 是否启用重排（0 否 1 是）
     */
    private Integer enableRerank;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否启用混合检索（0 否 1 是）
     */
    private Integer enableHybrid;

    /**
     * 混合检索权重比例 (0.0-1.0)
     */
    private Double hybridAlpha;


}
