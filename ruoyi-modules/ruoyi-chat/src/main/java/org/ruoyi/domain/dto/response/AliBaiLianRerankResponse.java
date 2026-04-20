package org.ruoyi.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ruoyi.domain.bo.rerank.RerankResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 阿里百炼重排序响应DTO（OpenAI兼容格式）
 *
 * @author yang
 * @date 2026-04-20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AliBaiLianRerankResponse(
        String id,
        String object,
        List<ResultItem> results,
        UsageInfo usage
) {
    /**
     * 单个重排序结果项
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResultItem(
            Integer index,
            @JsonProperty("relevance_score")
            Double relevanceScore,
            Object document
    ) {
        /**
         * 获取文档文本内容
         */
        public String getDocumentText() {
            if (document == null) return null;
            if (document instanceof String) return (String) document;
            if (document instanceof Map) {
                Object text = ((Map<?, ?>) document).get("text");
                return text != null ? text.toString() : null;
            }
            return document.toString();
        }
    }

    /**
     * Token使用信息
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsageInfo(
            @JsonProperty("total_tokens")
            Integer totalTokens,
            @JsonProperty("prompt_tokens")
            Integer promptTokens
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
                        .document(item.getDocumentText())
                        .build())
                .collect(Collectors.toList());

        return RerankResult.builder()
                .documents(documents)
                .totalDocuments(totalDocs)
                .durationMs(durationMs)
                .build();
    }
}
