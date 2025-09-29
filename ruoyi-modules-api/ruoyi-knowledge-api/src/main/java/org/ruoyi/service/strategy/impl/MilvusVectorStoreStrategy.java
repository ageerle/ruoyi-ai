package org.ruoyi.service.strategy.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.strategy.AbstractVectorStoreStrategy;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Milvus向量库策略实现
 *
 * @author ageer
 */
@Slf4j
@Component
public class MilvusVectorStoreStrategy extends AbstractVectorStoreStrategy {

    private MilvusClientV2 client;
    
    public MilvusVectorStoreStrategy(ConfigService configService) {
        super(configService);
    }

    @Override
    public String getVectorStoreType() {
        return "milvus";
    }
    
    @Override
    public void createSchema(String vectorModelName, String kid, String modelName) {
        log.info("Milvus创建schema: vectorModelName={}, kid={}, modelName={}", vectorModelName, kid, modelName);
        
        // 1. 获取Milvus配置
        String host = configService.getConfigValue("milvus", "url");
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        ConnectConfig config = ConnectConfig.builder()
                .uri(host)
                .build();
        client = new MilvusClientV2(config);

        // 2. 检查集合是否存在
        HasCollectionReq hasCollectionReq = HasCollectionReq.builder()
                .collectionName(collectionName)
                .build();
        
        Boolean hasCollection = client.hasCollection(hasCollectionReq);
        
        if (!hasCollection) {
            // 3. 创建集合schema
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                    .build();
            
            // 添加字段定义
            schema.addField(AddFieldReq.builder()
                    .fieldName("id")
                    .dataType(DataType.Int64)
                    .isPrimaryKey(true)
                    .autoID(true)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("text")
                    .dataType(DataType.VarChar)
                    .maxLength(65535)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("fid")
                    .dataType(DataType.VarChar)
                    .maxLength(255)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("kid")
                    .dataType(DataType.VarChar)
                    .maxLength(255)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("docId")
                    .dataType(DataType.VarChar)
                    .maxLength(255)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("vector")
                    .dataType(DataType.FloatVector)
                    .dimension(1024) // 根据实际embedding维度调整
                    .build());

            // 4. 创建索引参数
            List<IndexParam> indexParams = new ArrayList<>();
            indexParams.add(IndexParam.builder()
                    .fieldName("vector")
                    .indexType(IndexParam.IndexType.IVF_FLAT)
                    .metricType(IndexParam.MetricType.L2)
                    .extraParams(Map.of("nlist", 1024))
                    .build());

            // 5. 创建集合
            CreateCollectionReq createCollectionReq = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexParams)
                    .build();

            client.createCollection(createCollectionReq);
            log.info("Milvus集合创建成功: {}", collectionName);
        } else {
            log.info("Milvus集合已存在: {}", collectionName);
        }
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) throws ServiceException {
        createSchema(storeEmbeddingBo.getVectorModelName(), storeEmbeddingBo.getKid(), storeEmbeddingBo.getVectorModelName());
        
        EmbeddingModel embeddingModel = getEmbeddingModel(storeEmbeddingBo.getEmbeddingModelName(),
                storeEmbeddingBo.getApiKey(), storeEmbeddingBo.getBaseUrl());
        
        List<String> chunkList = storeEmbeddingBo.getChunkList();
        List<String> fidList = storeEmbeddingBo.getFids();
        String kid = storeEmbeddingBo.getKid();
        String docId = storeEmbeddingBo.getDocId();
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        log.info("Milvus向量存储条数记录: " + chunkList.size());
        long startTime = System.currentTimeMillis();
        
        // 准备批量插入数据
        List<String> textList = new ArrayList<>();
        List<String> fidListData = new ArrayList<>();
        List<String> kidList = new ArrayList<>();
        List<String> docIdList = new ArrayList<>();
        List<List<Float>> vectorList = new ArrayList<>();
        
        for (int i = 0; i < chunkList.size(); i++) {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Embedding embedding = embeddingModel.embed(text).content();
            
            textList.add(text);
            fidListData.add(fid);
            kidList.add(kid);
            docIdList.add(docId);
            
            List<Float> vector = new ArrayList<>();
            for (float f : embedding.vector()) {
                vector.add(f);
            }
            vectorList.add(vector);
        }
        
        // 构建插入数据
        List<JsonObject> data = new ArrayList<>();
        Gson gson = new Gson();
        for (int i = 0; i < textList.size(); i++) {
            JsonObject row = new JsonObject();
            row.addProperty("text", textList.get(i));
            row.addProperty("fid", fidListData.get(i));
            row.addProperty("kid", kidList.get(i));
            row.addProperty("docId", docIdList.get(i));
            row.add("vector", gson.toJsonTree(vectorList.get(i)));
            data.add(row);
        }
        
        // 执行插入
        InsertReq insertReq = InsertReq.builder()
                .collectionName(collectionName)
                .data(data)
                .build();
        
        InsertResp insertResp = client.insert(insertReq);
        if (insertResp.getInsertCnt() > 0) {
            log.info("Milvus向量存储成功，插入条数: {}", insertResp.getInsertCnt());
        } else {
            log.error("Milvus向量存储失败");
            throw new ServiceException("Milvus向量存储失败");
        }
        
        long endTime = System.currentTimeMillis();
        log.info("Milvus向量存储完成消耗时间：" + (endTime - startTime) / 1000 + "秒");
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getVectorModelName(), queryVectorBo.getKid(), queryVectorBo.getVectorModelName());
        
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(),
                queryVectorBo.getApiKey(), queryVectorBo.getBaseUrl());
        
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String collectionName = configService.getConfigValue("milvus", "collectionname") + queryVectorBo.getKid();
        
        List<String> resultList = new ArrayList<>();
        
        // 准备查询向量
        List<BaseVector> searchVectors = new ArrayList<>();
        float[] queryVectorArray = new float[queryEmbedding.vector().length];
        for (int i = 0; i < queryEmbedding.vector().length; i++) {
            queryVectorArray[i] = queryEmbedding.vector()[i];
        }
        searchVectors.add(new FloatVec(queryVectorArray));
        
        // 构建搜索请求
        SearchReq searchReq = SearchReq.builder()
                .collectionName(collectionName)
                .data(searchVectors)
                .topK(queryVectorBo.getMaxResults())
                .outputFields(Arrays.asList("text", "fid", "kid", "docId"))
                .build();
        
        SearchResp searchResp = client.search(searchReq);
        if (searchResp != null && searchResp.getSearchResults() != null) {
            List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
            
            for (List<SearchResp.SearchResult> results : searchResults) {
                for (SearchResp.SearchResult result : results) {
                    Map<String, Object> entity = result.getEntity();
                    String text = (String) entity.get("text");
                    if (text != null) {
                        resultList.add(text);
                    }
                }
            }
        } else {
            log.error("Milvus查询失败或无结果");
        }
        
        return resultList;
    }

    @Override
    public void removeById(String id, String modelName) throws ServiceException {
        String collectionName = configService.getConfigValue("milvus", "collectionname") + id;
        
        // 删除整个集合
        DropCollectionReq dropCollectionReq = DropCollectionReq.builder()
                .collectionName(collectionName)
                .build();
        
        try {
            client.dropCollection(dropCollectionReq);
            log.info("Milvus集合删除成功: {}", collectionName);
        } catch (Exception e) {
            log.error("Milvus集合删除失败: {}", e.getMessage());
            throw new ServiceException("Milvus集合删除失败: " + e.getMessage());
        }
    }

    @Override
    public void removeByDocId(String docId, String kid) throws ServiceException {
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        String expr = "docId == \"" + docId + "\"";
        DeleteReq deleteReq = DeleteReq.builder()
                .collectionName(collectionName)
                .filter(expr)
                .build();
        
        try {
            DeleteResp deleteResp = client.delete(deleteReq);
            log.info("Milvus成功删除 docId={} 的所有向量数据，删除条数: {}", docId, deleteResp.getDeleteCnt());
        } catch (Exception e) {
            log.error("Milvus删除失败: {}", e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public void removeByFid(String fid, String kid) throws ServiceException {
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        String expr = "fid == \"" + fid + "\"";
        DeleteReq deleteReq = DeleteReq.builder()
                .collectionName(collectionName)
                .filter(expr)
                .build();
        
        try {
            DeleteResp deleteResp = client.delete(deleteReq);
            log.info("Milvus成功删除 fid={} 的所有向量数据，删除条数: {}", fid, deleteResp.getDeleteCnt());
        } catch (Exception e) {
            log.error("Milvus删除失败: {}", e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }
}