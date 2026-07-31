package org.ruoyi.workflow.workflow.node.googleSearch;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.web_search.WebSearchRequest;
import ai.z.openapi.service.web_search.WebSearchResp;
import ai.z.openapi.service.web_search.WebSearchResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 智谱 Web Search 官方 SDK 适配器。
 */
@Component
@RequiredArgsConstructor
public class ZhipuWebSearchClient {

    private static final String ZHIPU_PROVIDER_CODE = "zhipu";
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/";

    private final ZhipuWebSearchProperties properties;
    private final IChatModelService chatModelService;

    public SearchResponse search(String query, GoogleSearchNodeConfig config, String requestId) {
        Credential credential = resolveCredential();
        ZhipuAiClient client = createClient(credential);
        try {
            WebSearchRequest request = WebSearchRequest.builder()
                .searchQuery(query)
                .searchEngine(config.getSearchEngine())
                .count(config.getResultCount())
                .searchDomainFilter(blankToNull(config.getSearchDomainFilter()))
                .searchRecencyFilter(config.getSearchRecencyFilter())
                .contentSize(config.getContentSize())
                .includeImage(config.getIncludeImage())
                .requestId(requestId)
                .build();

            WebSearchResponse response = client.webSearch().createWebSearch(request);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                String message = response == null ? "接口未返回响应" : StringUtils.defaultIfBlank(response.getMsg(), "未知错误");
                throw new IllegalStateException("智谱 Web Search 调用失败：" + message);
            }

            List<SearchResult> results = response.getData().getWebSearchResp() == null
                ? List.of()
                : response.getData().getWebSearchResp().stream()
                    .map(this::toSearchResult)
                    .toList();

            return new SearchResponse(
                query,
                config.getSearchEngine(),
                response.getData().getRequestId(),
                results.size(),
                results
            );
        } finally {
            client.close();
        }
    }

    private ZhipuAiClient createClient(Credential credential) {
        int connectTimeout = positiveOrDefault(properties.getConnectTimeout(), 10);
        int readTimeout = positiveOrDefault(properties.getReadTimeout(), 30);
        return ZhipuAiClient.builder()
            .ofZHIPU()
            .apiKey(credential.apiKey())
            .baseUrl(credential.baseUrl())
            .networkConfig(connectTimeout, readTimeout, readTimeout, readTimeout, TimeUnit.SECONDS)
            .enableTokenCache()
            .build();
    }

    private Credential resolveCredential() {
        if (isUsableApiKey(properties.getApiKey())) {
            return new Credential(normalizeBaseUrl(properties.getBaseUrl()), properties.getApiKey().trim());
        }

        ChatModelBo query = new ChatModelBo();
        query.setProviderCode(ZHIPU_PROVIDER_CODE);
        return chatModelService.queryList(query).stream()
            .filter(model -> isUsableApiKey(model.getApiKey()))
            .findFirst()
            .map(model -> new Credential(normalizeBaseUrl(model.getApiHost()), model.getApiKey().trim()))
            .orElseThrow(() -> new IllegalStateException(
                "未配置智谱 API Key，请设置环境变量 ZAI_API_KEY，或在模型管理中配置 zhipu 厂商密钥"
            ));
    }

    private boolean isUsableApiKey(String apiKey) {
        return StringUtils.isNotBlank(apiKey)
            && !"sk_xx".equalsIgnoreCase(apiKey.trim())
            && !"your_api_key".equalsIgnoreCase(apiKey.trim());
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = StringUtils.defaultIfBlank(baseUrl, DEFAULT_BASE_URL).trim();
        normalized = StringUtils.removeEnd(normalized, "/");
        if (!normalized.endsWith("/api/paas/v4")) {
            normalized += "/api/paas/v4";
        }
        return normalized + "/";
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private SearchResult toSearchResult(WebSearchResp result) {
        return new SearchResult(
            result.getTitle(),
            result.getContent(),
            result.getLink(),
            result.getMedia(),
            result.getIcon(),
            result.getRefer(),
            result.getPublishDate(),
            result.getImages()
        );
    }

    private record Credential(String baseUrl, String apiKey) {
    }

    public record SearchResponse(
        String query,
        String searchEngine,
        String requestId,
        int count,
        List<SearchResult> results
    ) {
    }

    public record SearchResult(
        String title,
        String content,
        String link,
        String media,
        String icon,
        String refer,
        String publishDate,
        List<String> images
    ) {
    }
}
