package org.ruoyi.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 图谱抽取结果
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GraphExtractionResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 抽取的实体列表
     */
    private List<ExtractedEntity> entities;

    /**
     * 抽取的关系列表
     */
    private List<ExtractedRelation> relations;

    /**
     * 原始LLM响应
     */
    private String rawResponse;

    /**
     * 消耗的token数
     */
    private Integer tokenUsed;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
