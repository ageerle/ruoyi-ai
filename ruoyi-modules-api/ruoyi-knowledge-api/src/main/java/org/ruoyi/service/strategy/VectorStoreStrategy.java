package org.ruoyi.service.strategy;

import org.ruoyi.service.VectorStoreService;

/**
 * 向量库策略接口
 * 继承VectorStoreService以避免重复定义相同的方法
 * 
 * @author Yzm
 */
public interface VectorStoreStrategy extends VectorStoreService {
    
    /**
     * 获取向量库类型标识
     * @return 向量库类型（如：weaviate, milvus）
     */
    String getVectorStoreType();
}