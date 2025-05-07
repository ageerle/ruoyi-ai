package org.ruoyi.service.impl;

import cn.hutool.core.util.RandomUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.service.VectorStoreService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.testcontainers.weaviate.WeaviateContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WeaviateVectorStoreImpl implements VectorStoreService {

    private volatile String protocol;
    private volatile String host;
    private volatile String className;

    @Lazy
    @Resource
    private IKnowledgeInfoService knowledgeInfoService;

    @Lazy
    @Resource
    private ConfigService configService;

    private  EmbeddingStore<TextSegment> embeddingStore;

    @PostConstruct
    public void loadConfig() {
        this.protocol = configService.getConfigValue("weaviate", "protocol");
        this.host = configService.getConfigValue("weaviate", "host");
        this.className = configService.getConfigValue("weaviate", "classname");
    }


    @Override
    public List<String> getQueryVector(String query, String kid) {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .baseUrl(System.getenv("OPENAI_BASE_URL"))
                .modelName("text-embedding-3-small")
                .build();

        Filter simpleFilter = new IsEqualTo("kid", kid);

        Embedding queryEmbedding = embeddingModel.embed("What is your favourite sport?").content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                // 添加过滤条件
                .filter(simpleFilter)
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
        WeaviateContainer weaviate = new WeaviateContainer(protocol);
        weaviate.start();
        this.embeddingStore = WeaviateEmbeddingStore.builder()
                .scheme("http")
                .host(host)
                .objectClass(className+kid)
                .scheme(protocol)
                .avoidDups(true)
                .consistencyLevel("ALL")
                .build();
    }

    @Override
    public void storeEmbeddings(List<String> chunkList,String kid) {
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .baseUrl(System.getenv("OPENAI_BASE_URL"))
                .modelName("text-embedding-3-small")
                .build();
        // 生成文档id
        String docId = RandomUtil.randomString(10);
        chunkList.forEach(chunk -> {
            // 生成知识块id
            String fid = RandomUtil.randomString(10);
            Map<String, Object> dataSchema = new HashMap<>();
            dataSchema.put("kid", kid);
            dataSchema.put("docId", docId);
            dataSchema.put("fid", fid);
            TextSegment segment = TextSegment.from(chunk);
            segment.metadata().putAll(dataSchema);
            Embedding content = embeddingModel.embed(segment).content();
            embeddingStore.add(content);
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
