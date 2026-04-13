package org.ruoyi.service.knowledge.rerank;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.json.utils.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;

/**
 * SiliconFlow 重排模型实现
 * 适配硅基流动的 /v1/rerank 接口
 */
@Slf4j
public class SiliconFlowScoringModel implements ScoringModel {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final OkHttpClient client;

    @Builder
    public SiliconFlowScoringModel(String apiKey, String modelName, String baseUrl) {
        if (isNullOrEmpty(apiKey)) {
            throw new IllegalArgumentException("SiliconFlow API Key 不能为空");
        }
        this.apiKey = apiKey;
        this.modelName = isNullOrEmpty(modelName) ? "BAAI/bge-reranker-v2-m3" : modelName;

        // 鲁棒性处理：自动补全 /rerank 路径
        String finalUrl = baseUrl;
        if (isNullOrEmpty(finalUrl)) {
            finalUrl = "https://api.siliconflow.cn/v1/rerank";
        } else {
            // 如果用户只填了基础路径 https://api.siliconflow.cn/v1，自动补全成 https://api.siliconflow.cn/v1/rerank
            if (finalUrl.endsWith("/v1")) {
                finalUrl = finalUrl + "/rerank";
            } else if (!finalUrl.endsWith("/rerank")) {
                // 如果没有以 /rerank 结尾也不以斜杠结尾，尝试拼接
                finalUrl = finalUrl.endsWith("/") ? finalUrl + "rerank" : finalUrl + "/rerank";
            }
        }
        this.baseUrl = finalUrl;
        log.info("初始化 SiliconFlow 重排模型: URL=[{}], Model=[{}]", this.baseUrl, this.modelName);

        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        if (isNullOrEmpty(segments)) {
            return Response.from(new ArrayList<>());
        }

        List<String> texts = segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList());

        RerankRequest requestBody = new RerankRequest();
        requestBody.setModel(modelName);
        requestBody.setQuery(query);
        requestBody.setDocuments(texts);
        requestBody.setTop_n(texts.size());
        requestBody.setReturn_documents(false);

        String json = JsonUtils.toJsonString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "unknown error";
                log.error("SiliconFlow Rerank API 调用失败: code={}, body={}", response.code(), errorBody);
                throw new RuntimeException("SiliconFlow Rerank API 调用失败: " + response.code());
            }

            String responseBody = response.body().string();
            RerankResponse rerankResponse = JsonUtils.parseObject(responseBody, RerankResponse.class);

            if (rerankResponse == null || rerankResponse.getResults() == null) {
                return Response.from(new ArrayList<>());
            }

            // 初始化分数组，默认值为 0.0
            Double[] scores = new Double[texts.size()];
            for (int i = 0; i < texts.size(); i++) {
                scores[i] = 0.0;
            }

            // 填充得分
            rerankResponse.getResults().forEach(item -> {
                if (item.getIndex() != null && item.getIndex() < texts.size()) {
                    scores[item.getIndex()] = item.getRelevance_score();
                }
            });

            List<Double> scoreList = new ArrayList<>();
            for (Double s : scores) {
                scoreList.add(s);
            }

            return Response.from(scoreList);

        } catch (IOException e) {
            log.error("SiliconFlow Rerank 网络请求异常", e);
            throw new RuntimeException("SiliconFlow Rerank 网络请求异常", e);
        }
    }

    @Override
    public Response<Double> score(TextSegment segment, String query) {
        List<TextSegment> segments = new ArrayList<>();
        segments.add(segment);
        Response<List<Double>> response = scoreAll(segments, query);
        return Response.from(response.content().get(0));
    }

    @Data
    public static class RerankRequest {
        private String model;
        private String query;
        private List<String> documents;
        private Integer top_n;
        private Boolean return_documents;
    }

    @Data
    public static class RerankResponse {
        private List<RerankResultItem> results;
    }

    @Data
    public static class RerankResultItem {
        private Integer index;
        private Double relevance_score;
    }
}
