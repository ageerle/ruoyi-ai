package org.ruoyi.service.embed.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
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
 * @Date: 2025-09-30-下午3:59
 * @Description: OpenAi嵌入模型
 */
@Component("openai")
public class OpenAiEmbeddingProvider implements BaseEmbedModelService {
    protected ChatModelVo chatModelVo;

    @Autowired
    private EmbeddingModelListenerProvider embeddingModelListenerProvider;

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    @Override
    public Set<ModalityType> getSupportedModalities() {
        return Set.of(ModalityType.TEXT);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<EmbeddingModelListener> listeners = embeddingModelListenerProvider.getEmbeddingModelListeners();
        EmbeddingModel model = OpenAiEmbeddingModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .dimensions(chatModelVo.getModelDimension())
                .build();

        if (!listeners.isEmpty()) {
            model = model.addListeners(listeners);
        }

        return model.embedAll(textSegments);
    }
}
