package org.ruoyi.domain.bo.knowledge;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import org.ruoyi.domain.entity.knowledge.KnowledgeGraphInstance;

/**
 * 知识图谱实例业务对象 knowledge_graph_instance
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = KnowledgeGraphInstance.class, reverseConvertGenerate = false)
public class KnowledgeGraphInstanceBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 图谱UUID
     */
    @NotBlank(message = "图谱UUID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String graphUuid;

    /**
     * 关联knowledge_info.kid
     */
    @NotBlank(message = "关联knowledge_info.kid不能为空", groups = { AddGroup.class, EditGroup.class })
    private String knowledgeId;

    /**
     * 图谱名称
     */
    @NotBlank(message = "图谱名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String graphName;

    /**
     * 构建状态：10构建中、20已完成、30失败
     */
    private Long graphStatus;

    /**
     * 节点数量
     */
    private Long nodeCount;

    /**
     * 关系数量
     */
    private Long relationshipCount;

    /**
     * 图谱配置(JSON格式)
     */
    private String config;

    /**
     * LLM模型名称
     */
    private String modelName;

    /**
     * 实体类型（逗号分隔）
     */
    private String entityTypes;

    /**
     * 关系类型（逗号分隔）
     */
    private String relationTypes;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 备注
     */
    private String remark;


}
