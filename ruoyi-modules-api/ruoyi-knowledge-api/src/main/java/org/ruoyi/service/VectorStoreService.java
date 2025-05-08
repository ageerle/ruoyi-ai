package org.ruoyi.service;

import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;

import java.util.List;

/**
 * 向量库管理
 * @author ageer
 */
public interface VectorStoreService {

    void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo);

    void removeByDocId(String kid,String docId);

    void removeByKid(String kid);

    List<String> getQueryVector(QueryVectorBo queryVectorBo);

    void createSchema(String kid,String modelName);

    void removeByKidAndFid(String kid, String fid);

}
