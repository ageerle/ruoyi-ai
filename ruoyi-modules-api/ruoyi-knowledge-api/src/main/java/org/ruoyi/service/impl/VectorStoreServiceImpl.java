package org.ruoyi.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.google.protobuf.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Service;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量库管理
 * @author ageer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final ConfigService configService;

    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void createSchema(String kid,String modelName) {
        switch (modelName) {
            case "weaviate" -> {
                String protocol = configService.getConfigValue("weaviate", "protocol");
                String host = configService.getConfigValue("weaviate", "host");
                String className = configService.getConfigValue("weaviate", "classname");
                embeddingStore = WeaviateEmbeddingStore.builder()
                        .scheme(protocol)
                        .host(host)
                        .objectClass(className + kid)
                        .scheme(protocol)
                        .avoidDups(true)
                        .consistencyLevel("ALL")
                        .build();
            }
            case "milvus" -> {
                String uri = configService.getConfigValue("milvus", "host");
                String collection = configService.getConfigValue("milvus", "collection");
                String dimension = configService.getConfigValue("milvus", "dimension");
                embeddingStore = MilvusEmbeddingStore.builder()
                        .uri(uri)
                        .collectionName(collection + kid)
                        .dimension(Integer.parseInt(dimension))
                        .build();
            }
            case "qdrant" -> {
                String host = configService.getConfigValue("qdrant", "host");
                String port = configService.getConfigValue("qdrant", "port");
                String collectionName = configService.getConfigValue("qdrant", "collectionName");
                embeddingStore = QdrantEmbeddingStore.builder()
                        .host(host)
                        .port(Integer.parseInt(port))
                        .collectionName(collectionName)
                        .build();
            }
            default -> {
                //使用内存
                embeddingStore = new InMemoryEmbeddingStore<>();
            }
        }
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        createSchema(storeEmbeddingBo.getKid(),storeEmbeddingBo.getVectorModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName(),
                storeEmbeddingBo.getApiKey(), storeEmbeddingBo.getBaseUrl());
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        for (int i = 0; i < chunkList.size(); i++) {
            Map<String, Object> dataSchema = new HashMap<>();
            dataSchema.put("kid", storeEmbeddingBo.getKid());
            dataSchema.put("docId", storeEmbeddingBo.getDocId());
            dataSchema.put("fid", storeEmbeddingBo.getFids().get(i));
            Embedding embedding = embeddingModel.embed(chunkList.get(i)).content();
            TextSegment segment = TextSegment.from(chunkList.get(i));
            segment.metadata().putAll(dataSchema);
            embeddingStore.add(embedding,segment);
        }
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getKid(),queryVectorBo.getVectorModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(),
                queryVectorBo.getApiKey(), queryVectorBo.getBaseUrl());
       // Filter simpleFilter = new IsEqualTo("kid", queryVectorBo.getKid());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(queryVectorBo.getMaxResults())
                // 添加过滤条件
         //       .filter(simpleFilter)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();
        List<String> results = new ArrayList<>();
        matches.forEach(embeddingMatch -> results.add(embeddingMatch.embedded().text()));
        return results;
    }


    @Override
    public void removeByKid(String kid,String modelName) {
        createSchema(kid,modelName);
        // 根据条件删除向量数据
        Filter simpleFilter = new IsEqualTo("kid", kid);
        removeByFilter(simpleFilter);
    }

    public void removeByFilter(Filter filter) {
        List<Float> dummyVector = new ArrayList<>();
        // TODO 模型维度
        int dimension = 1024;
        for (int i = 0; i < dimension; i++) {
            dummyVector.add(0.0f);
        }
        Embedding dummyEmbedding = Embedding.from(dummyVector);
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(dummyEmbedding)
                .filter(filter)
                .maxResults(10000)
                .build();
        // 搜索
        List<String> idsToDelete = embeddingStore.search(request)
                .matches().stream()
                .map(EmbeddingMatch::embeddingId)
                .collect(Collectors.toList());
        // 删除
        if (!idsToDelete.isEmpty()) {
            embeddingStore.removeAll(idsToDelete);
        }
    }

    @Override
    public void removeByDocId(String kid, String docId,String modelName) {
        createSchema(kid,modelName);
        // 根据条件删除向量数据
        Filter simpleFilterByDocId = new IsEqualTo("docId", docId);
        embeddingStore.removeAll(simpleFilterByDocId);
    }

    @Override
    public void removeByKidAndFid(String kid, String fid,String modelName) {
        createSchema(kid,modelName);
        // 根据条件删除向量数据
        Filter simpleFilterByKid = new IsEqualTo("kid", kid);
        Filter simpleFilterFid = new IsEqualTo("fid", fid);
        Filter simpleFilterByAnd = Filter.and(simpleFilterFid, simpleFilterByKid);
        embeddingStore.removeAll(simpleFilterByAnd);
    }

    /**
     * 获取向量模型
     */
    @SneakyThrows
    public EmbeddingModel getEmbeddingModel(String modelName, String apiKey, String baseUrl) {
        EmbeddingModel embeddingModel;
        if(TEXT_EMBEDDING_3_SMALL.toString().equals(modelName)) {
             embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        // TODO 添加枚举
        }else if("quentinz/bge-large-zh-v1.5".equals(modelName)) {
            embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        }else if("baai/bge-m3".equals(modelName)) {
            embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        }else {
            throw new ServiceException("未找到对应向量化模型!");
        }
        return embeddingModel;
    }

}
