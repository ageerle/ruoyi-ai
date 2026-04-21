package org.ruoyi.observability;

import cn.hutool.core.collection.CollUtil;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * LangChain4j 监听器共享提供者。
 * <p>
 * 供所有 {@link dev.langchain4j.model.chat.StreamingChatModel} 构建器使用，
 * 将可观测性监听器注入到模型实例中。
 *
 * @author evo
 */
@Component
@Getter
@Lazy
public class ChatModelListenerProvider {

    private final List<ChatModelListener> chatModelListeners;
    private final List<EmbeddingModelListener> embeddingModelListeners;

    public ChatModelListenerProvider(@Nullable List<ChatModelListener> chatModelListeners,
                                     @Nullable List<EmbeddingModelListener> embeddingModelListeners) {
        if (CollUtil.isEmpty(chatModelListeners)) {
            chatModelListeners = Collections.emptyList();
        }
        if (CollUtil.isEmpty(embeddingModelListeners)) {
            embeddingModelListeners = Collections.emptyList();
        }
        this.chatModelListeners = chatModelListeners;
        this.embeddingModelListeners = embeddingModelListeners;
    }
}
