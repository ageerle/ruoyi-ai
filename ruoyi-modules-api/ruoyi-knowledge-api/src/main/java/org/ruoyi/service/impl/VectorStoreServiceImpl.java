package org.ruoyi.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.VectorStoreService;
import org.ruoyi.service.strategy.VectorStoreStrategy;
import org.ruoyi.service.strategy.VectorStoreStrategyFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量库服务实现
 *
 * @author ageer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final VectorStoreStrategyFactory strategyFactory;


    /**
     * 获取当前配置的向量库策略
     */
    private VectorStoreStrategy getCurrentStrategy() {
        return strategyFactory.getStrategy();
    }

    @Override
    public void createSchema(String vectorModelName, String kid, String modelName) {
        log.info("创建向量库schema: vectorModelName={}, kid={}, modelName={}", vectorModelName, kid, modelName);
        VectorStoreStrategy strategy = getCurrentStrategy();
        strategy.createSchema(vectorModelName, kid, modelName);
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        log.info("存储向量数据: kid={}, docId={}, 数据条数={}", 
                storeEmbeddingBo.getKid(), storeEmbeddingBo.getDocId(), storeEmbeddingBo.getChunkList().size());
        VectorStoreStrategy strategy = getCurrentStrategy();
        strategy.storeEmbeddings(storeEmbeddingBo);
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        log.info("查询向量数据: kid={}, query={}, maxResults={}", 
                queryVectorBo.getKid(), queryVectorBo.getQuery(), queryVectorBo.getMaxResults());
        VectorStoreStrategy strategy = getCurrentStrategy();
        return strategy.getQueryVector(queryVectorBo);
    }

    @Override
    public void removeById(String id, String modelName) {
        log.info("根据ID删除向量数据: id={}, modelName={}", id, modelName);
        VectorStoreStrategy strategy = getCurrentStrategy();
        strategy.removeById(id, modelName);
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        log.info("根据docId删除向量数据: docId={}, kid={}", docId, kid);
        VectorStoreStrategy strategy = getCurrentStrategy();
        strategy.removeByDocId(docId, kid);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        log.info("根据fid删除向量数据: fid={}, kid={}", fid, kid);
        VectorStoreStrategy strategy = getCurrentStrategy();
        strategy.removeByFid(fid, kid);
    }
}
