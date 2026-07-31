package org.ruoyi.service.vector.impl;

import cn.hutool.json.JSONObject;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;

import io.weaviate.client.WeaviateClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.domain.bo.vector.QueryVectorBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.springframework.stereotype.Component;
import io.weaviate.client.Config;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatchDeleter;
import io.weaviate.client.v1.batch.api.ObjectsBatcher;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.batch.model.BatchDeleteResponse;
import io.weaviate.client.v1.filters.Operator;
import io.weaviate.client.v1.filters.WhereFilter;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.Schema;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Weaviate向量库策略实现
 *
 * @author Yzm
 */
@Slf4j
@Component
public class WeaviateVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private volatile WeaviateClient client;
    private final KnowledgeAttachMapper knowledgeAttachMapper;
    /**
     * 已确认存在的 class 缓存，避免每次检索都全量拉取 schema
     */
    private final java.util.Set<String> knownClasses = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public WeaviateVectorStoreStrategy(VectorStoreProperties vectorStoreProperties,
                                       IChatModelService chatModelService,
                                       EmbeddingModelFactory embeddingModelFactory,
                                       KnowledgeAttachMapper knowledgeAttachMapper) {
        super(vectorStoreProperties, embeddingModelFactory,chatModelService);
        this.knowledgeAttachMapper = knowledgeAttachMapper;
    }

    /**
     * 懒加载单例客户端，避免 remove 等方法在未调用 createSchema 时 NPE
     */
    private WeaviateClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    String protocol = vectorStoreProperties.getWeaviate().getProtocol();
                    String host = vectorStoreProperties.getWeaviate().getHost();
                    client = new WeaviateClient(new Config(protocol, host));
                }
            }
        }
        return client;
    }

    @Override
    public String getVectorStoreType() {
        return "weaviate";
    }

    @Override
    public void createSchema(String kid, String embeddingModelName) {
        String className = vectorStoreProperties.getWeaviate().getClassname() + kid;
        if (knownClasses.contains(className)) {
            return;
        }
        // 检查类是否存在，如果不存在就创建 schema
        Result<Schema> schemaResult = getClient().schema().getter().run();
        Schema schema = schemaResult.getResult();
        boolean classExists = false;
        for (WeaviateClass weaviateClass : schema.getClasses()) {
            if (weaviateClass.getClassName().equals(className)) {
                classExists = true;
                break;
            }
        }
        if (!classExists) {
            // 类不存在，创建 schema
            WeaviateClass build = WeaviateClass.builder()
                    .className(className)
                    .vectorizer("none")
                    .properties(
                            List.of(Property.builder().name("text").dataType(Collections.singletonList("text")).build(),
                                    Property.builder().name("fid").dataType(Collections.singletonList("text")).build(),
                                    Property.builder().name("kid").dataType(Collections.singletonList("text")).build(),
                                    Property.builder().name("docId").dataType(Collections.singletonList("text")).build())
                    )
                    .build();
            Result<Boolean> createResult = getClient().schema().classCreator().withClass(build).run();
            if (createResult.hasErrors()) {
                log.error("Schema 创建失败: {}", createResult.getError());
                throw new ServiceException("Weaviate Schema 创建失败: " + createResult.getError());
            } else {
                log.info("Schema 创建成功: {}", className);
            }
        }
        knownClasses.add(className);
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
        createSchema(storeEmbeddingBo.getKid(), storeEmbeddingBo.getEmbeddingModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName());
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        log.info("向量存储条数记录: {}", chunkList.size());
        long startTime = System.currentTimeMillis();
        List<TextSegment> segments = chunkList.stream().map(TextSegment::from).toList();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        if (embeddings.size() != chunkList.size()) {
            throw new ServiceException("Embedding 返回数量与分片数量不一致");
        }
        ObjectsBatcher batcher = getClient().batch().objectsBatcher();
        for (int i = 0; i < chunkList.size(); i++) {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Embedding embedding = embeddings.get(i);
            Map<String, Object> properties = Map.of(
                    "text", text,
                    "fid", fid,
                    "kid", kid,
                    "docId", docId
            );
            float[] vectorArray = embedding.vector();
            normalize(vectorArray);
            Float[] vector = toObjectArray(vectorArray);

            batcher.withObject(WeaviateObject.builder()
                    .className(vectorStoreProperties.getWeaviate().getClassname() + kid)
                    .properties(properties).vector(vector).build());
        }
        Result<?> batchResult = batcher.run();
        if (batchResult.hasErrors()) {
            throw new ServiceException("Weaviate 批量写入失败: " + batchResult.getError());
        }
        long endTime = System.currentTimeMillis();
        log.info("向量存储完成消耗时间：" + (endTime - startTime) / 1000 + "秒");
    }


    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getKid(), queryVectorBo.getEmbeddingModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        float[] vector = queryEmbedding.vector();
        // 查询向量单位化处理
        normalize(vector);

        List<String> vectorStrings = new ArrayList<>();
        for (float v : vector) {
            vectorStrings.add(String.valueOf(v));
        }
        String vectorStr = String.join(",", vectorStrings);
        String className = vectorStoreProperties.getWeaviate().getClassname();

        // 构建 GraphQL 查询
        String graphQLQuery = String.format(
                "{\n" +
                        "  Get {\n" +
                        "    %s(nearVector: {vector: [%s]} limit: %d) {\n" +
                        "      text\n" +
                        "      fid\n" +
                        "      kid\n" +
                        "      docId\n" +
                        "      _additional {\n" +
                        "        distance\n" +
                        "        id\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}",
                className + queryVectorBo.getKid(),
                vectorStr,
                queryVectorBo.getMaxResults()
        );

        Result<GraphQLResponse> result = getClient().graphQL().raw().withQuery(graphQLQuery).run();
        List<String> resultList = new ArrayList<>();
        if (result != null && !result.hasErrors()) {
            Object data = result.getResult().getData();
            JSONObject entries = new JSONObject(data);
            Map<String, cn.hutool.json.JSONArray> entriesMap = entries.get("Get", Map.class);
            cn.hutool.json.JSONArray objects = entriesMap.get(className + queryVectorBo.getKid());
            if (objects.isEmpty()) {
                return resultList;
            }
            for (Object object : objects) {
                Map<String, String> map = (Map<String, String>) object;
                String content = map.get("text");
                resultList.add(content);
            }
            return resultList;
        } else {
            log.error("GraphQL 查询失败: {}", result.getError());
            return resultList;
        }
    }

    @Override
    public List<KnowledgeRetrievalVo> search(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getKid(), queryVectorBo.getEmbeddingModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        float[] vector = queryEmbedding.vector();
        // 查询向量单位化处理
        normalize(vector);
        List<String> vectorStrings = new ArrayList<>();
        for (float v : vector) {
            vectorStrings.add(String.valueOf(v));
        }
        String vectorStr = String.join(",", vectorStrings);
        String className = vectorStoreProperties.getWeaviate().getClassname();

        String graphQLQuery = String.format(
                "{\n" +
                "  Get {\n" +
                "    %s(nearVector: {vector: [%s]} limit: %d) {\n" +
                "      text\n" +
                "      fid\n" +
                "      docId\n" +
                "      _additional {\n" +
                "        distance\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}",
                className + queryVectorBo.getKid(),
                vectorStr,
                queryVectorBo.getMaxResults()
        );

        Result<GraphQLResponse> result = getClient().graphQL().raw().withQuery(graphQLQuery).run();
        List<org.ruoyi.domain.vo.knowledge.KnowledgeRetrievalVo> resultList = new ArrayList<>();

        if (result != null && !result.hasErrors()) {
            Object data = result.getResult().getData();
            JSONObject entries = new JSONObject(data);
            Map<String, cn.hutool.json.JSONArray> entriesMap = entries.get("Get", Map.class);
            cn.hutool.json.JSONArray objects = entriesMap.get(className + queryVectorBo.getKid());

            for (Object obj : objects) {
                Map<String, Object> map = (Map<String, Object>) obj;
                String content = (String) map.get("text");
                String docId = (String) map.get("docId");
                String fid = (String) map.get("fid");

                Map<String, Object> additional = (Map<String, Object>) map.get("_additional");
                Double distance = Double.valueOf(String.valueOf(additional.get("distance")));
                // 转换距离为得分 (Weaviate 0 是最相近，1 是最远；余弦距离下 1-dist 即为相似度)
                double score = 1.0 - distance;

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
                        .id(fid)
                        .docId(docId)
                        .content(content)
                        .score(score)
                        .sourceName(sourceName)
                        .build());
            }
        }
        return resultList;
    }

    @Override
    @SneakyThrows
    public void removeById(String id, String modelName) {
        String className = vectorStoreProperties.getWeaviate().getClassname();
        String finalClassName = className + id;
        Result<Boolean> result = getClient().schema().classDeleter().withClassName(finalClassName).run();
        knownClasses.remove(finalClassName);
        if (result.hasErrors()) {
            log.error("失败删除向量: " + result.getError());
            throw new ServiceException("失败删除向量数据!");
        } else {
            log.info("成功删除向量数据: " + result.getResult());
        }
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String className = vectorStoreProperties.getWeaviate().getClassname() + kid;
        // 构建 Where 条件
        WhereFilter whereFilter = WhereFilter.builder()
                .path("docId")
                .operator(Operator.Equal)
                .valueText(docId)
                .build();
        ObjectsBatchDeleter deleter = getClient().batch().objectsBatchDeleter();
        Result<BatchDeleteResponse> result = deleter.withClassName(className)
                .withWhere(whereFilter)
                .run();
        if (result != null && !result.hasErrors()) {
            log.info("成功删除 docId={} 的所有向量数据", docId);
        } else {
            log.error("删除失败: {}", result.getError());
        }
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String className = vectorStoreProperties.getWeaviate().getClassname() + kid;
        // 构建 Where 条件
        WhereFilter whereFilter = WhereFilter.builder()
                .path("fid")
                .operator(Operator.Equal)
                .valueText(fid)
                .build();
        ObjectsBatchDeleter deleter = getClient().batch().objectsBatchDeleter();
        Result<BatchDeleteResponse> result = deleter.withClassName(className)
                .withWhere(whereFilter)
                .run();
        if (result != null && !result.hasErrors()) {
            log.info("成功删除 fid={} 的所有向量数据", fid);
        } else {
            log.error("删除失败: {}", result.getError());
        }
    }

}
