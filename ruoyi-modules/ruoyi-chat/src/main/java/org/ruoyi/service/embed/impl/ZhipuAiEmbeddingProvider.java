package org.ruoyi.service.embed.impl;



import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ModalityType;
import org.ruoyi.service.embed.BaseEmbedModelService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @Author:yang
 * @Date:
 * @Description: 智谱AI嵌入模型
 */
@Component("zhipu")
public class ZhipuAiEmbeddingProvider implements BaseEmbedModelService {
    protected ChatModelVo chatModelVo;

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
        EmbeddingModel model = ZhipuAiEmbeddingModel.builder()
            .baseUrl(chatModelVo.getApiHost())
            .apiKey(chatModelVo.getApiKey())
            .model(chatModelVo.getModelName())
            .dimensions(chatModelVo.getModelDimension())
            .build();

        return model.embedAll(textSegments);
    }
}
