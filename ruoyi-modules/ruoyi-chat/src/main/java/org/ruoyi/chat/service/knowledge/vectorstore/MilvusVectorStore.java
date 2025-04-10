package org.ruoyi.chat.service.knowledge.vectorstore;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeIndexResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.param.partition.CreatePartitionParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.service.ConfigService;

import org.ruoyi.service.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class MilvusVectorStore implements VectorStoreService {

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
    public void init() {
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
        String fullCollectionName = collectionName + kid;

        // 检查集合是否存在
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(fullCollectionName)
                .build();
        R<Boolean> booleanR = milvusServiceClient.hasCollection(hasCollectionParam);

        if (booleanR.getStatus() == R.Status.Success.getCode()) {
            boolean collectionExists = booleanR.getData().booleanValue();
            if (!collectionExists) {
                // 集合不存在，创建集合
                List<FieldType> fieldTypes = new ArrayList<>();
                // 假设这里定义 id 字段，根据实际情况修改
                FieldType idField = FieldType.newBuilder()
                        .withName("id")
                        .withDataType(DataType.Int64)
                        .withPrimaryKey(true)
                        .withAutoID(true)
                        .build();
                fieldTypes.add(idField);

                // 定义向量字段
                FieldType vectorField = FieldType.newBuilder()
                        .withName("fv")
                        .withDataType(DataType.FloatVector)
                        .withDimension(vectorList.get(0).size())
                        .build();
                fieldTypes.add(vectorField);

                // 定义其他字段
                FieldType contentField = FieldType.newBuilder()
                        .withName("content")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(chunkList.size() * 1024) // 根据实际情况修改
                        .build();
                fieldTypes.add(contentField);

                FieldType kidField = FieldType.newBuilder()
                        .withName("kid")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(256) // 根据实际情况修改
                        .build();
                fieldTypes.add(kidField);

                FieldType docIdField = FieldType.newBuilder()
                        .withName("docId")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(256) // 根据实际情况修改
                        .build();
                fieldTypes.add(docIdField);

                FieldType fidField = FieldType.newBuilder()
                        .withName("fid")
                        .withDataType(DataType.VarChar)
                        .withMaxLength(256) // 根据实际情况修改
                        .build();
                fieldTypes.add(fidField);

                CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(fullCollectionName)
                        .withFieldTypes(fieldTypes)
                        .build();

                R<RpcStatus> collection = milvusServiceClient.createCollection(createCollectionParam);
                if (collection.getStatus() == R.Status.Success.getCode()) {
                    System.out.println("集合 " + fullCollectionName + " 创建成功");

                    // 创建索引
                    CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                            .withCollectionName(fullCollectionName)
                            .withFieldName("fv") // 向量字段名
                            .withIndexType(IndexType.IVF_FLAT) // 索引类型
                            .withMetricType(MetricType.IP)
                            .withExtraParam("{\"nlist\":1024}") // 索引参数
                            .build();
                    R<RpcStatus> indexResponse = milvusServiceClient.createIndex(createIndexParam);
                    if (indexResponse.getStatus() == R.Status.Success.getCode()) {
                        System.out.println("索引创建成功");
                    } else {
                        System.err.println("索引创建失败: " + indexResponse.getMessage());
                        return;
                    }
                } else {
                    System.err.println("集合创建失败: " + collection.getMessage());
                    return;
                }
            }
        } else {
            System.err.println("检查集合是否存在时出错: " + booleanR.getMessage());
            return;
        }

        if (StringUtils.isNotBlank(docId)) {
            milvusServiceClient.createPartition(
                    CreatePartitionParam.newBuilder()
                            .withCollectionName(fullCollectionName)
                            .withPartitionName(docId)
                            .build()
            );
        }

        List<List<Float>> vectorFloatList = new ArrayList<>();
        List<String> kidList = new ArrayList<>();
        List<String> docIdList = new ArrayList<>();
        for (int i = 0; i < Math.min(chunkList.size(), vectorList.size()); i++) {
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
                .withCollectionName(fullCollectionName)
                .withPartitionName(docId)
                .withFields(fields)
                .build();
        System.out.println("=========================");

        R<MutationResult> insert = milvusServiceClient.insert(insertParam);
        if (insert.getStatus() == R.Status.Success.getCode()) {
            System.out.println("插入成功，插入的行数: " + insert.getData().getInsertCnt());
        } else {
            System.err.println("插入失败: " + insert.getMessage());
        }
        System.out.println("=========================");
        // milvus在将数据装载到内存后才能进行向量计算.
        LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                .withCollectionName(fullCollectionName)
                .build();
        R<RpcStatus> loadResponse = milvusServiceClient.loadCollection(loadCollectionParam);
        if (loadResponse.getStatus() != R.Status.Success.getCode()) {
            System.err.println("加载集合 " + fullCollectionName + " 到内存时出错：" + loadResponse.getMessage());
        }
//        milvusServiceClient.loadCollection(LoadCollectionParam.newBuilder().withCollectionName(fullCollectionName).build());
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
        String fullCollectionName = collectionName + kid;

        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(fullCollectionName)
                .build();

        R<Boolean> booleanR = milvusServiceClient.hasCollection(hasCollectionParam);
        if (booleanR.getStatus() != R.Status.Success.getCode() || !booleanR.getData().booleanValue()) {
            System.err.println("集合 " + fullCollectionName + " 不存在或检查集合存在性时出错。");
            return new ArrayList<>();
        }

        DescribeIndexParam describeIndexParam = DescribeIndexParam.newBuilder().withCollectionName(fullCollectionName).build();

        R<DescribeIndexResponse> describeIndexResponseR = milvusServiceClient.describeIndex(describeIndexParam);

        if (describeIndexResponseR.getStatus() == R.Status.Success.getCode()) {
            System.out.println("索引信息: " + describeIndexResponseR.getData().getIndexDescriptionsCount());
        } else {
            System.err.println("获取索引失败: " + describeIndexResponseR.getMessage());
        }

        List<String> search_output_fields = Arrays.asList("content", "fv");
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
        System.out.println("SearchParam: " + searchParam.toString());
        R<SearchResults> respSearch = milvusServiceClient.search(searchParam);
        if (respSearch.getStatus() == R.Status.Success.getCode()) {
            SearchResults searchResults = respSearch.getData();
            if (searchResults != null) {
                System.out.println(searchResults.getResults());
                SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(searchResults.getResults());
                List<QueryResultsWrapper.RowRecord> rowRecords = wrapperSearch.getRowRecords();

                List<String> resultList = new ArrayList<>();
                if (rowRecords != null && !rowRecords.isEmpty()) {
                    for (QueryResultsWrapper.RowRecord rowRecord : rowRecords) {
                        String content = rowRecord.get("content").toString();
                        resultList.add(content);
                    }
                }
                return resultList;
            } else {
                System.err.println("搜索结果为空");
            }
        } else {
            System.err.println("搜索操作失败: " + respSearch.getMessage());
        }
        return new ArrayList<>();

    }

    /**
     * milvus 不支持通过文本检索相似性
     *
     * @param query
     * @param kid
     * @return
     */
    @Override
    public List<String> nearest(String query, String kid) {
        return null;
    }

}
