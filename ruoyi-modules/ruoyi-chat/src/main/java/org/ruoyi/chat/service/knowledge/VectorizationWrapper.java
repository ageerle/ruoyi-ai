package org.ruoyi.chat.service.knowledge;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.factory.VectorizationFactory;
import org.ruoyi.service.VectorizationService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@Primary
@AllArgsConstructor
public class VectorizationWrapper implements VectorizationService {

    private final VectorizationFactory vectorizationFactory;
    @Override
    public List<List<Double>> batchVectorization(List<String> chunkList, String kid) {
        VectorizationService embedding = vectorizationFactory.getEmbedding(kid);
        return embedding.batchVectorization(chunkList, kid);
    }

    @Override
    public List<Double> singleVectorization(String chunk, String kid) {
        VectorizationService embedding = vectorizationFactory.getEmbedding(kid);
        return embedding.singleVectorization(chunk, kid);
    }
}
