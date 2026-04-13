package org.ruoyi.service.vector.impl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points.DenseVector;
import io.qdrant.client.grpc.Points.Query;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.VectorInput;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

import static io.qdrant.client.VectorInputFactory.vectorInput;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Qdrant向量库策略实现
 */
@Slf4j
@Component
public class QdrantVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private static final String VECTOR_STORE_TYPE   = "qdrant";
    private static final String TEXT_SEGMENT_KEY    = "text_segment";
    private static final String METADATA_FID_KEY    = "fid";
    private static final String METADATA_KID_KEY    = "kid";
    private static final String METADATA_DOC_ID_KEY = "doc_id";

    private final KnowledgeAttachMapper knowledgeAttachMapper;

    public QdrantVectorStoreStrategy(VectorStoreProperties vectorStoreProperties,
                                     IChatModelService chatModelService,
                                     EmbeddingModelFactory embeddingModelFactory,
                                     KnowledgeAttachMapper knowledgeAttachMapper) {
        super(vectorStoreProperties, embeddingModelFactory, chatModelService);
        this.knowledgeAttachMapper = knowledgeAttachMapper;
    }

    private EmbeddingStore<TextSegment> getQdrantStore(String collectionName) {
        VectorStoreProperties.Qdrant cfg = vectorStoreProperties.getQdrant();
        QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder()
                .host(cfg.getHost())
                .port(cfg.getPort())
                .collectionName(collectionName)
                .useTls(cfg.isUseTls());
        if (cfg.getApiKey() != null && !cfg.getApiKey().isEmpty()) {
            builder.apiKey(cfg.getApiKey());
        }
        return builder.build();
    }

    private QdrantClient buildQdrantClient() {
        VectorStoreProperties.Qdrant cfg = vectorStoreProperties.getQdrant();
        QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(cfg.getHost(), cfg.getPort(), cfg.isUseTls());
        if (cfg.getApiKey() != null && !cfg.getApiKey().isEmpty()) {
            grpcBuilder.withApiKey(cfg.getApiKey());
        }
        return new QdrantClient(grpcBuilder.build());
    }

    private int getModelDimension(String modelName) {
        return chatModelService.selectModelByName(modelName).getModelDimension();
    }

    @Override
    public String getVectorStoreType() {
        return VECTOR_STORE_TYPE;
    }

    @Override
    public void createSchema(String kid, String modelName) {
        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + kid;
        int dimension = getModelDimension(modelName);
        try (QdrantClient client = buildQdrantClient()) {
            Boolean exists = client.collectionExistsAsync(collectionName).get();
            if (!exists) {
                VectorParams params = VectorParams.newBuilder()
                        .setSize(dimension)
                        .setDistance(Distance.Cosine)
                        .build();
                client.createCollectionAsync(collectionName, params).get();
                log.info("Qdrant集合创建成功: {}, dimension: {}", collectionName, dimension);
            } else {
                log.info("Qdrant集合已存在: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("Qdrant集合创建失败: {}", collectionName, e);
            throw new ServiceException("Qdrant集合创建失败: " + collectionName);
        }
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName());
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + kid;

        EmbeddingStore<TextSegment> embeddingStore = getQdrantStore(collectionName);

        log.info("Qdrant向量存储条数记录: {}", chunkList.size());
        long startTime = System.currentTimeMillis();

        IntStream.range(0, chunkList.size()).forEach(i -> {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Metadata metadata = new Metadata();
            metadata.put(METADATA_FID_KEY, fid);
            metadata.put(METADATA_KID_KEY, kid);
            metadata.put(METADATA_DOC_ID_KEY, docId);
            TextSegment textSegment = TextSegment.from(text, metadata);
            Embedding embedding = embeddingModel.embed(text).content();
            // 单位化处理
            float[] vector = embedding.vector();
            normalize(vector);
            embeddingStore.add(Embedding.from(vector), textSegment);
        });

        long endTime = System.currentTimeMillis();
        log.info("Qdrant向量存储完成消耗时间：{}秒", (endTime - startTime) / 1000);
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        // 查询向量单位化处理
        float[] queryVector = queryEmbedding.vector();
        normalize(queryVector);

        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + queryVectorBo.getKid();

        List<Float> vectorList = new ArrayList<>();
        for (float f : queryVector) {
            vectorList.add(f);
        }

        try (QdrantClient client = buildQdrantClient()) {
            QueryPoints request = QueryPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setQuery(Query.newBuilder()
                            .setNearest(vectorInput(vectorList))
                            .build())
                    .setLimit(queryVectorBo.getMaxResults())
                    .setWithPayload(enable(true))
                    .build();

            List<ScoredPoint> results = client.queryAsync(request).get();
            List<String> resultList = new ArrayList<>();
            for (ScoredPoint point : results) {
                JsonWithInt.Value textValue = point.getPayloadMap().get(TEXT_SEGMENT_KEY);
                if (textValue != null && textValue.hasStringValue()) {
                    resultList.add(textValue.getStringValue());
                }
            }
            return resultList;
        } catch (Exception e) {
            log.error("Qdrant查询失败: {}", collectionName, e);
            throw new ServiceException("Qdrant向量查询失败");
        }
    }

    @Override
    public List<KnowledgeRetrievalVo> search(QueryVectorBo queryVectorBo) {
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        // 查询向量单位化处理
        float[] queryVector = queryEmbedding.vector();
        normalize(queryVector);

        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + queryVectorBo.getKid();

        List<Float> vectorList = new ArrayList<>();
        for (float f : queryVector) {
            vectorList.add(f);
        }

        try (QdrantClient client = buildQdrantClient()) {
            QueryPoints request = QueryPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setQuery(Query.newBuilder()
                            .setNearest(vectorInput(vectorList))
                            .build())
                    .setLimit(queryVectorBo.getMaxResults())
                    .setWithPayload(enable(true))
                    .build();

            List<ScoredPoint> results = client.queryAsync(request).get();
            List<org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo> resultList = new ArrayList<>();
            for (ScoredPoint point : results) {
                String content = "";
                JsonWithInt.Value textValue = point.getPayloadMap().get(TEXT_SEGMENT_KEY);
                if (textValue != null && textValue.hasStringValue()) {
                    content = textValue.getStringValue();
                }

                String docId = null;
                JsonWithInt.Value docIdValue = point.getPayloadMap().get(METADATA_DOC_ID_KEY);
                if (docIdValue != null && docIdValue.hasStringValue()) {
                    docId = docIdValue.getStringValue();
                }

                String sourceName = "未知来源";
                if (docId != null) {
                    KnowledgeAttach attach = knowledgeAttachMapper.selectOne(new LambdaQueryWrapper<KnowledgeAttach>()
                            .eq(KnowledgeAttach::getDocId, docId)
                            .last("limit 1"));
                    if (attach != null) {
                        sourceName = attach.getName();
                    }
                }

                resultList.add(org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo.builder()
                        .content(content)
                        .score((double) point.getScore())
                        .sourceName(sourceName)
                        .build());
            }
            return resultList;
        } catch (Exception e) {
            log.error("Qdrant检索失败: {}", collectionName, e);
            throw new ServiceException("Qdrant向量检索失败");
        }
    }

    @Override
    public void removeById(String id, String modelName) {
        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + id;
        try (QdrantClient client = buildQdrantClient()) {
            client.deleteCollectionAsync(collectionName).get();
            log.info("Qdrant成功删除集合: {}", collectionName);
        } catch (Exception e) {
            log.error("Qdrant删除集合失败: {}", collectionName, e);
            throw new ServiceException("失败删除向量数据!");
        }
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = getQdrantStore(collectionName);
        Filter filter = MetadataFilterBuilder.metadataKey(METADATA_DOC_ID_KEY).isEqualTo(docId);
        embeddingStore.removeAll(filter);
        log.info("Qdrant成功删除 docId={} 的所有向量数据", docId);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String collectionName = vectorStoreProperties.getQdrant().getCollectionname() + kid;
        EmbeddingStore<TextSegment> embeddingStore = getQdrantStore(collectionName);
        Filter filter = MetadataFilterBuilder.metadataKey(METADATA_FID_KEY).isEqualTo(fid);
        embeddingStore.removeAll(filter);
        log.info("Qdrant成功删除 fid={} 的所有向量数据", fid);
    }
}
