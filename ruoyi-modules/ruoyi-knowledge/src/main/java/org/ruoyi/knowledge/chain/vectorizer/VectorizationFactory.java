package org.ruoyi.knowledge.chain.vectorizer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文本向量化
 * @author huangkh
 */
@Component
@Slf4j
public class VectorizationFactory {

    private final OpenAiVectorization openAiVectorization;

    public VectorizationFactory(OpenAiVectorization openAiVectorization) {
        this.openAiVectorization = openAiVectorization;
    }

    public Vectorization getEmbedding(){
        return openAiVectorization;
    }
}
