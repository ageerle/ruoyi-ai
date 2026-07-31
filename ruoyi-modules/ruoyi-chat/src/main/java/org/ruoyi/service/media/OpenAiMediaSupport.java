package org.ruoyi.service.media;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;

import java.util.Base64;

public final class OpenAiMediaSupport {

    public static final MediaType JSON = MediaType.get("application/json");
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private OpenAiMediaSupport() {
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
        if (host.endsWith("/v1")) {
            return host + normalizedPath;
        }
        return host + "/v1" + normalizedPath;
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static String dataUrl(String mimeType, byte[] bytes) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    public static String dataUrl(String mimeType, String b64Json) {
        return StrUtil.isBlank(b64Json) ? null : "data:" + mimeType + ";base64," + b64Json;
    }
}
