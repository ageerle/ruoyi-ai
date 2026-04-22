package org.ruoyi.domain.bo.vector;


import lombok.Data;

/**
 * 查询向量所需参数
 *
 * @author ageer
 */
@Data
public class QueryVectorBo {

    /**
     * 查询内容
     */
    private String query;

    /**
     * 知识库kid
     */
    private String kid;

    /**
     * 查询向量返回条数
     */
    private Integer maxResults;

    /**
     * 向量库模型名称
     */
    private String vectorModelName;

    /**
     * 向量化模型ID
     */
    private Long embeddingModelId;

    /**
     * 向量化模型ID
     */
    private String embeddingModelName;

    /**
     * 请求key
     */
    private String apiKey;

    /**
     * 请求地址
     */
    private String baseUrl;


    // ========== 重排序相关参数 ==========

    /**
     * 是否启用重排序
     * 默认为 false
     */
    private Boolean enableRerank = false;

    /**
     * 重排序模型名称
     */
    private String rerankModelName;

    /**
     * 重排序后返回的文档数量（topN）
     * 如果不指定，默认与 maxResults 相同
     */
    private Integer rerankTopN;

    /**
     * 重排序相关性分数阈值
     * 低于此阈值的文档将被过滤
     */
    private Double rerankScoreThreshold;

    // ========== 混合检索与阈值相关参数 ==========

    /**
     * 相似度阈值 (0.0-1.0)
     * 应用于向量搜索阶段
     */
    private Double similarityThreshold;

    /**
     * 是否启用混合检索
     */
    private Boolean enableHybrid = false;

    /**
     * 混合检索权重 (0.0-1.0)
     */
    private Double hybridAlpha;

}
