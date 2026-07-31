package org.ruoyi.workflow.workflow.node.googleSearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GoogleSearchNodeConfig {
    /**
     * 搜索查询关键词
     */
    private String query;

    @JsonProperty("search_engine")
    @Pattern(
        regexp = "search_std|search_pro|search_pro_sogou|search_pro_quark",
        message = "搜索引擎参数无效"
    )
    private String searchEngine = "search_std";

    @JsonProperty("result_count")
    @Min(value = 1, message = "搜索结果数量不能小于 1")
    @Max(value = 50, message = "搜索结果数量不能大于 50")
    private Integer resultCount = 10;

    @JsonProperty("search_domain_filter")
    private String searchDomainFilter;

    @JsonProperty("search_recency_filter")
    @Pattern(
        regexp = "oneDay|oneWeek|oneMonth|oneYear|noLimit",
        message = "搜索时间范围参数无效"
    )
    private String searchRecencyFilter = "noLimit";

    @JsonProperty("content_size")
    @Pattern(regexp = "medium|high", message = "网页摘要长度参数无效")
    private String contentSize = "medium";

    @JsonProperty("include_image")
    private Boolean includeImage = false;
}
