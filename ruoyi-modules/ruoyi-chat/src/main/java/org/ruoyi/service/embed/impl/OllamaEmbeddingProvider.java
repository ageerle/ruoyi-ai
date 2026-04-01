package org.ruoyi.service.embed.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ModalityType;
import org.ruoyi.observability.EmbeddingModelListenerProvider;
import org.ruoyi.service.embed.BaseEmbedModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @Author: Robust_H
 * @Date: 2025-09-30-下午3:00
 * @Description: Ollama嵌入模型
 */
@Component("ollama")
public class OllamaEmbeddingProvider implements BaseEmbedModelService {
    private ChatModelVo chatModelVo;

    @Resource
    private EmbeddingModelListenerProvider embeddingModelListenerProvider;

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    @Override
    public Set<ModalityType> getSupportedModalities() {
        return Set.of(ModalityType.TEXT);
    }

    // ollama不能设置embedding维度，使用milvus时请注意！！创建向量表时需要先设定维度大小
    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<EmbeddingModelListener> listeners = embeddingModelListenerProvider.getEmbeddingModelListeners();
        EmbeddingModel model = OllamaEmbeddingModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .build();

        if (!listeners.isEmpty()) {
            model = model.addListeners(listeners);
        }

        return model.embedAll(textSegments);
    }
}
