package org.ruoyi.mcpserve.tools;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 网页内容加载工具类
 *
 * @author OpenX
 */
@Component
public class WebPageTools implements McpTool {

    public static final String TOOL_NAME = "web-page";

    private final OkHttpClient httpClient;

    public WebPageTools() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Tool(description = "加载网页并提取文本内容")
    public String loadWebPage(@ToolParam(description = "要加载的网页URL地址") String url) {
        if (url == null || url.trim().isEmpty()) {
            return "Error: URL is empty. Please provide a valid URL.";
        }

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "Error: Failed to load web page, status: " + response.code();
                }

                String html = response.body().string();
                // 简单的HTML文本提取
                String text = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                        .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                        .replaceAll("<[^>]+>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();

                return text;
            }
        } catch (Exception e) {
            return "Error loading web page: " + e.getMessage();
        }
    }
}
