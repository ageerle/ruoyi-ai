package org.ruoyi.factory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.service.vector.impl.MilvusVectorStoreStrategy;
import org.ruoyi.service.vector.impl.QdrantVectorStoreStrategy;
import org.ruoyi.service.vector.impl.WeaviateVectorStoreStrategy;
import org.springframework.stereotype.Component;
import org.ruoyi.common.core.exception.ServiceException;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量库策略工厂
 * 根据配置动态选择向量库实现
 *
 * @author Yzm
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreStrategyFactory {

    private final VectorStoreProperties vectorStoreProperties;
    private final WeaviateVectorStoreStrategy weaviateStrategy;
    private final MilvusVectorStoreStrategy milvusStrategy;
    private final QdrantVectorStoreStrategy qdrantStrategy;

    private Map<String, VectorStoreService> strategies;

    @PostConstruct
    public void init() {
        strategies = new HashMap<>();
        strategies.put("weaviate", weaviateStrategy);
        strategies.put("milvus", milvusStrategy);
        strategies.put("qdrant", qdrantStrategy);
        log.info("向量库策略工厂初始化完成，支持的策略: {}", strategies.keySet());
    }

    /**
     * 获取当前配置的向量库策略
     */
    public VectorStoreService getStrategy() {
        return getStrategy(null);
    }

    public VectorStoreService getStrategy(String requestedType) {
        String vectorStoreType = requestedType;
        if (vectorStoreType == null || vectorStoreType.trim().isEmpty()) {
            vectorStoreType = vectorStoreProperties.getType();
        }
        if (vectorStoreType == null || vectorStoreType.trim().isEmpty()) {
            vectorStoreType = "weaviate"; // 默认使用weaviate
        }
        VectorStoreService strategy = strategies.get(vectorStoreType.toLowerCase());
        if (strategy == null) {
            throw new ServiceException("不支持的向量库类型: " + vectorStoreType);
        }
        log.debug("使用向量库策略: {}", vectorStoreType);
        return strategy;
    }

}
