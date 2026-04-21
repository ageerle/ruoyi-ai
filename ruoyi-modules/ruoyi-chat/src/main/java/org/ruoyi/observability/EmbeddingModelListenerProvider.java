package org.ruoyi.observability;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * EmbeddingModel 监听器共享提供者。
 * <p>
 * 供所有 {@link dev.langchain4j.model.embedding.EmbeddingModel} 构建器使用，
 * 将可观测性监听器注入到模型实例中。
 *
 * @author evo
 */
@Component
@Getter
@Lazy
public class EmbeddingModelListenerProvider {

    private final List<EmbeddingModelListener> embeddingModelListeners;

    public EmbeddingModelListenerProvider(@Nullable List<EmbeddingModelListener> embeddingModelListeners) {
        if (CollUtil.isEmpty(embeddingModelListeners)) {
            embeddingModelListeners = Collections.emptyList();
        }
        this.embeddingModelListeners = embeddingModelListeners;
    }
}
