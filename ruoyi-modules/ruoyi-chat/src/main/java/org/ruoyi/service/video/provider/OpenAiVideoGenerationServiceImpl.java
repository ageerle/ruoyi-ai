package org.ruoyi.service.video.provider;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.media.OpenAiMediaSupport;
import org.ruoyi.service.video.AbstractVideoGenerationService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("openaiVideo")
public class OpenAiVideoGenerationServiceImpl extends AbstractVideoGenerationService {

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    @Override
    protected MediaGenerationResponse doGenerateVideo(VideoContext videoContext) {
        ChatModelVo model = videoContext.getChatModelVo();
        ObjectNode payload = OpenAiMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", model.getModelName());
        payload.put("prompt", videoContext.getPrompt());
        if (StrUtil.isNotBlank(videoContext.getSize())) {
            payload.put("size", videoContext.getSize());
        }
        if (videoContext.getSeconds() != null) {
            payload.put("seconds", videoContext.getSeconds());
        }
        if (StrUtil.isNotBlank(videoContext.getQuality())) {
            payload.put("quality", videoContext.getQuality());
        }

        try {
            return toResponse(postJson(model, "/videos", payload.toString()));
        } catch (IOException e) {
            throw new RuntimeException("视频生成任务创建失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected MediaGenerationResponse doRetrieveVideo(VideoContext videoContext) {
        if (StrUtil.isBlank(videoContext.getVideoId())) {
            throw new IllegalArgumentException("videoId不能为空");
        }
        try {
            return toResponse(getJson(videoContext.getChatModelVo(), "/videos/" + videoContext.getVideoId()));
        } catch (IOException e) {
            throw new RuntimeException("视频生成任务查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

    private MediaGenerationResponse toResponse(String raw) throws IOException {
        JsonNode root = OpenAiMediaSupport.OBJECT_MAPPER.readTree(raw);
        return MediaGenerationResponse.builder()
            .type("video")
            .mimeType("video/mp4")
            .id(OpenAiMediaSupport.text(root, "id"))
            .status(OpenAiMediaSupport.text(root, "status"))
            .url(firstAvailableUrl(root))
            .rawResponse(raw)
            .build();
    }

    private String firstAvailableUrl(JsonNode root) {
        String url = OpenAiMediaSupport.text(root, "url");
        if (StrUtil.isNotBlank(url)) {
            return url;
        }
        JsonNode data = root.path("data");
        if (data.isArray() && !data.isEmpty()) {
            JsonNode first = data.get(0);
            url = OpenAiMediaSupport.text(first, "url");
            if (StrUtil.isNotBlank(url)) {
                return url;
            }
        }
        return null;
    }

    private String postJson(ChatModelVo model, String path, String jsonBody) throws IOException {
        Request request = new Request.Builder()
            .url(OpenAiMediaSupport.endpoint(model.getApiHost(), path))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonBody, OpenAiMediaSupport.JSON))
            .build();
        return execute(request, "OpenAI Videos API调用失败");
    }

    private String getJson(ChatModelVo model, String path) throws IOException {
        Request request = new Request.Builder()
            .url(OpenAiMediaSupport.endpoint(model.getApiHost(), path))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .get()
            .build();
        return execute(request, "OpenAI Videos API查询失败");
    }

    private String execute(Request request, String errorMessage) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException(errorMessage + ": " + response.code() + " - " + responseText);
            }
            return responseText;
        }
    }
}
