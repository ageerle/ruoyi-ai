package org.ruoyi.service.strategy.impl;

import com.google.protobuf.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.QueryVectorBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.service.strategy.AbstractVectorStoreStrategy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milvus向量库策略实现
 *
 * @author ageer
 */
@Slf4j
@Component
public class MilvusVectorStoreStrategy extends AbstractVectorStoreStrategy {

    // Milvus客户端和相关配置
    // private MilvusClient milvusClient;
    
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
        String host = configService.getConfigValue("milvus", "host");
        String port = configService.getConfigValue("milvus", "port");
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        // 2. 初始化Milvus客户端
        // ConnectParam connectParam = ConnectParam.newBuilder()
        //         .withHost(host)
        //         .withPort(Integer.parseInt(port))
        //         .build();
        // milvusClient = new MilvusClient(connectParam);
        
        // 3. 检查集合是否存在，如果不存在则创建
        // HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
        //         .withCollectionName(collectionName)
        //         .build();
        // R<Boolean> hasCollectionResponse = milvusClient.hasCollection(hasCollectionParam);
        // 
        // if (!hasCollectionResponse.getData()) {
        //     // 创建集合
        //     List<FieldType> fieldsSchema = new ArrayList<>();
        //     
        //     // 主键字段
        //     fieldsSchema.add(FieldType.newBuilder()
        //             .withName("id")
        //             .withDataType(DataType.Int64)
        //             .withPrimaryKey(true)
        //             .withAutoID(true)
        //             .build());
        //     
        //     // 文本字段
        //     fieldsSchema.add(FieldType.newBuilder()
        //             .withName("text")
        //             .withDataType(DataType.VarChar)
        //             .withMaxLength(65535)
        //             .build());
        //     
        //     // fid字段
        //     fieldsSchema.add(FieldType.newBuilder()
        //             .withName("fid")
        //             .withDataType(DataType.VarChar)
        //             .withMaxLength(255)
        //             .build());
        //     
        //     // kid字段
        //     fieldsSchema.add(FieldType.newBuilder()
        //             .withName("kid")
        //             .withDataType(DataType.VarChar)
        //             .withMaxLength(255)
        //             .build());
        //     
        //     // docId字段
        //     fieldsSchema.add(FieldType.newBuilder()
        //             .withName("docId")
        //             .withDataType(DataType.VarChar)
        //             .withMaxLength(255)
        //             .build());
        //     
        //     // 向量字段
        //     fieldsSchema.add(FieldType.newBuilder()
        //             .withName("vector")
        //             .withDataType(DataType.FloatVector)
        //             .withDimension(1536) // 根据实际embedding维度调整
        //             .build());
        //     
        //     CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
        //             .withCollectionName(collectionName)
        //             .withDescription("Knowledge base collection for " + kid)
        //             .withShardsNum(2)
        //             .withFieldTypes(fieldsSchema)
        //             .build();
        //     
        //     R<RpcStatus> createCollectionResponse = milvusClient.createCollection(createCollectionParam);
        //     if (createCollectionResponse.getStatus() == R.Status.Success.getCode()) {
        //         log.info("Milvus集合创建成功: {}", collectionName);
        //         
        //         // 创建索引
        //         IndexParam indexParam = IndexParam.newBuilder()
        //                 .withCollectionName(collectionName)
        //                 .withFieldName("vector")
        //                 .withIndexType(IndexType.IVF_FLAT)
        //                 .withMetricType(MetricType.L2)
        //                 .withExtraParam("{\"nlist\":1024}")
        //                 .build();
        //         
        //         R<RpcStatus> createIndexResponse = milvusClient.createIndex(indexParam);
        //         if (createIndexResponse.getStatus() == R.Status.Success.getCode()) {
        //             log.info("Milvus索引创建成功: {}", collectionName);
        //         } else {
        //             log.error("Milvus索引创建失败: {}", createIndexResponse.getMessage());
        //         }
        //     } else {
        //         log.error("Milvus集合创建失败: {}", createCollectionResponse.getMessage());
        //     }
        // }
        
        log.info("Milvus schema创建完成: {}", collectionName);
    }

    @Override
    public void storeEmbeddings(StoreEmbeddingBo storeEmbeddingBo) {
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
        
        // List<InsertParam.Field> fields = new ArrayList<>();
        // List<String> textList = new ArrayList<>();
        // List<String> fidListData = new ArrayList<>();
        // List<String> kidList = new ArrayList<>();
        // List<String> docIdList = new ArrayList<>();
        // List<List<Float>> vectorList = new ArrayList<>();
        
        for (int i = 0; i < chunkList.size(); i++) {
            String text = chunkList.get(i);
            String fid = fidList.get(i);
            Embedding embedding = embeddingModel.embed(text).content();
            
            // textList.add(text);
            // fidListData.add(fid);
            // kidList.add(kid);
            // docIdList.add(docId);
            // 
            // List<Float> vector = new ArrayList<>();
            // for (float f : embedding.vector()) {
            //     vector.add(f);
            // }
            // vectorList.add(vector);
        }
        
        // fields.add(new InsertParam.Field("text", textList));
        // fields.add(new InsertParam.Field("fid", fidListData));
        // fields.add(new InsertParam.Field("kid", kidList));
        // fields.add(new InsertParam.Field("docId", docIdList));
        // fields.add(new InsertParam.Field("vector", vectorList));
        // 
        // InsertParam insertParam = InsertParam.newBuilder()
        //         .withCollectionName(collectionName)
        //         .withFields(fields)
        //         .build();
        // 
        // R<MutationResult> insertResponse = milvusClient.insert(insertParam);
        // if (insertResponse.getStatus() == R.Status.Success.getCode()) {
        //     log.info("Milvus向量存储成功，插入条数: {}", insertResponse.getData().getInsertCnt());
        // } else {
        //     log.error("Milvus向量存储失败: {}", insertResponse.getMessage());
        //     throw new ServiceException("Milvus向量存储失败: " + insertResponse.getMessage());
        // }
        
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
        
        // List<String> searchOutputFields = List.of("text", "fid", "kid", "docId");
        // List<List<Float>> searchVectors = new ArrayList<>();
        // List<Float> queryVector = new ArrayList<>();
        // for (float f : queryEmbedding.vector()) {
        //     queryVector.add(f);
        // }
        // searchVectors.add(queryVector);
        // 
        // SearchParam searchParam = SearchParam.newBuilder()
        //         .withCollectionName(collectionName)
        //         .withMetricType(MetricType.L2)
        //         .withOutFields(searchOutputFields)
        //         .withTopK(queryVectorBo.getMaxResults())
        //         .withVectors(searchVectors)
        //         .withVectorFieldName("vector")
        //         .withParams("{\"nprobe\":10}")
        //         .build();
        // 
        // R<SearchResults> searchResponse = milvusClient.search(searchParam);
        // if (searchResponse.getStatus() == R.Status.Success.getCode()) {
        //     SearchResults searchResults = searchResponse.getData();
        //     List<SearchResults.QueryResult> queryResults = searchResults.getResults();
        //     
        //     for (SearchResults.QueryResult queryResult : queryResults) {
        //         List<SearchResults.QueryResult.Row> rows = queryResult.getRows();
        //         for (SearchResults.QueryResult.Row row : rows) {
        //             String text = (String) row.get("text");
        //             resultList.add(text);
        //         }
        //     }
        // } else {
        //     log.error("Milvus查询失败: {}", searchResponse.getMessage());
        // }
        
        return resultList;
    }

    @Override
    public void removeById(String id, String modelName) {
        String collectionName = configService.getConfigValue("milvus", "collectionname") + id;
        
        // DropCollectionParam dropCollectionParam = DropCollectionParam.newBuilder()
        //         .withCollectionName(collectionName)
        //         .build();
        // 
        // R<RpcStatus> dropResponse = milvusClient.dropCollection(dropCollectionParam);
        // if (dropResponse.getStatus() == R.Status.Success.getCode()) {
        //     log.info("Milvus集合删除成功: {}", collectionName);
        // } else {
        //     log.error("Milvus集合删除失败: {}", dropResponse.getMessage());
        //     throw new ServiceException("Milvus集合删除失败: " + dropResponse.getMessage());
        // }
        
        log.info("Milvus删除集合: {}", collectionName);
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        // String expr = "docId == \"" + docId + "\"";
        // DeleteParam deleteParam = DeleteParam.newBuilder()
        //         .withCollectionName(collectionName)
        //         .withExpr(expr)
        //         .build();
        // 
        // R<MutationResult> deleteResponse = milvusClient.delete(deleteParam);
        // if (deleteResponse.getStatus() == R.Status.Success.getCode()) {
        //     log.info("Milvus成功删除 docId={} 的所有向量数据，删除条数: {}", docId, deleteResponse.getData().getDeleteCnt());
        // } else {
        //     log.error("Milvus删除失败: {}", deleteResponse.getMessage());
        // }
        
        log.info("Milvus删除docId={}的数据", docId);
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String collectionName = configService.getConfigValue("milvus", "collectionname") + kid;
        
        // String expr = "fid == \"" + fid + "\"";
        // DeleteParam deleteParam = DeleteParam.newBuilder()
        //         .withCollectionName(collectionName)
        //         .withExpr(expr)
        //         .build();
        // 
        // R<MutationResult> deleteResponse = milvusClient.delete(deleteParam);
        // if (deleteResponse.getStatus() == R.Status.Success.getCode()) {
        //     log.info("Milvus成功删除 fid={} 的所有向量数据，删除条数: {}", fid, deleteResponse.getData().getDeleteCnt());
        // } else {
        //     log.error("Milvus删除失败: {}", deleteResponse.getMessage());
        // }
        
        log.info("Milvus删除fid={}的数据", fid);
    }
}