package org.ruoyi.domain.dto.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 智谱AI重排序请求DTO
 *
 * @author yang
 * @date 2026-04-19
 */
public record ZhipuRerankRequest(
        String model,
        String query,
        List<String> documents,
        Integer top_n,
        Boolean return_documents
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 创建智谱重排序请求
     */
    public static ZhipuRerankRequest create(String modelName, String query,
                                            List<String> documents, Integer topN,
                                            Boolean returnDocuments) {
        return new ZhipuRerankRequest(
                modelName,
                query,
                documents,
                topN != null ? topN : documents.size(),
                returnDocuments != null ? returnDocuments : true
        );
    }

    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("序列化智谱重排序请求失败", e);
        }
    }
}
