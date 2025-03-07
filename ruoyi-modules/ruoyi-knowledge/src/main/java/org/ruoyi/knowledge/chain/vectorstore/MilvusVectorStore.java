package org.ruoyi.knowledge.chain.vectorstore;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.service.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class MilvusVectorStore implements VectorStore{

    private volatile Integer dimension;
    private volatile String collectionName;
    private MilvusServiceClient milvusServiceClient;

    @Resource
    private ConfigService configService;

    @PostConstruct
    public void loadConfig() {
        this.dimension = Integer.parseInt(configService.getConfigValue("milvus", "dimension"));
        this.collectionName = configService.getConfigValue("milvus", "collection");
    }

    @PostConstruct
    public void init(){
        String milvusHost = configService.getConfigValue("milvus", "host");
        String milvausPort = configService.getConfigValue("milvus", "port");
        milvusServiceClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(Integer.parseInt(milvausPort))
                        .withDatabaseName("default")
                        .build()
        );
    }

    private void createSchema(String kid) {
        FieldType primaryField = FieldType.newBuilder()
                .withName("row_id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(1000)
                .build();
        FieldType kidField = FieldType.newBuilder()
                .withName("kid")
                .withDataType(DataType.VarChar)
                .withMaxLength(20)
                .build();
        FieldType docIdField = FieldType.newBuilder()
                .withName("docId")
                .withDataType(DataType.VarChar)
                .withMaxLength(20)
                .build();
        FieldType fidField = FieldType.newBuilder()
                .withName("fid")
                .withDataType(DataType.VarChar)
                .withMaxLength(20)
                .build();
        FieldType vectorField = FieldType.newBuilder()
                .withName("fv")
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();
        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName + kid)
                .withDescription("local knowledge")
                .addFieldType(primaryField)
                .addFieldType(contentField)
                .addFieldType(kidField)
                .addFieldType(docIdField)
                .addFieldType(fidField)
                .addFieldType(vectorField)
                .build();
        milvusServiceClient.createCollection(createCollectionReq);

        // 创建向量的索引
        IndexType INDEX_TYPE = IndexType.IVF_FLAT;
        String INDEX_PARAM = "{\"nlist\":1024}";
        milvusServiceClient.createIndex(
                CreateIndexParam.newBuilder()
                        .withCollectionName(collectionName + kid)
                        .withFieldName("fv")
                        .withIndexType(INDEX_TYPE)
                        .withMetricType(MetricType.IP)
                        .withExtraParam(INDEX_PARAM)
                        .withSyncMode(Boolean.FALSE)
                        .build()
        );

    }

    @Override
    public void newSchema(String kid) {
        createSchema(kid);
    }

    @Override
    public void removeByKidAndFid(String kid, String fid) {
        milvusServiceClient.delete(
                DeleteParam.newBuilder()
                        .withCollectionName(collectionName + kid)
                        .withExpr("fid == " + fid)
                        .build()
        );
    }

    @Override
    public void storeEmbeddings(List<String> chunkList, List<List<Double>> vectorList, String kid, String docId, List<String> fidList) {

        if (StringUtils.isNotBlank(docId)){
            milvusServiceClient.createPartition(
                    CreatePartitionParam.newBuilder()
                            .withCollectionName(collectionName + kid)
                            .withPartitionName(docId)
                            .build()
            );
        }

        List<List<Float>> vectorFloatList = new ArrayList<>();
        List<String> kidList = new ArrayList<>();
        List<String> docIdList = new ArrayList<>();
        for (int i = 0; i < chunkList.size(); i++) {
            List<Double> vector = vectorList.get(i);
            List<Float> vfList = new ArrayList<>();
            for (int j = 0; j < vector.size(); j++) {
                Double value = vector.get(j);
                vfList.add(value.floatValue());
            }
            vectorFloatList.add(vfList);
            kidList.add(kid);
            docIdList.add(docId);
        }
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("content", chunkList));
        fields.add(new InsertParam.Field("kid", kidList));
        fields.add(new InsertParam.Field("docId", docIdList));
        fields.add(new InsertParam.Field("fid", fidList));
        fields.add(new InsertParam.Field("fv", vectorFloatList));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName + kid)
                .withPartitionName(docId)
                .withFields(fields)
                .build();
        milvusServiceClient.insert(insertParam);
        // milvus在将数据装载到内存后才能进行向量计算
        milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder().withCollectionName(collectionName + kid).build());
    }



    @Override
    public void removeByDocId(String kid, String docId) {
        milvusServiceClient.delete(
                DeleteParam.newBuilder()
                        .withCollectionName(collectionName + kid)
                        .withExpr("1 == 1")
                        .withPartitionName(docId)
                        .build()
        );
    }

    @Override
    public void removeByKid(String kid) {
        milvusServiceClient.dropCollection(
                DropCollectionParam.newBuilder()
                        .withCollectionName(collectionName + kid)
                        .build()
        );
    }

    @Override
    public List<String> nearest(List<Double> queryVector, String kid) {
        List<String> search_output_fields = Arrays.asList("content","fv");
        List<Float> fv = new ArrayList<>();
        for (int i = 0; i < queryVector.size(); i++) {
            fv.add(queryVector.get(i).floatValue());
        }
        List<List<Float>> vectors = new ArrayList<>();
        vectors.add(fv);
        String search_param = "{\"nprobe\":10, \"offset\":0}";
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName + kid)
                .withMetricType(MetricType.IP)
                .withOutFields(search_output_fields)
                .withTopK(10)
                .withVectors(vectors)
                .withVectorFieldName("fv")
                .withParams(search_param)
                .build();
        R<SearchResults> respSearch = milvusServiceClient.search(searchParam);
        SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(respSearch.getData().getResults());
        List<QueryResultsWrapper.RowRecord> rowRecords = wrapperSearch.getRowRecords();

        List<String> resultList = new ArrayList<>();
        if (resultList!=null && resultList.size() > 0){
            for (int i = 0; i < rowRecords.size(); i++) {
                String content = rowRecords.get(i).get("content").toString();
                resultList.add(content);
            }
        }
        return resultList;
    }

    /**
     * milvus 不支持通过文本检索相似性
     * @param query
     * @param kid
     * @return
     */
    @Override
    public List<String> nearest(String query, String kid) {
        return null;
    }

}
