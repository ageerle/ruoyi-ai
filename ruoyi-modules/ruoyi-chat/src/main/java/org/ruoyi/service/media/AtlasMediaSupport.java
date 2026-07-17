package org.ruoyi.service.media;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;

public final class AtlasMediaSupport {

    public static final MediaType JSON = MediaType.get("application/json");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AtlasMediaSupport() {
    }

    public static String endpoint(String apiHost, String path) {
        if (StrUtil.isBlank(apiHost)) {
            throw new IllegalArgumentException("apiHost不能为空");
        }
        String host = StrUtil.removeSuffix(apiHost.trim(), "/");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (host.endsWith(normalizedPath)) {
            return host;
        }
        if (host.endsWith("/api/v1")) {
            return host + normalizedPath;
        }
        if (host.endsWith("/v1")) {
            return StrUtil.removeSuffix(host, "/v1") + "/api/v1" + normalizedPath;
        }
        return host + "/api/v1" + normalizedPath;
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * 截断超长文本，用于日志输出原始响应时防止刷屏。
     */
    public static String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max) + "...(truncated " + (text.length() - max) + " chars)";
    }
}
