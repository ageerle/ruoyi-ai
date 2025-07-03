package org.ruoyi.service.impl;

import com.google.protobuf.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 向量库管理
 *
 * @author ageer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final ConfigService configService;

    private EmbeddingStore<TextSegment> embeddingStore;


    @Override
    public void createSchema(String kid, String modelName) {
        String protocol = configService.getConfigValue("weaviate", "protocol");
        String host = configService.getConfigValue("weaviate", "host");
        String className = configService.getConfigValue("weaviate", "classname");
        embeddingStore = WeaviateEmbeddingStore.builder()
                .scheme(protocol)
                .host(host)
                .objectClass(className+kid)
                .scheme(protocol)
                .avoidDups(true)
                .consistencyLevel("ALL")
                .build();
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        createSchema(storeEmbeddingBo.getKid(), storeEmbeddingBo.getVectorModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName(),
                storeEmbeddingBo.getApiKey(), storeEmbeddingBo.getBaseUrl());
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        for (String s : chunkList) {
            Embedding embedding = embeddingModel.embed(s).content();
            TextSegment segment = TextSegment.from(s);
            embeddingStore.add(embedding, segment);
        }
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getKid(), queryVectorBo.getVectorModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(),
                queryVectorBo.getApiKey(), queryVectorBo.getBaseUrl());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(queryVectorBo.getMaxResults())
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
        List<String> results = new ArrayList<>();
        matches.forEach(embeddingMatch -> results.add(embeddingMatch.embedded().text()));
        return results;
    }


    @Override
    @SneakyThrows
    public void removeById(String id, String modelName)  {
        String protocol = configService.getConfigValue("weaviate", "protocol");
        String host = configService.getConfigValue("weaviate", "host");
        String className = configService.getConfigValue("weaviate", "classname");
        String finalClassName = className + id;
        WeaviateClient client = new WeaviateClient(new Config(protocol, host));
        Result<Boolean> result = client.schema().classDeleter().withClassName(finalClassName).run();
        if (result.hasErrors()) {
            log.error("失败删除向量: " + result.getError());
            throw new ServiceException("失败删除向量数据!");
        } else {
            log.info("成功删除向量数据: " + result.getResult());
        }
    }

    /**
     * 获取向量模型
     */
    @SneakyThrows
    public EmbeddingModel getEmbeddingModel(String modelName, String apiKey, String baseUrl) {
        EmbeddingModel embeddingModel;
        if ("quentinz/bge-large-zh-v1.5".equals(modelName)) {
            embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        } else if ("baai/bge-m3".equals(modelName)) {
            embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        } else {
            throw new ServiceException("未找到对应向量化模型!");
        }
        return embeddingModel;
    }

}
