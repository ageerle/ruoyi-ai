package org.ruoyi.service.strategy;

import org.ruoyi.common.core.exception.ServiceException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.config.VectorStoreProperties;

/**
 * 向量库策略抽象基类
 * 提供公共的方法实现，如embedding模型获取等
 *
 * @author ageer
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractVectorStoreStrategy implements VectorStoreStrategy {

    protected final VectorStoreProperties vectorStoreProperties;

    /**
     * 获取向量模型
     */
    @SneakyThrows
    protected EmbeddingModel getEmbeddingModel(String modelName, String apiKey, String baseUrl) {
        EmbeddingModel embeddingModel;
        if ("quentinz/bge-large-zh-v1.5".equals(modelName)) {
            embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        } else if ("baai/bge-m3".equals(modelName)) {
            embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        } else {
            throw new ServiceException("未找到对应向量化模型!");
        }
        return embeddingModel;
    }

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
     * 获取向量库类型标识
     */
    public abstract String getVectorStoreType();
}