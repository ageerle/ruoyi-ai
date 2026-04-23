package org.ruoyi.domain.bo.knowledge;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;

/**
 * 知识片段业务对象 knowledge_fragment
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeFragment.class, reverseConvertGenerate = false)
public class KnowledgeFragmentBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 文档ID-用于关联文本块信息
     */
    private String docId;


    /**
     * 片段索引下标
     */
    @NotNull(message = "片段索引下标不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer idx;

    /**
     * 文档内容
     */
    @NotBlank(message = "文档内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;

    /**
     * 备注
     */
    private String remark;

    /**
     * 知识库ID
     */
    private Long knowledgeId;

    /**
     * 检索内容
     */
    private String query;

    /**
     * 返回条数
     */
    private Integer topK;

    /**
     * 相似度阈值
     */
    private Double threshold;

    /**
     * 是否启用重排
     */
    private Boolean enableRerank;

    /**
     * 重排模型名称
     */
    private String rerankModel;

    /**
     * 是否启用混合检索
     */
    private Boolean enableHybrid;

    /**
     * 混合检索权重 (0.0-1.0)
     */
    private Double hybridAlpha;

}
