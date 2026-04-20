package org.ruoyi.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 阿里百炼重排序请求DTO（OpenAI兼容格式）
 *
 * @author yang
 * @date 2026-04-20
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AliBaiLianRerankRequest(
        String model,
        List<String> documents,
        String query,
        @JsonProperty("top_n")
        Integer topN,
        String instruct,
        @JsonProperty("return_documents")
        Boolean returnDocuments
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 创建文本重排序请求
     */
    public static AliBaiLianRerankRequest create(String modelName, String query,
                                                  List<String> documents, Integer topN,
                                                  Boolean returnDocuments) {
        return new AliBaiLianRerankRequest(
                modelName,
                documents,
                query,
                topN != null ? topN : documents.size(),
                null,
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
            throw new IllegalArgumentException("序列化阿里百炼重排序请求失败", e);
        }
    }
}
