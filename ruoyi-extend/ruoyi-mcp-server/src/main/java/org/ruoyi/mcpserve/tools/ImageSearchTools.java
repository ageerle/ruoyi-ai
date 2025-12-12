package org.ruoyi.mcpserve.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.ruoyi.mcpserve.config.ToolsProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具类
 *
 * @author OpenX
 */
@Component
public class ImageSearchTools implements McpTool {

    public static final String TOOL_NAME = "image-search";

    private final ToolsProperties toolsProperties;

    public ImageSearchTools(ToolsProperties toolsProperties) {
        this.toolsProperties = toolsProperties;
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "从Pexels搜索图片")
    public String searchImage(@ToolParam(description = "图片搜索关键词") String query) {
        try {
            String apiKey = toolsProperties.getPexels().getApiKey();
            String apiUrl = toolsProperties.getPexels().getApiUrl();

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", apiKey);

            Map<String, Object> params = new HashMap<>();
            params.put("query", query);

            String response = HttpUtil.createGet(apiUrl)
                    .addHeaders(headers)
                    .form(params)
                    .execute()
                    .body();

            List<String> images = JSONUtil.parseObj(response)
                    .getJSONArray("photos")
                    .stream()
                    .map(photoObj -> (JSONObject) photoObj)
                    .map(photoObj -> photoObj.getJSONObject("src"))
                    .map(photo -> photo.getStr("medium"))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());

            return String.join(",", images);
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }
}
