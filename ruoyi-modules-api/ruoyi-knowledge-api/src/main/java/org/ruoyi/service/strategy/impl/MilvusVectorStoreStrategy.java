package org.ruoyi.service.strategy.impl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.config.VectorStoreProperties;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.embedding.EmbeddingModelFactory;
import org.ruoyi.service.strategy.AbstractVectorStoreStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MilvusVectorStoreStrategy extends AbstractVectorStoreStrategy {


    private final Integer DIMENSION = 2048;
    // 缓存不同集合与 autoFlush 配置的 Milvus 连接
    private final Map<String, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    public MilvusVectorStoreStrategy(VectorStoreProperties vectorStoreProperties, EmbeddingModelFactory embeddingModelFactory) {
        super(vectorStoreProperties, embeddingModelFactory);
    }

    private EmbeddingStore<TextSegment> getMilvusStore(String collectionName, boolean autoFlushOnInsert) {
        String key = collectionName + "|" + autoFlushOnInsert;
        return storeCache.computeIfAbsent(key, k ->
                MilvusEmbeddingStore.builder()
                        .uri(vectorStoreProperties.getMilvus().getUrl())
                        .collectionName(collectionName)
                        .dimension(DIMENSION)
                        .indexType(IndexType.IVF_FLAT)
                        .metricType(MetricType.L2)
                        .autoFlushOnInsert(autoFlushOnInsert)
                        .idFieldName("id")
                        .textFieldName("text")
                        .metadataFieldName("metadata")
                        .vectorFieldName("vector")
                        .build()
        );
    }

    @Override
    public void createSchema(String kid, String modelName) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        // 使用缓存获取连接以确保只初始化一次
        EmbeddingStore<TextSegment> store = getMilvusStore(collectionName, true);
        log.info("Milvus集合初始化完成: {}", collectionName);
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName(), DIMENSION);

        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;

        log.info("Milvus向量存储条数记录: {}", chunkList.size());
        long startTime = System.currentTimeMillis();

        // 复用连接，写入场景使用 autoFlush=false 以提升批量插入性能
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, false);

        IntStream.range(0, chunkList.size()).forEach(i -> {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Metadata metadata = new Metadata();
            metadata.put("fid", fid);
            metadata.put("kid", kid);
            metadata.put("docId", docId);

            TextSegment textSegment = TextSegment.from(text, metadata);
            Embedding embedding = embeddingModel.embed(text).content();
            embeddingStore.add(embedding, textSegment);
        });
        long endTime = System.currentTimeMillis();
        log.info("Milvus向量存储完成消耗时间：{}秒", (endTime - startTime) / 1000);
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(), DIMENSION);

        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + queryVectorBo.getKid();

        // 查询复用连接，autoFlush 对查询无影响，此处保持 true
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, true);

        List<String> resultList = new ArrayList<>();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(queryVectorBo.getMaxResults())
                .build();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            if (segment != null) {
                resultList.add(segment.text());
            }
        }
        return resultList;
    }

    @Override
    @SneakyThrows
    public void removeById(String id, String modelName) {
        // 注意：此处原逻辑使用 collectionname + id，保持现状
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(vectorStoreProperties.getMilvus().getCollectionname() + id, false);
        embeddingStore.remove(id);
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, false);
        Filter filter = MetadataFilterBuilder.metadataKey("docId").isEqualTo(docId);
        embeddingStore.removeAll(filter);
        log.info("Milvus成功删除 docId={} 的所有向量数据", docId);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, false);
        Filter filter = MetadataFilterBuilder.metadataKey("fid").isEqualTo(fid);
        embeddingStore.removeAll(filter);
        log.info("Milvus成功删除 fid={} 的所有向量数据", fid);
    }

    @Override
    public String getVectorStoreType() {
        return "milvus";
    }
}
