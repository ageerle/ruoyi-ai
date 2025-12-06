package org.ruoyi.graph.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 知识图谱实例对象 knowledge_graph_instance
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_graph_instance")
public class GraphInstance extends BaseEntity {

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
     * 图谱实例名称（前端使用，不映射到数据库）
     */
    @TableField(exist = false)
    private String instanceName;

    /**
     * 构建状态：10构建中、20已完成、30失败
     */
    private Integer graphStatus;

    /**
     * 节点数量
     */
    private Integer nodeCount;

    /**
     * 关系数量
     */
    private Integer relationshipCount;

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
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    /**
     * 获取实例名称（兼容前端）
     */
    public String getInstanceName() {
        return instanceName != null ? instanceName : graphName;
    }

    /**
     * 设置实例名称（同步到graphName）
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        this.graphName = instanceName;
    }
}
