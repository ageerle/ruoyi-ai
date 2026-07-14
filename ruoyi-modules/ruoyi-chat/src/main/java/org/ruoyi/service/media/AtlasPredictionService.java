package org.ruoyi.service.media;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AtlasPredictionService {

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    public MediaGenerationResponse retrieve(ChatModelVo model, String predictionId) {
        if (StrUtil.isBlank(predictionId)) {
            throw new IllegalArgumentException("predictionId不能为空");
        }
        Request request = new Request.Builder()
            .url(AtlasMediaSupport.endpoint(model.getApiHost(), "/model/prediction/" + predictionId))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .get()
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("Atlas Cloud生成结果查询失败: " + response.code() + " - " + responseText);
            }
            return toResponse(responseText, mediaType(model.getCategory()));
        } catch (IOException e) {
            throw new RuntimeException("Atlas Cloud生成结果查询失败: " + e.getMessage(), e);
        }
    }

    public MediaGenerationResponse toResponse(String raw, String type) throws IOException {
        JsonNode root = AtlasMediaSupport.OBJECT_MAPPER.readTree(raw);
        JsonNode data = root.path("data");
        return MediaGenerationResponse.builder()
            .type(type)
            .mimeType("image".equals(type) ? "image/png" : "video/mp4")
            .id(AtlasMediaSupport.text(data, "id"))
            .status(AtlasMediaSupport.text(data, "status"))
            .url(firstOutput(data))
            .rawResponse(raw)
            .build();
    }

    private String firstOutput(JsonNode data) {
        JsonNode outputs = data.path("outputs");
        if (outputs.isArray() && !outputs.isEmpty()) {
            JsonNode first = outputs.get(0);
            // 字符串格式：outputs[0] = "https://..."
            if (first.isTextual()) return first.asText();
            // 对象格式：outputs[0] = {"url": "https://..."}
            if (first.isObject()) {
                String url = first.path("url").asText(null);
                if (url != null) return url;
                url = first.path("video_url").asText(null);
                if (url != null) return url;
                log.warn("outputs[0] 为对象但未找到 url 字段: {}", first);
            }
        }
        return null;
    }

    private String mediaType(String category) {
        return "image".equals(category) ? "image" : "video";
    }
}
