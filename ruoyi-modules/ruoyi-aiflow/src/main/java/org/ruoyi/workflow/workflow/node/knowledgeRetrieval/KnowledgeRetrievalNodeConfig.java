package org.ruoyi.workflow.workflow.node.knowledgeRetrieval;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库检索节点配置
 */
@EqualsAndHashCode
@Data
public class KnowledgeRetrievalNodeConfig {

    /**
     * 知识库UUID（主要字段）
     */
    @JsonProperty("knowledge_base_uuid")
    private String knowledgeBaseUuid;

    /**
     * 知识库ID（兼容字段）
     */
    @JsonProperty("knowledge_id")
    private String knowledgeId;

    /**
     * 获取知识库ID（优先使用knowledgeBaseUuid）
     */
    public String getKnowledgeId() {
        return knowledgeBaseUuid != null ? knowledgeBaseUuid : knowledgeId;
    }

    /**
     * 检索的最大结果数
     */
    @Min(1)
    @Max(100)
    @JsonProperty("top_k")
    private Integer topK = 5;

    /**
     * 检索的最大结果数（兼容字段，前端使用top_n）
     */
    @JsonProperty("top_n")
    private Integer topN;

    /**
     * 获取topK值（优先使用topN）
     */
    public Integer getTopK() {
        return topN != null ? topN : topK;
    }

    /**
     * 相似度阈值（0-1之间）
     */
    @Min(0)
    @Max(1)
    @JsonProperty("similarity_threshold")
    private Double similarityThreshold = 0.7;

    /**
     * 相似度阈值（兼容字段，前端使用score）
     */
    @JsonProperty("score")
    private Double score;

    /**
     * 获取相似度阈值（优先使用score）
     */
    public Double getSimilarityThreshold() {
        return score != null ? score : similarityThreshold;
    }

    /**
     * 历史检索模式，用于识别旧流程中的图谱检索配置
     */
    @JsonProperty("retrieval_mode")
    private String retrievalMode = "vector";

    /**
     * Embedding模型名称（用于向量检索）
     */
    @JsonProperty("embedding_model")
    private String embeddingModel;

    /**
     * 是否返回原文
     */
    @JsonProperty("return_source")
    private Boolean returnSource = true;

    /**
     * 是否开启严格模式
     * 开启后，当检索无结果时返回 defaultResponse；关闭则返回空字符串
     */
    @JsonProperty("is_strict")
    private Boolean isStrict = true;

    /**
     * 严格模式下的默认回复内容
     * 当检索无结果且严格模式开启时，返回此内容
     */
    @JsonProperty("default_response")
    private String defaultResponse;
}
