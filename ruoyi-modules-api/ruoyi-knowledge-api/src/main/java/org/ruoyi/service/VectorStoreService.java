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

    List<String> getQueryVector(QueryVectorBo queryVectorBo);

    void createSchema(String kid,String modelName);

    void removeByKid(String kid,String modelName);

    void removeByDocId(String kid,String docId,String modelName);

    void removeByKidAndFid(String kid, String fid,String modelName);

}
