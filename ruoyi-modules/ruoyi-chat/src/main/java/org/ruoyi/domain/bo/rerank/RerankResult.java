package org.ruoyi.domain.bo.rerank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 重排序结果
 *
 * @author yang
 * @date 2026-04-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankResult {

    /**
     * 重排序后的文档结果列表
     */
    private List<RerankDocument> documents;

    /**
     * 原始请求中的文档总数
     */
    private Integer totalDocuments;

    /**
     * 重排序耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 单个重排序文档结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RerankDocument {

        /**
         * 文档在原始列表中的索引位置
         */
        private Integer index;

        /**
         * 相关性分数（通常 0-1 之间，越高越相关）
         */
        private Double relevanceScore;

        /**
         * 文档内容
         */
        private String document;
    }

    /**
     * 创建空结果
     */
    public static RerankResult empty() {
        return RerankResult.builder()
                .documents(List.of())
                .totalDocuments(0)
                .durationMs(0L)
                .build();
    }
}
