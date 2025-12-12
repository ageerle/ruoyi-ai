package org.ruoyi.mcpserve.tools;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.ruoyi.mcpserve.config.ToolsProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网页搜索工具类
 *
 * @author OpenX
 */
@Component
public class WebSearchTools implements McpTool {

    public static final String TOOL_NAME = "web-search";

    private final ToolsProperties toolsProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebSearchTools(ToolsProperties toolsProperties) {
        this.toolsProperties = toolsProperties;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "从网络搜索引擎搜索信息")
    public String webSearch(
            @ToolParam(description = "搜索查询文本") String query,
            @ToolParam(description = "最大返回结果数量") int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            String apiKey = toolsProperties.getTavily().getApiKey();
            String baseUrl = toolsProperties.getTavily().getBaseUrl();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .post(RequestBody.create(MediaType.parse("application/json"),
                            objectMapper.writeValueAsString(requestBody)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "搜索请求失败: " + response;
                }

                JsonNode jsonNode = objectMapper.readTree(response.body().string()).get("results");
                if (jsonNode != null && !jsonNode.isEmpty()) {
                    jsonNode.forEach(data -> {
                        Map<String, String> processedResult = new HashMap<>();
                        processedResult.put("title", data.get("title").asText());
                        processedResult.put("url", data.get("url").asText());
                        processedResult.put("content", data.get("content").asText());
                        results.add(processedResult);
                    });
                }
            }
        } catch (Exception e) {
            return "搜索时发生错误: " + e.getMessage();
        }
        return JSONUtil.toJsonStr(results);
    }
}
