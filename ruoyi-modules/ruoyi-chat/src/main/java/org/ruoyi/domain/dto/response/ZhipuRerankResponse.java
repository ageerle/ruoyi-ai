package org.ruoyi.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ruoyi.domain.bo.rerank.RerankResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 智谱AI重排序响应DTO
 *
 * @author yang
 * @date 2026-04-19
 */
public record ZhipuRerankResponse(
        String model,
        String object,
        List<ResultItem> results,
        UsageInfo usage
) {
    /**
     * 单个重排序结果项
     */
    public record ResultItem(
            Integer index,
            @JsonProperty("relevance_score")
            Double relevanceScore,
            String document
    ) {}

    /**
     * Token使用信息
     */
    public record UsageInfo(
            @JsonProperty("total_tokens")
            Integer totalTokens,
            @JsonProperty("input_tokens")
            Integer inputTokens,
            @JsonProperty("output_tokens")
            Integer outputTokens
    ) {}

    /**
     * 转换为通用RerankResult
     */
    public RerankResult toRerankResult(int totalDocs, long durationMs) {
        if (results == null || results.isEmpty()) {
            return RerankResult.empty();
        }

        List<RerankResult.RerankDocument> documents = results.stream()
                .map(item -> RerankResult.RerankDocument.builder()
                        .index(item.index())
                        .relevanceScore(item.relevanceScore())
                        .document(item.document())
                        .build())
                .collect(Collectors.toList());

        return RerankResult.builder()
                .documents(documents)
                .totalDocuments(totalDocs)
                .durationMs(durationMs)
                .build();
    }
}
