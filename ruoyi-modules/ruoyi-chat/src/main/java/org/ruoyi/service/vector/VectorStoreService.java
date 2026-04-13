package org.ruoyi.service.vector;

import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;

import java.util.List;

/**
 * 向量库管理
 *
 * @author ageer
 */
public interface VectorStoreService {

    void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) throws ServiceException;

    List<String> getQueryVector(QueryVectorBo queryVectorBo);

    /**
     * 带分数及元数据的检索（用于测试检索功能）
     */
    List<KnowledgeRetrievalVo> search(QueryVectorBo queryVectorBo);

    void createSchema(String kid, String embeddingModelName);

    void removeById(String id, String modelName) throws ServiceException;

    void removeByDocId(String docId, String kid) throws ServiceException;

    void removeByFid(String fid, String kid) throws ServiceException;
}
