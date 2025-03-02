package org.ruoyi.knowledge.chain.vectorizer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public List<List<Double>> batchVectorization(List<String> chunkList) {
        Vectorization embedding = vectorizationFactory.getEmbedding();
        return embedding.batchVectorization(chunkList);
    }

    @Override
    public List<Double> singleVectorization(String chunk) {
        Vectorization embedding = vectorizationFactory.getEmbedding();
        return embedding.singleVectorization(chunk);
    }
}
