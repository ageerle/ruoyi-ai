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
            // 404: 预测任务已过期或不存在（常见于更换 API Key 后残留的旧任务），按失败处理，
            // 避免前端轮询反复触发异常提示。其余非 2xx 仍按异常抛出。
            if (response.code() == 404) {
                log.warn("Atlas Cloud 预测任务不存在或已过期: predictionId={}, category={}", predictionId, model.getCategory());
                return MediaGenerationResponse.builder()
                    .type(mediaType(model.getCategory()))
                    .mimeType("image".equals(model.getCategory()) ? "image/png" : "video/mp4")
                    .status("failed")
                    .rawResponse(responseText)
                    .build();
            }
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
        String status = AtlasMediaSupport.text(data, "status");
        String lastFrameUrl = firstLastFrame(data);
        if ("video".equals(type)) {
            // 终态打完整原始响应（确认 Atlas 末帧字段名/结构），轮询中间态只打摘要，避免刷屏
            if ("succeeded".equals(status) || "completed".equals(status) || "failed".equals(status)) {
                log.info("Atlas 视频结果[{}]原始响应: {}", status, AtlasMediaSupport.truncate(raw, 2000));
            }
            log.info("Atlas 视频结果解析: status={}, lastFrameUrl={}", status, lastFrameUrl);
        }
        return MediaGenerationResponse.builder()
            .type(type)
            .mimeType("image".equals(type) ? "image/png" : "video/mp4")
            .id(AtlasMediaSupport.text(data, "id"))
            .status(status)
            .url(firstOutput(data))
            .lastFrameUrl(lastFrameUrl)
            .rawResponse(raw)
            .build();
    }

    /**
     * 提取末帧 URL。Atlas return_last_frame=true 时会在 outputs 或顶层节点返回末帧图片，
     * 字段名兼容 last_frame_url / end_frame_url / last_frame / last_frame_image。
     */
    private String firstLastFrame(JsonNode data) {
        if (data == null || data.isMissingNode()) return null;
        String[] keys = {"last_frame_url", "end_frame_url", "last_frame", "last_frame_image"};
        for (String key : keys) {
            String val = AtlasMediaSupport.text(data, key);
            if (val != null) return val;
        }
        JsonNode outputs = data.path("outputs");
        if (outputs.isObject()) {
            for (String key : keys) {
                String val = AtlasMediaSupport.text(outputs, key);
                if (val != null) return val;
            }
        }
        if (outputs.isArray() && !outputs.isEmpty()) {
            JsonNode first = outputs.get(0);
            if (first.isObject()) {
                for (String key : keys) {
                    String val = AtlasMediaSupport.text(first, key);
                    if (val != null) return val;
                }
            }
        }
        return null;
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
