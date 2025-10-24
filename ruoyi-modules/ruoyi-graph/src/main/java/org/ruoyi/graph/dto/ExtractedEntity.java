package org.ruoyi.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 抽取的实体
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtractedEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实体名称
     */
    private String name;

    /**
     * 实体类型
     */
    private String type;

    /**
     * 实体描述
     */
    private String description;

    /**
     * 置信度（0.0-1.0）
     */
    private Double confidence;
}
