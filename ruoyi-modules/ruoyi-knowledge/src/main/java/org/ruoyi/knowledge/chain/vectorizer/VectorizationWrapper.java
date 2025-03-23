package org.ruoyi.knowledge.chain.vectorizer;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.service.IKnowledgeInfoService;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@Primary
@AllArgsConstructor
public class VectorizationWrapper implements Vectorization{

    private final VectorizationFactory vectorizationFactory;
    @Override
    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {
        Vectorization embedding = vectorizationFactory.getEmbedding(kid);
        return embedding.batchVectorization(chunkList, kid);
    }

    @Override
    public List<Double> singleVectorization(String chunk, String kid) {
        Vectorization embedding = vectorizationFactory.getEmbedding(kid);
        return embedding.singleVectorization(chunk, kid);
    }
}
