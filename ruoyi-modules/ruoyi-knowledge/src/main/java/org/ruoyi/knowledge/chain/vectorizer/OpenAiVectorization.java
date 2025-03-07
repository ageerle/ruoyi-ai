package org.ruoyi.knowledge.chain.vectorizer;

import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.config.ChatConfig;
import org.ruoyi.common.chat.entity.embeddings.Embedding;

import org.ruoyi.common.chat.entity.embeddings.EmbeddingResponse;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.service.IKnowledgeInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenAiVectorization implements Vectorization {

    @Lazy
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;

    @Getter
    private OpenAiStreamClient openAiStreamClient;

    private final ChatConfig chatConfig;

    @Override
    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {
        openAiStreamClient = chatConfig.getOpenAiStreamClient();
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(kid));
        Embedding embedding = Embedding.builder()
            .input(chunkList)
            .model(knowledgeInfoVo.getVectorModel())
            .build();
        EmbeddingResponse embeddings = openAiStreamClient.embeddings(embedding);
        List<List<Double>> vectorList = new ArrayList<>();
        embeddings.getData().forEach(data -> {
            List<BigDecimal> vector = data.getEmbedding();
            List<Double> doubleVector = new ArrayList<>();
            for (BigDecimal bd : vector) {
                doubleVector.add(bd.doubleValue());
            }
            vectorList.add(doubleVector);
        });
        return vectorList;
    }

    @Override
    public List<Double> singleVectorization(String chunk, String kid) {
        List<String> chunkList = new ArrayList<>();
        chunkList.add(chunk);
        List<List<Double>> vectorList = batchVectorization(chunkList, kid);
        return vectorList.get(0);
    }

}
