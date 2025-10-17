package org.ruoyi.service.strategy.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
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
import org.ruoyi.service.strategy.AbstractVectorStoreStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Milvus向量库策略实现
 *
 * @author Yzm
 */
@Slf4j
@Component
public class MilvusVectorStoreStrategy extends AbstractVectorStoreStrategy {

    public MilvusVectorStoreStrategy(VectorStoreProperties vectorStoreProperties) {
        super(vectorStoreProperties);
    }

    @Override
    public String getVectorStoreType() {
        return "milvus";
    }

    @Override
    public void createSchema(String vectorModelName, String kid) {
        String url = vectorStoreProperties.getMilvus().getUrl();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        // 使用 LangChain4j 的 MilvusEmbeddingStore 来确保集合存在（按需创建）
        MilvusEmbeddingStore store = MilvusEmbeddingStore.builder()
                .uri(url)
                .collectionName(collectionName)
                .dimension(2048)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.L2)
                .autoFlushOnInsert(true)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();
        log.info("Milvus集合初始化完成: {}", collectionName);
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        createSchema(storeEmbeddingBo.getVectorModelName(), storeEmbeddingBo.getKid());

        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName(),
                storeEmbeddingBo.getApiKey(), storeEmbeddingBo.getBaseUrl());

        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;

        log.info("Milvus向量存储条数记录: {}", chunkList.size());
        long startTime = System.currentTimeMillis();

        EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                .uri(vectorStoreProperties.getMilvus().getUrl())
                .collectionName(collectionName)
                .dimension(2048)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.L2)
                .autoFlushOnInsert(false)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();

        IntStream.range(0, chunkList.size()).forEach(i -> {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Embedding embedding = embeddingModel.embed(text).content();
            Metadata metadata = new Metadata()
                    .put("fid", fid)
                    .put("kid", kid)
                    .put("docId", docId);
            TextSegment segment = TextSegment.from(text, metadata);
            embeddingStore.add(embedding, segment);
        });

        long endTime = System.currentTimeMillis();
        log.info("Milvus向量存储完成消耗时间：{}秒", (endTime - startTime) / 1000);
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getVectorModelName(), queryVectorBo.getKid());

        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(),
                queryVectorBo.getApiKey(), queryVectorBo.getBaseUrl());

        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + queryVectorBo.getKid();

        EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                .uri(vectorStoreProperties.getMilvus().getUrl())
                .collectionName(collectionName)
                .dimension(2048)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.L2)
                .autoFlushOnInsert(true)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();

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
        String url = vectorStoreProperties.getMilvus().getUrl();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + id;
        MilvusEmbeddingStore store = MilvusEmbeddingStore.builder()
                .uri(url)
                .collectionName(collectionName)
                .dimension(2048)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.L2)
                .autoFlushOnInsert(true)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();
        // 修正：MilvusEmbeddingStore 的 dropCollection 需要传入集合名
        store.dropCollection(collectionName);
        log.info("Milvus集合删除成功: {}", collectionName);
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                .uri(vectorStoreProperties.getMilvus().getUrl())
                .collectionName(collectionName)
                .dimension(2048)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.L2)
                .autoFlushOnInsert(false)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();
        Filter filter = MetadataFilterBuilder.metadataKey("docId").isEqualTo(docId);
        embeddingStore.removeAll(filter);
        log.info("Milvus成功删除 docId={} 的所有向量数据", docId);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = MilvusEmbeddingStore.builder()
                .uri(vectorStoreProperties.getMilvus().getUrl())
                .collectionName(collectionName)
                .dimension(2048)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.L2)
                .autoFlushOnInsert(false)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();
        Filter filter = MetadataFilterBuilder.metadataKey("fid").isEqualTo(fid);
        embeddingStore.removeAll(filter);
        log.info("Milvus成功删除 fid={} 的所有向量数据", fid);
    }
}
