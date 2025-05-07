package org.ruoyi.service.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Service;

import static dev.langchain4j.model.openai.OpenAiEmbeddingModelName.TEXT_EMBEDDING_3_SMALL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ageer
 * Weaviate 向量库管理
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeaviateVectorStoreImpl implements VectorStoreService {

    private EmbeddingStore<TextSegment> embeddingStore;

    private final ConfigService configService;

    @Override
    public List<String> getQueryVector(String query, String kid) {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey("sk-xxx")
                .baseUrl("https://api.pandarobot.chat/v1/")
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

      //  Filter simpleFilter = new IsEqualTo("kid", kid);

     //   createSchema(kid);

        Embedding queryEmbedding = embeddingModel.embed("聊天补全模型").content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(2)
                // 添加过滤条件
             //   .filter(simpleFilter)
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(embeddingSearchRequest).matches();



        List<String> results = new ArrayList<>();

        matches.forEach(embeddingMatch -> {
            results.add(embeddingMatch.embedded().text());
        });

        return results;
    }

    @Override
    public void createSchema(String kid) {
        String protocol = configService.getConfigValue("weaviate", "protocol");
        String host = configService.getConfigValue("weaviate", "host");
        String className = configService.getConfigValue("weaviate", "classname");
        this.embeddingStore = WeaviateEmbeddingStore.builder()
                .scheme(protocol)
                .host(host)
                .objectClass(className+kid)
                .scheme(protocol)
                .avoidDups(true)
                .consistencyLevel("ALL")
                .build();
    }

    @Override
    public void storeEmbeddings(List<String> chunkList,String kid,String docId,List<String> fids) {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey("sk-xxxx")
                .baseUrl("https://api.pandarobot.chat/v1/")
                .modelName(TEXT_EMBEDDING_3_SMALL)
                .build();

        chunkList.forEach(chunk -> {
            Map<String, Object> dataSchema = new HashMap<>();
            dataSchema.put("kid", kid);
            dataSchema.put("docId", docId);
            dataSchema.put("fid", fids.get(0));
            Response<Embedding> response = embeddingModel.embed(chunk);
            Embedding embedding = response.content();
            TextSegment segment = TextSegment.from(chunk);
            segment.metadata().putAll(dataSchema);
            embeddingStore.add(embedding,segment);
        });
    }


    @Override
    public void removeByKid(String kid) {
        // 根据条件删除向量数据
        Filter simpleFilter = new IsEqualTo("kid", kid);
        embeddingStore.removeAll(simpleFilter);
    }

    @Override
    public void removeByDocId(String kid, String docId) {
        // 根据条件删除向量数据
        Filter simpleFilterByDocId = new IsEqualTo("docId", docId);
        embeddingStore.removeAll(simpleFilterByDocId);
    }

    @Override
    public void removeByKidAndFid(String kid, String fid) {
        // 根据条件删除向量数据
        Filter simpleFilterByKid = new IsEqualTo("kid", kid);
        Filter simpleFilterFid = new IsEqualTo("fid", fid);
        Filter simpleFilterByAnd = Filter.and(simpleFilterFid, simpleFilterByKid);
        embeddingStore.removeAll(simpleFilterByAnd);
    }

}
