package org.ruoyi.service;

import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;

import java.util.List;

/**
 * 向量库管理
 * @author ageer
 */
public interface VectorStoreService {

    void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) throws ServiceException;

    List<String> getQueryVector(QueryVectorBo queryVectorBo);

    void createSchema(String kid, String embeddingModelName);

    void removeById(String id,String modelName) throws ServiceException;

    void removeByDocId(String docId, String kid) throws ServiceException;

    void removeByFid(String fid, String kid) throws ServiceException;
}
