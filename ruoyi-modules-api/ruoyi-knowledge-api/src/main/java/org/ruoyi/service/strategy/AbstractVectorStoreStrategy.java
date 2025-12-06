package org.ruoyi.service.strategy;

import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.config.VectorStoreProperties;
import org.ruoyi.embedding.EmbeddingModelFactory;
import org.ruoyi.service.VectorStoreService;

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
    protected EmbeddingModel getEmbeddingModel(String modelName, Integer dimension) {
        return embeddingModelFactory.createModel(modelName, dimension);
    }

    /**
     * 获取向量库类型标识
     */
    public abstract String getVectorStoreType();
}