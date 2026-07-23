package org.ruoyi.service.vector.impl;

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
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Slf4j
@Component
public class MilvusVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private final KnowledgeAttachMapper knowledgeAttachMapper;
    private final org.ruoyi.mapper.knowledge.KnowledgeInfoMapper knowledgeInfoMapper;

    public MilvusVectorStoreStrategy(VectorStoreProperties vectorStoreProperties,
                                     IChatModelService chatModelService,
                                     EmbeddingModelFactory embeddingModelFactory,
                                     KnowledgeAttachMapper knowledgeAttachMapper,
                                     org.ruoyi.mapper.knowledge.KnowledgeInfoMapper knowledgeInfoMapper) {
        super(vectorStoreProperties, embeddingModelFactory, chatModelService);
        this.knowledgeAttachMapper = knowledgeAttachMapper;
        this.knowledgeInfoMapper = knowledgeInfoMapper;
    }

    // 缓存不同集合与 autoFlush 配置的 Milvus 连接
    private final Map<String, EmbeddingStore<TextSegment>> storeCache = new ConcurrentHashMap<>();

    /**
     * 获取 Milvus Store，支持动态维度
     */
    private EmbeddingStore<TextSegment> getMilvusStore(String collectionName, int dimension, boolean autoFlushOnInsert) {

        return MilvusEmbeddingStore.builder()
            .uri(vectorStoreProperties.getMilvus().getUrl())
            .collectionName(collectionName)
            .dimension(dimension)
            .indexType(IndexType.IVF_FLAT)
            .metricType(MetricType.COSINE)
            .autoFlushOnInsert(autoFlushOnInsert)
            .idFieldName("id")
            .textFieldName("text")
            .metadataFieldName("metadata")
            .vectorFieldName("vector")
            .build();
    }

    /**
     * 获取 embedding 模型的实际维度
     */
    private int getModelDimension(String modelName) {
        ChatModelVo modelConfig = chatModelService.selectModelByName(modelName);
        if (modelConfig == null || modelConfig.getModelDimension() == null) {
            log.warn("无法解析模型 {} 的向量维度，使用默认值 1024", modelName);
            return 1024;
        }
        return modelConfig.getModelDimension();
    }

    @Override
    public void createSchema(String kid, String modelName) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        int dimension = getModelDimension(modelName);
        // 使用缓存获取连接以确保只初始化一次
        EmbeddingStore<TextSegment> store = getMilvusStore(collectionName, dimension, true);
        log.info("Milvus集合初始化完成: {}, dimension: {}", collectionName, dimension);
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        int dimension = getModelDimension(storeEmbeddingBo.getEmbeddingModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName());

        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;

        log.info("Milvus向量存储条数记录: {}", chunkList.size());
        long startTime = System.currentTimeMillis();

        // 复用连接，写入场景使用 autoFlush=false 以提升批量插入性能
        // addAll is already batched; flush before returning so newly parsed fragments are immediately searchable.
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, dimension, true);

        List<TextSegment> segments = new ArrayList<>(chunkList.size());
        for (int i = 0; i < chunkList.size(); i++) {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Metadata metadata = new Metadata();
            metadata.put("fid", fid);
            metadata.put("kid", kid);
            metadata.put("docId", docId);

            segments.add(TextSegment.from(text, metadata));
        }
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        if (embeddings.size() != segments.size()) {
            throw new org.ruoyi.common.core.exception.ServiceException("Embedding 返回数量与分片数量不一致");
        }
        for (Embedding embedding : embeddings) {
            // 单位化处理
            float[] vector = embedding.vector();
            normalize(vector);
        }
        embeddingStore.addAll(embeddings, segments);
        long endTime = System.currentTimeMillis();
        log.info("Milvus向量存储完成消耗时间：{}秒", (endTime - startTime) / 1000);
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        int dimension = getModelDimension(queryVectorBo.getEmbeddingModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());

        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + queryVectorBo.getKid();

        // 查询复用连接，autoFlush 对查询无影响，此处保持 true
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, dimension, true);

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
    public List<KnowledgeRetrievalVo> search(QueryVectorBo queryVectorBo) {
        int dimension = getModelDimension(queryVectorBo.getEmbeddingModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());

        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        // 查询向量单位化处理
        float[] queryVector = queryEmbedding.vector();
        normalize(queryVector);

        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + queryVectorBo.getKid();

        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, dimension, true);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(queryVector))
                .maxResults(queryVectorBo.getMaxResults())
                .build();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        List<KnowledgeRetrievalVo> resultList = new ArrayList<>();

        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            if (segment == null) continue;

            String docId = segment.metadata().getString("docId");
            String fid = segment.metadata().getString("fid");
            String sourceName = "未知来源";
            if (docId != null) {
                KnowledgeAttach attach = knowledgeAttachMapper.selectOne(new LambdaQueryWrapper<KnowledgeAttach>()
                        .eq(KnowledgeAttach::getDocId, docId)
                        .last("limit 1"));
                if (attach != null) {
                    sourceName = attach.getName();
                }
            }

            // 提取内容、评分及来源
            double score = match.score();

            resultList.add(org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo.builder()
                    .id(fid)
                    .docId(docId)
                    .content(segment.text())
                    .score(score)
                    .sourceName(sourceName)
                    .build());
        }
        return resultList;
    }

    @Override
    @SneakyThrows
    public void removeById(String id, String modelName) {
        // 注意：此处原逻辑使用 collectionname + id，保持现状
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + id;
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
            .uri(vectorStoreProperties.getMilvus().getUrl()).build());
        try {
            client.dropCollection(DropCollectionReq.builder().collectionName(collectionName).build());
            storeCache.keySet().removeIf(key -> key.startsWith(collectionName));
            log.info("Milvus collection deleted: {}", collectionName);
        } catch (Exception e) {
            log.error("Milvus collection delete failed: {}", collectionName, e);
            throw new org.ruoyi.common.core.exception.ServiceException("Milvus向量集合删除失败");
        } finally {
            client.close();
        }
    }

    /**
     * 根据知识库ID解析其 embedding 模型维度，失败时回退默认 1024
     */
    private int getDimensionByKid(String kid) {
        try {
            org.ruoyi.domain.entity.knowledge.KnowledgeInfo info = knowledgeInfoMapper.selectById(Long.parseLong(kid));
            if (info != null && info.getEmbeddingModel() != null) {
                return getModelDimension(info.getEmbeddingModel());
            }
        } catch (Exception e) {
            log.warn("根据 kid={} 解析向量维度失败，使用默认 1024: {}", kid, e.getMessage());
        }
        return 1024;
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, getDimensionByKid(kid), true);
        Filter filter = MetadataFilterBuilder.metadataKey("docId").isEqualTo(docId);
        embeddingStore.removeAll(filter);
        log.info("Milvus成功删除 docId={} 的所有向量数据", docId);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = getMilvusStore(collectionName, getDimensionByKid(kid), true);
        Filter filter = MetadataFilterBuilder.metadataKey("fid").isEqualTo(fid);
        embeddingStore.removeAll(filter);
        log.info("Milvus成功删除 fid={} 的所有向量数据", fid);
    }

    @Override
    public String getVectorStoreType() {
        return "milvus";
    }
}
