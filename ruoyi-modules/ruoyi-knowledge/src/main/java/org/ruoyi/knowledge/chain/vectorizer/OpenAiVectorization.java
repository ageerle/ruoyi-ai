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
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenAiVectorization implements Vectorization {

    @Lazy
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;
    @Lazy
    @Resource
    private LocalModelsVectorization localModelsVectorization;

    @Getter
    private OpenAiStreamClient openAiStreamClient;

    private final ChatConfig chatConfig;

    @Override
    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {
        List<List<Double>> vectorList = new ArrayList<>();

        // 获取知识库信息
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(Long.valueOf(kid));

        // 如果使用本地模型
        try {
            return localModelsVectorization.batchVectorization(chunkList, kid);
        } catch (Exception e) {
            log.error("Local models vectorization failed, falling back to OpenAI embeddings", e);
        }

        // 如果本地模型失败，则调用 OpenAI 服务进行向量化
        Embedding embedding = buildEmbedding(chunkList, knowledgeInfoVo);
        EmbeddingResponse embeddings = openAiStreamClient.embeddings(embedding);

        // 处理 OpenAI 返回的嵌入数据
        vectorList = processOpenAiEmbeddings(embeddings);

        return vectorList;
    }

    /**
     * 构建 Embedding 对象
     */
    private Embedding buildEmbedding(List<String> chunkList, KnowledgeInfoVo knowledgeInfoVo) {
        return Embedding.builder()
                .input(chunkList)
                .model(knowledgeInfoVo.getVectorModel())
                .build();
    }

    /**
     * 处理 OpenAI 返回的嵌入数据
     */
    private List<List<Double>> processOpenAiEmbeddings(EmbeddingResponse embeddings) {
        List<List<Double>> vectorList = new ArrayList<>();

        embeddings.getData().forEach(data -> {
            List<BigDecimal> vector = data.getEmbedding();
            List<Double> doubleVector = convertToDoubleList(vector);
            vectorList.add(doubleVector);
        });

        return vectorList;
    }

    /**
     * 将 BigDecimal 转换为 Double 列表
     */
    private List<Double> convertToDoubleList(List<BigDecimal> vector) {
        return vector.stream()
                .map(BigDecimal::doubleValue)
                .collect(Collectors.toList());
    }


    @Override
    public List<Double> singleVectorization(String chunk, String kid) {
        List<String> chunkList = new ArrayList<>();
        chunkList.add(chunk);
        List<List<Double>> vectorList = batchVectorization(chunkList, kid);
        return vectorList.get(0);
    }

}
