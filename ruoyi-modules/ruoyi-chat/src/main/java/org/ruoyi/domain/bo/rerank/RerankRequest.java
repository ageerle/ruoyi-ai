package org.ruoyi.domain.bo.rerank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 重排序请求参数
 *
 * @author yang
 * @date 2026-04-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankRequest {

    /**
     * 查询文本
     */
    private String query;

    /**
     * 候选文档列表
     */
    private List<String> documents;

    /**
     * 返回的文档数量（topN）
     * 如果不指定，默认返回所有文档
     */
    private Integer topN;

    /**
     * 是否返回原始文档内容
     * 默认为 true
     */
    @Builder.Default
    private Boolean returnDocuments = true;
}
