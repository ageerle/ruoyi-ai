package org.ruoyi.service.vector.impl;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.service.vector.VectorStoreService;

/**
 * 向量库策略抽象基类
 * 提供公共的方法实现，如embedding模型获取等
 *
 * @author Yzm
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractVectorStoreStrategy implements VectorStoreService {

    protected final VectorStoreProperties vectorStoreProperties;

    private final EmbeddingModelFactory embeddingModelFactory;

    protected final IChatModelService chatModelService;


    /**
     * 将float数组转换为Float对象数组
     */
    protected static Float[] toObjectArray(float[] primitive) {
        Float[] result = new Float[primitive.length];
        for (int i = 0; i < primitive.length; i++) {
            result[i] = primitive[i]; // 自动装箱
        }
        return result;
    }

    /**
     * 获取向量模型
     */
    @SneakyThrows
    protected EmbeddingModel getEmbeddingModel(String modelName) {
        return embeddingModelFactory.createModel(modelName);
    }

    /**
     * 获取向量库类型标识
     */
    public abstract String getVectorStoreType();
}
