package org.ruoyi.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * 保存向量所需参数
 * @author ageer
 */
@Data
public class StoreEmbeddingBo {

    /**
     * 切分文本块列表
     */
    private List<String> chunkList;

    /**
     * 知识库kid
     */
    private String kid;

    /**
     * 文档id
     */
    private String docId;

    /**
     * 知识块id列表
     */
    private List<String> fids;

    /**
     * 向量库模型名称
     */
    private String vectorModelName;

    /**
     * 向量化模型id
     */
    private Long embeddingModelId;

    /**
     * 向量化模型名称
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

}
