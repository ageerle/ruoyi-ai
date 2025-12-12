package org.ruoyi.workflow.workflow.node.keywordExtractor;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关键词提取节点配置
 */
@EqualsAndHashCode
@Data
public class KeywordExtractorNodeConfig {

    /**
     * 模型分类（如：llm, embedding 等）
     */
    private String category;

    /**
     * 模型名称
     */
    @NotNull
    @JsonProperty("model_name")
    private String modelName;

    /**
     * 提取的关键词数量
     */
    @Min(1)
    @Max(50)
    @JsonProperty("top_n")
    private Integer topN = 5;

    /**
     * 提示词（可选）
     * 用于指导关键词提取的额外说明
     */
    private String prompt;
}
