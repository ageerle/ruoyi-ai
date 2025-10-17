package org.ruoyi.service.strategy.impl;

import org.ruoyi.common.core.exception.ServiceException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.response.DescCollResponseWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.config.VectorStoreProperties;
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

    private MilvusServiceClient milvusClient;

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
        
        // 创建Milvus客户端连接
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUri(url)
                .build();
        milvusClient = new MilvusServiceClient(connectParam);

        // 检查集合是否存在
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        
        R<Boolean> hasCollectionResponse = milvusClient.hasCollection(hasCollectionParam);
        if (hasCollectionResponse.getStatus() != R.Status.Success.getCode()) {
            log.error("检查集合是否存在失败: {}", hasCollectionResponse.getMessage());
            return;
        }
        
        if (!hasCollectionResponse.getData()) {
            // 创建字段
            List<FieldType> fields = new ArrayList<>();
            
            // ID字段 (主键)
            fields.add(FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(true)
                    .build());
            
            // 文本字段
            fields.add(FieldType.newBuilder()
                    .withName("text")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build());
            
            // fid字段
            fields.add(FieldType.newBuilder()
                    .withName("fid")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(255)
                    .build());
            
            // kid字段
            fields.add(FieldType.newBuilder()
                    .withName("kid")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(255)
                    .build());
            
            // docId字段
            fields.add(FieldType.newBuilder()
                    .withName("docId")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(255)
                    .build());
            
            // 向量字段
            fields.add(FieldType.newBuilder()
                    .withName("vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(2048) // 根据实际embedding维度调整
                    .build());

            // 创建集合
            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("Knowledge base collection for " + kid)
                    .withShardsNum(2)
                    .withFieldTypes(fields)
                    .build();

            R<RpcStatus> createCollectionResponse = milvusClient.createCollection(createCollectionParam);
            if (createCollectionResponse.getStatus() != R.Status.Success.getCode()) {
                log.error("创建集合失败: {}", createCollectionResponse.getMessage());
                return;
            }

            // 创建索引
            CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.L2)
                    .withExtraParam("{\"nlist\":1024}")
                    .build();

            R<RpcStatus> createIndexResponse = milvusClient.createIndex(createIndexParam);
            if (createIndexResponse.getStatus() != R.Status.Success.getCode()) {
                log.error("创建索引失败: {}", createIndexResponse.getMessage());
            } else {
                log.info("Milvus集合和索引创建成功: {}", collectionName);
            }
        } else {
            log.info("Milvus集合已存在: {}", collectionName);
        }
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
        
        log.info("Milvus向量存储条数记录: " + chunkList.size());
        long startTime = System.currentTimeMillis();
        
        // 准备批量插入数据
        List<InsertParam.Field> fields = new ArrayList<>();
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
        
        // 构建字段数据
        fields.add(new InsertParam.Field("text", textList));
        fields.add(new InsertParam.Field("fid", fidListData));
        fields.add(new InsertParam.Field("kid", kidList));
        fields.add(new InsertParam.Field("docId", docIdList));
        fields.add(new InsertParam.Field("vector", vectorList));
        
        // 执行插入
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();
        
        R<MutationResult> insertResponse = milvusClient.insert(insertParam);
        if (insertResponse.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus向量存储失败: {}", insertResponse.getMessage());
            throw new ServiceException("Milvus向量存储失败");
        } else {
            log.info("Milvus向量存储成功，插入条数: {}", insertResponse.getData().getInsertCnt());
        }
        
        long endTime = System.currentTimeMillis();
        log.info("Milvus向量存储完成消耗时间：" + (endTime - startTime) / 1000 + "秒");
    }

    @Override
    public List<String> getQueryVector(QueryVectorBo queryVectorBo) {
        createSchema(queryVectorBo.getVectorModelName(), queryVectorBo.getKid());
        
        EmbeddingModel embeddingModel = getEmbeddingModel(queryVectorBo.getEmbeddingModelName(),
                queryVectorBo.getApiKey(), queryVectorBo.getBaseUrl());
        
        Embedding queryEmbedding = embeddingModel.embed(queryVectorBo.getQuery()).content();
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + queryVectorBo.getKid();
        
        List<String> resultList = new ArrayList<>();
        
        // 加载集合到内存
        LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        milvusClient.loadCollection(loadCollectionParam);
        
        // 准备查询向量
        List<List<Float>> searchVectors = new ArrayList<>();
        List<Float> queryVector = new ArrayList<>();
        for (float f : queryEmbedding.vector()) {
            queryVector.add(f);
        }
        searchVectors.add(queryVector);
        
        // 构建搜索参数
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                // 匹配方法
                .withMetricType(MetricType.L2)
                .withOutFields(Arrays.asList("text", "fid", "kid", "docId"))
                .withTopK(queryVectorBo.getMaxResults())
                .withVectors(searchVectors)
                .withVectorFieldName("vector")
                .withParams("{\"nprobe\":10}")
                .build();
        
        R<SearchResults> searchResponse = milvusClient.search(searchParam);
        if (searchResponse.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus查询失败: {}", searchResponse.getMessage());
            return resultList;
        }
        
        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
        
        // 遍历搜索结果
        for (int i = 0; i < wrapper.getIDScore(0).size(); i++) {
            SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0).get(i);
            
            // 获取text字段数据
            List<?> textFieldData = wrapper.getFieldData("text", 0);
            if (textFieldData != null && i < textFieldData.size()) {
                Object textObj = textFieldData.get(i);
                if (textObj != null) {
                    resultList.add(textObj.toString());
                    log.debug("找到相似文本，ID: {}, 距离: {}, 内容: {}", 
                            idScore.getLongID(), idScore.getScore(), textObj.toString());
                }
            }
        }
        
        return resultList;
    }

    @Override
    @SneakyThrows
    public void removeById(String id, String modelName) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + id;
        
        // 删除整个集合
        DropCollectionParam dropCollectionParam = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        
        R<RpcStatus> dropResponse = milvusClient.dropCollection(dropCollectionParam);
        if (dropResponse.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus集合删除失败: {}", dropResponse.getMessage());
            throw new ServiceException("Milvus集合删除失败");
        } else {
            log.info("Milvus集合删除成功: {}", collectionName);
        }
    }

    @Override
    public void removeByDocId(String docId, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        
        String expr = "docId == \"" + docId + "\"";
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();
        
        R<MutationResult> deleteResponse = milvusClient.delete(deleteParam);
        if (deleteResponse.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus删除失败: {}", deleteResponse.getMessage());
            throw new ServiceException("Milvus删除失败");
        } else {
            log.info("Milvus成功删除 docId={} 的所有向量数据，删除条数: {}", docId, deleteResponse.getData().getDeleteCnt());
        }
    }

    @Override
    public void removeByFid(String fid, String kid) {
        String collectionName = vectorStoreProperties.getMilvus().getCollectionname() + kid;
        
        String expr = "fid == \"" + fid + "\"";
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();
        
        R<MutationResult> deleteResponse = milvusClient.delete(deleteParam);
        if (deleteResponse.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus删除失败: {}", deleteResponse.getMessage());
            throw new ServiceException("Milvus删除失败");
        } else {
            log.info("Milvus成功删除 fid={} 的所有向量数据，删除条数: {}", fid, deleteResponse.getData().getDeleteCnt());
        }
    }
}