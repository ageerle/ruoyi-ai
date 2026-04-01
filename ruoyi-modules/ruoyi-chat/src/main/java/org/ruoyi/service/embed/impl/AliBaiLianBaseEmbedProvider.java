package org.ruoyi.service.embed.impl;


import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.output.Response;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.observability.EmbeddingModelListenerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ruoyi.enums.ModalityType;

import java.util.List;
import java.util.Set;

/**
 * @Author: Robust_H
 * @Date: 2025-09-30-下午3:00
 * @Description: 阿里百炼基础嵌入模型（兼容openai）
 */
@Component("alibailian")
public class AliBaiLianBaseEmbedProvider extends OpenAiEmbeddingProvider {

    private ChatModelVo chatModelVo;

    @Autowired
    private EmbeddingModelListenerProvider embeddingModelListenerProvider;

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    @Override
    public Set<ModalityType> getSupportedModalities() {
        return Set.of();
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<EmbeddingModelListener> listeners = embeddingModelListenerProvider.getEmbeddingModelListeners();
        EmbeddingModel model = QwenEmbeddingModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .dimension(chatModelVo.getModelDimension())
                .build();

        if (!listeners.isEmpty()) {
            model = model.addListeners(listeners);
        }

        return model.embedAll(textSegments);
    }

}
