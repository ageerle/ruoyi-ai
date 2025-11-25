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
     * 检索模式：vector（向量检索）、graph（图谱检索）、hybrid（混合检索）
     */
    @JsonProperty("retrieval_mode")
    private String retrievalMode = "vector";
    
    /**
     * 模型分类（用于LLM查询改写）
     */
    private String category;
    
    /**
     * LLM模型名称（用于查询改写）
     */
    @JsonProperty("model_name")
    private String modelName;
    
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
     * 自定义查询提示词（可选）
     * 用于对查询进行预处理或改写
     */
    private String prompt;
}
