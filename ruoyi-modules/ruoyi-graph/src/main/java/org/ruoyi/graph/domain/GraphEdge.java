package org.ruoyi.graph.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 图关系实体
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("graph_edge")
public class GraphEdge implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 图谱UUID
     */
    private String graphUuid;

    /**
     * 关系唯一标识（Neo4j中的关系ID）
     */
    private String edgeId;

    /**
     * 关系标签（类型）
     */
    private String label;

    /**
     * 源节点ID
     */
    private String sourceNodeId;

    /**
     * 源节点名称
     */
    private String sourceName;

    /**
     * 目标节点ID
     */
    private String targetNodeId;

    /**
     * 目标节点名称
     */
    private String targetName;

    /**
     * 关系类型编码
     */
    private String relationType;

    /**
     * 关系描述
     */
    private String description;

    /**
     * 关系权重（0.0-1.0）
     */
    private Double weight;

    /**
     * 置信度（0.0-1.0）
     */
    private Double confidence;

    /**
     * 来源知识库ID
     */
    private String knowledgeId;

    /**
     * 来源文档ID列表（JSON格式）
     */
    private String docIds;

    /**
     * 来源片段ID列表（JSON格式）
     */
    private String fragmentIds;

    /**
     * 文本段ID（关联到具体的文本段）
     */
    @JsonProperty("text_segment_id")
    private String textSegmentId;

    /**
     * 其他属性（JSON格式）
     */
    private String properties;

    /**
     * 元数据（JSON格式）
     */
    private Map<String, Object> metadata;

    /**
     * 源节点元数据
     */
    private Map<String, Object> sourceMetadata;

    /**
     * 目标节点元数据
     */
    private Map<String, Object> targetMetadata;

    /**
     * 备注
     */
    private String remark;
}
