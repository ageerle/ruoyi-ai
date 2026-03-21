package org.ruoyi.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识图谱实例对象 knowledge_graph_instance
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_graph_instance")
public class KnowledgeGraphInstance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 图谱UUID
     */
    private String graphUuid;

    /**
     * 关联knowledge_info.kid
     */
    private String knowledgeId;

    /**
     * 图谱名称
     */
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

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
