package org.ruoyi.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 抽取的关系
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedRelation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 源实体名称
     */
    private String sourceEntity;

    /**
     * 目标实体名称
     */
    private String targetEntity;

    /**
     * 关系描述
     */
    private String description;

    /**
     * 关系强度（0-10）
     */
    private Integer strength;

    /**
     * 置信度（0.0-1.0）
     */
    private Double confidence;
}
