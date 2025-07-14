package org.ruoyi.service.impl;

import cn.hutool.json.JSONObject;
import com.google.protobuf.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.api.ObjectsBatchDeleter;
import io.weaviate.client.v1.batch.model.BatchDeleteResponse;
import io.weaviate.client.v1.filters.Operator;
import io.weaviate.client.v1.filters.WhereFilter;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.Schema;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Service;
import java.util.*;

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
    private WeaviateClient client;


    @Override
    public void createSchema(String kid, String modelName) {
        String protocol = configService.getConfigValue("weaviate", "protocol");
        String host = configService.getConfigValue("weaviate", "host");
        String className = configService.getConfigValue("weaviate", "classname")+kid;
        // 创建 Weaviate 客户端
        client= new WeaviateClient(new Config(protocol, host));
        // 检查类是否存在，如果不存在就创建 schema
        Result<Schema> schemaResult = client.schema().getter().run();
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
            Result<Boolean> createResult = client.schema().classCreator().withClass(build).run();
            if (createResult.hasErrors()) {
                log.error("Schema 创建失败: {}", createResult.getError());
            } else {
                log.info("Schema 创建成功: {}", className);
            }
        }
        embeddingStore = WeaviateEmbeddingStore.builder()
                .scheme(protocol)
                .host(host)
                .objectClass(className)
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
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        log.info("向量存储条数记录: " + chunkList.size());
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < chunkList.size(); i++) {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Embedding embedding = embeddingModel.embed(text).content();
            Map<String, Object> properties = Map.of(
                    "text", text,
                    "fid",fid,
                    "kid", kid,
                    "docId", docId
            );
            Float[] vector = toObjectArray(embedding.vector());
            client.data().creator()
                    .withClassName("LocalKnowledge" + kid) // 注意替换成实际类名
                    .withProperties(properties)
                    .withVector(vector)
                    .run();
        }
        long endTime = System.currentTimeMillis();
        log.info("向量存储完成消耗时间："+ (endTime-startTime)/1000+"秒");
    }

    private static Float[] toObjectArray(float[] primitive) {
        Float[] result = new Float[primitive.length];
        for (int i = 0; i < primitive.length; i++) {
            result[i] = primitive[i]; // 自动装箱
        }
        return result;
    }
    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getKid(), queryVectorBo.getVectorModelName());
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(),
                queryVectorBo.getApiKey(), queryVectorBo.getBaseUrl());
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        float[] vector = queryEmbedding.vector();
        List<String> vectorStrings = new ArrayList<>();
        for (float v : vector) {
            vectorStrings.add(String.valueOf(v));
        }
        String vectorStr = String.join(",", vectorStrings);
        String className = configService.getConfigValue("weaviate", "classname") ;
        // 构建 GraphQL 查询
        String graphQLQuery = String.format(
                "{\n" +
                        "  Get {\n" +
                        "    %s(nearVector: {vector: [%s], certainty: %f} limit: %d) {\n" +
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
                className+ queryVectorBo.getKid(),
                vectorStr,
                queryVectorBo.getMaxResults()
        );

        Result<GraphQLResponse> result = client.graphQL().raw().withQuery(graphQLQuery).run();
        List<String> resultList = new ArrayList<>();
        if (result != null && !result.hasErrors()) {
            Object data = result.getResult().getData();
            JSONObject entries = new JSONObject(data);
            Map<String, cn.hutool.json.JSONArray> entriesMap = entries.get("Get", Map.class);
            cn.hutool.json.JSONArray objects = entriesMap.get(className + queryVectorBo.getKid());
            if(objects.isEmpty()){
                return resultList;
            }
            for (Object object : objects) {
                Map<String, String> map = (Map<String, String>) object;
                String content = map.get("text");
                resultList.add( content);
            }
            return resultList;
        } else {
            log.error("GraphQL 查询失败: {}", result.getError());
            return resultList;
        }
    }

    @Override
    @SneakyThrows
    public void removeById(String id, String modelName) {
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

    @Override
    public void removeByDocId(String docId, String kid) {
        String className = configService.getConfigValue("weaviate", "classname") + kid;
        // 构建 Where 条件
        WhereFilter whereFilter = WhereFilter.builder()
                .path("docId")
                .operator(Operator.Equal)
                .valueText(docId)
                .build();
        ObjectsBatchDeleter deleter = client.batch().objectsBatchDeleter();
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
        String className = configService.getConfigValue("weaviate", "classname") + kid;
        // 构建 Where 条件
        WhereFilter whereFilter = WhereFilter.builder()
                .path("fid")
                .operator(Operator.Equal)
                .valueText(fid)
                .build();
        ObjectsBatchDeleter deleter = client.batch().objectsBatchDeleter();
        Result<BatchDeleteResponse> result = deleter.withClassName(className)
                .withWhere(whereFilter)
                .run();
        if (result != null && !result.hasErrors()) {
            log.info("成功删除 fid={} 的所有向量数据", fid);
        } else {
            log.error("删除失败: {}", result.getError());
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
