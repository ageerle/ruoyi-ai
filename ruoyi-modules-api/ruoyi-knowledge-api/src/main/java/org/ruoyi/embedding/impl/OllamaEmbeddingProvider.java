package org.ruoyi.embedding.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.embedding.BaseEmbedModelService;
import org.ruoyi.embedding.model.ModalityType;
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
        return OllamaEmbeddingModel.builder()
                .baseUrl(chatModelVo.getApiHost())
                .modelName(chatModelVo.getModelName())
                .build()
                .embedAll(textSegments);
    }
}
