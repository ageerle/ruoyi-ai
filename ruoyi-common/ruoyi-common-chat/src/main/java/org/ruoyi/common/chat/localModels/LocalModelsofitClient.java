package org.ruoyi.common.chat.localModels;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.ruoyi.common.chat.entity.models.LocalModelsSearchRequest;
import org.ruoyi.common.chat.entity.models.LocalModelsSearchResponse;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Service
public class LocalModelsofitClient {
    private static final String BASE_URL = "http://127.0.0.1:5000"; // Flask 服务的 URL
    private static Retrofit retrofit = null;

    // 获取 Retrofit 实例
    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create()) // 使用 Jackson 处理 JSON 转换
                    .build();
        }
        return retrofit;
    }

    /**
     * 向 Flask 服务发送文本向量化请求
     *
     * @param queries 查询文本列表
     * @param modelName 模型名称
     * @param delimiter 文本分隔符
     * @param topK 返回的结果数
     * @param blockSize 文本块大小
     * @param overlapChars 重叠字符数
     * @return 返回计算得到的 Top K 嵌入向量列表
     */

    public static List<List<Double>> getTopKEmbeddings(
            List<String> queries,
            String modelName,
            String delimiter,
            int topK,
            int blockSize,
            int overlapChars) {

        modelName = (!StringUtils.isEmpty(modelName)) ? modelName : "msmarco-distilbert-base-tas-b"; // 默认模型名称
        delimiter = (!StringUtils.isEmpty(delimiter) ) ? delimiter : ".";                             // 默认分隔符
        topK = (topK > 0) ? topK : 3;                                                  // 默认返回 3 个结果
        blockSize = (blockSize > 0) ? blockSize : 500;                                 // 默认文本块大小为 500
        overlapChars = (overlapChars > 0) ? overlapChars : 50;                         // 默认重叠字符数为 50

        // 创建 Retrofit 实例
        Retrofit retrofit = getRetrofitInstance();

        // 创建 SearchService 接口
        SearchService service = retrofit.create(SearchService.class);

        // 创建请求对象 LocalModelsSearchRequest
        LocalModelsSearchRequest request = new LocalModelsSearchRequest(
                queries,            // 查询文本列表
                modelName,          // 模型名称
                delimiter,          // 文本分隔符
                topK,               // 返回的结果数
                blockSize,          // 文本块大小
                overlapChars        // 重叠字符数
        );

        final CountDownLatch latch = new CountDownLatch(1);  // 创建一个 CountDownLatch
        final List<List<Double>>[] topKEmbeddings = new List[]{null}; // 使用数组来存储结果（因为 Java 不支持直接修改 List）

        // 发起异步请求
        service.vectorize(request).enqueue(new Callback<LocalModelsSearchResponse>() {
            @Override
            public void onResponse(Call<LocalModelsSearchResponse> call, Response<LocalModelsSearchResponse> response) {
                if (response.isSuccessful()) {
                    LocalModelsSearchResponse searchResponse = response.body();
                    if (searchResponse != null) {
                        topKEmbeddings[0] = searchResponse.getTopKEmbeddings().get(0);  // 获取结果
                        log.info("Successfully retrieved embeddings");
                    } else {
                        log.error("Response body is null");
                    }
                } else {
                    log.error("Request failed. HTTP error code: " + response.code());
                }
                latch.countDown();  // 请求完成，减少计数
            }

            @Override
            public void onFailure(Call<LocalModelsSearchResponse> call, Throwable t) {
                t.printStackTrace();
                log.error("Request failed: ", t);
                latch.countDown();  // 请求失败，减少计数
            }
        });

        try {
            latch.await();  // 等待请求完成
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return topKEmbeddings[0];  // 返回结果
    }

//    public static void main(String[] args) {
//        // 示例调用
//        List<String> queries = Arrays.asList("What is artificial intelligence?", "AI is transforming industries.");
//        String modelName = "msmarco-distilbert-base-tas-b";
//        String delimiter = ".";
//        int topK = 3;
//        int blockSize = 500;
//        int overlapChars = 50;
//
//        List<List<Double>> topKEmbeddings = getTopKEmbeddings(queries, modelName, delimiter, topK, blockSize, overlapChars);
//
//        // 打印结果
//        if (topKEmbeddings != null) {
//            System.out.println("Top K embeddings: ");
//            for (List<Double> embedding : topKEmbeddings) {
//                System.out.println(embedding);
//            }
//        } else {
//            System.out.println("No embeddings returned.");
//        }
//    }


//    public static void main(String[] args) {
//        // 创建 Retrofit 实例
//        Retrofit retrofit = LocalModelsofitClient.getRetrofitInstance();
//
//        // 创建 SearchService 接口
//        SearchService service = retrofit.create(SearchService.class);
//
//        // 创建请求对象 LocalModelsSearchRequest
//        LocalModelsSearchRequest request = new LocalModelsSearchRequest(
//                Arrays.asList("What is artificial intelligence?", "AI is transforming industries."), // 查询文本列表
//                "msmarco-distilbert-base-tas-b",  // 模型名称
//                ".",  // 分隔符
//                3,  // 返回的结果数
//                500,  // 文本块大小
//                50  // 重叠字符数
//        );
//
//        // 发起请求
//        service.vectorize(request).enqueue(new Callback<LocalModelsSearchResponse>() {
//            @Override
//            public void onResponse(Call<LocalModelsSearchResponse> call, Response<LocalModelsSearchResponse> response) {
//                if (response.isSuccessful()) {
//                    LocalModelsSearchResponse searchResponse = response.body();
//                    System.out.println("Response Body: " + response.body());  // Print the whole response body for debugging
//
//                    if (searchResponse != null) {
//                        // If the response is not null, process it.
//                        // Example: Extract the embeddings and print them
//                        List<List<List<Double>>> topKEmbeddings = searchResponse.getTopKEmbeddings();
//                        if (topKEmbeddings != null) {
//                            // Print the Top K embeddings
//
//                        } else {
//                            System.err.println("Top K embeddings are null");
//                        }
//
//                        // If there is more information you want to process, handle it here
//
//                    } else {
//                        System.err.println("Response body is null");
//                    }
//                } else {
//                    System.err.println("Request failed. HTTP error code: " + response.code());
//                    log.error("Failed to retrieve data. HTTP error code: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<LocalModelsSearchResponse> call, Throwable t) {
//                // 请求失败，打印错误
//                t.printStackTrace();
//                log.error("Request failed: ", t);
//            }
//        });
//    }

}
