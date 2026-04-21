package org.ruoyi.service.rerank.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.service.rerank.RerankModelService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 硅基流动重排序模型实现
 * 适配硅基流动的 /v1/rerank 接口
 *
 * @author RobustH
 * @date 2026-04-21
 */
@Slf4j
@Component("siliconflowRerank")
public class SiliconFlowRerankModelService implements RerankModelService {

    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1/rerank";

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private ChatModelVo chatModelVo;

    public SiliconFlowRerankModelService() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    @Override
    public RerankResult rerank(RerankRequest rerankRequest) {
        long startTime = System.currentTimeMillis();

        try {
            String url = buildUrl();
            String requestJson = buildRequestJson(rerankRequest);

            RequestBody body = RequestBody.create(requestJson, MediaType.get("application/json"));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + chatModelVo.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            log.info("硅基流动重排序请求: model={}, url={}", chatModelVo.getModelName(), url);

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "无错误信息";
                    throw new IllegalArgumentException("硅基流动 Rerank API 调用失败: " + response.code() + " - " + err);
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new IllegalArgumentException("响应体为空");
                }

                SiliconFlowRerankResponse rerankResponse = objectMapper.readValue(
                        responseBody.string(), SiliconFlowRerankResponse.class);

                return buildRerankResult(rerankResponse, rerankRequest.getDocuments(),
                        System.currentTimeMillis() - startTime);
            }

        } catch (Exception e) {
            log.error("硅基流动重排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("重排序服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求 URL，鲁棒处理 API Host 末尾路径
     */
    private String buildUrl() {
        String apiHost = chatModelVo.getApiHost();
        if (apiHost == null || apiHost.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        if (apiHost.endsWith("/rerank")) {
            return apiHost;
        }
        if (apiHost.endsWith("/v1")) {
            return apiHost + "/rerank";
        }
        return apiHost.endsWith("/") ? apiHost + "rerank" : apiHost + "/rerank";
    }

    /**
     * 构建请求体 JSON
     */
    private String buildRequestJson(RerankRequest rerankRequest) throws IOException {
        SiliconFlowRerankRequest request = new SiliconFlowRerankRequest();
        request.setModel(chatModelVo.getModelName());
        request.setQuery(rerankRequest.getQuery());
        request.setDocuments(rerankRequest.getDocuments());
        request.setTop_n(rerankRequest.getTopN() != null ? rerankRequest.getTopN() : rerankRequest.getDocuments().size());
        request.setReturn_documents(rerankRequest.getReturnDocuments() != null ? rerankRequest.getReturnDocuments() : false);
        return objectMapper.writeValueAsString(request);
    }

    /**
     * 构建标准 RerankResult
     */
    private RerankResult buildRerankResult(SiliconFlowRerankResponse response,
                                            List<String> originalDocuments, long durationMs) {
        Double[] scores = new Double[originalDocuments.size()];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = 0.0;
        }

        List<RerankResult.RerankDocument> docs = new ArrayList<>();
        if (response != null && response.getResults() != null) {
            response.getResults().forEach(item -> {
                if (item.getIndex() != null && item.getIndex() < originalDocuments.size()) {
                    scores[item.getIndex()] = item.getRelevance_score();
                    docs.add(RerankResult.RerankDocument.builder()
                            .index(item.getIndex())
                            .relevanceScore(item.getRelevance_score())
                            .document(originalDocuments.get(item.getIndex()))
                            .build());
                }
            });
        }

        return RerankResult.builder()
                .documents(docs)
                .totalDocuments(originalDocuments.size())
                .durationMs(durationMs)
                .build();
    }

    // ==================== 内部 DTO ====================

    @Data
    static class SiliconFlowRerankRequest {
        private String model;
        private String query;
        private List<String> documents;
        private Integer top_n;
        private Boolean return_documents;
    }

    @Data
    static class SiliconFlowRerankResponse {
        private List<SiliconFlowRerankResultItem> results;
    }

    @Data
    static class SiliconFlowRerankResultItem {
        private Integer index;
        private Double relevance_score;
    }
}
