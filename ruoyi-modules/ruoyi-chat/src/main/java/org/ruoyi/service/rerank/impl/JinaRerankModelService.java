package org.ruoyi.service.rerank.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.http.client.*;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.service.rerank.RerankModelService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jina AI 重排序模型实现
 * 参考设计模式：OpenAiEmbeddingProvider
 * 使用 Jina 官方重排序API
 *
 * @author yang
 * @date 2026-04-19
 */
@Slf4j
@Component("jina")
public class JinaRerankModelService implements RerankModelService {

    protected ChatModelVo chatModelVo;
    protected HttpClient httpClient;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
        HttpClientBuilder httpClientBuilder = HttpClientBuilderLoader.loadHttpClientBuilder();
        this.httpClient = httpClientBuilder.build();
    }

    @Override
    public RerankResult rerank(RerankRequest rerankRequest) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建Jina重排序请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", chatModelVo.getModelName());
            requestBody.put("query", rerankRequest.getQuery());
            requestBody.put("documents", rerankRequest.getDocuments());
            requestBody.put("top_n", rerankRequest.getTopN() != null ?
                    rerankRequest.getTopN() : rerankRequest.getDocuments().size());
            requestBody.put("return_documents", rerankRequest.getReturnDocuments());

            // 构建HTTP请求
            HttpRequest httpRequest = HttpRequest.builder()
                    .url(chatModelVo.getApiHost())
                    .method(HttpMethod.POST)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + chatModelVo.getApiKey())
                    .body(objectMapper.writeValueAsString(requestBody))
                    .build();

            // 发送请求
            SuccessfulHttpResponse httpResponse = httpClient.execute(httpRequest);

            // 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(httpResponse.body(), Map.class);
            return parseResponse(response, rerankRequest.getDocuments().size(),
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Jina重排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("重排序服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析Jina API响应
     */
    @SuppressWarnings("unchecked")
    private RerankResult parseResponse(Map<String, Object> response, int totalDocs, long durationMs) {
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            return RerankResult.empty();
        }

        List<RerankResult.RerankDocument> documents = new ArrayList<>();
        for (Map<String, Object> result : results) {
            Integer index = (Integer) result.get("index");
            Double score = ((Number) result.get("relevance_score")).doubleValue();
            String document = (String) result.get("document");

            documents.add(RerankResult.RerankDocument.builder()
                    .index(index)
                    .relevanceScore(score)
                    .document(document)
                    .build());
        }

        return RerankResult.builder()
                .documents(documents)
                .totalDocuments(totalDocs)
                .durationMs(durationMs)
                .build();
    }
}
