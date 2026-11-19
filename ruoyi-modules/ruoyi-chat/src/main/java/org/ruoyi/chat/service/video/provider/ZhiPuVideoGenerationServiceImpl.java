package org.ruoyi.chat.service.video.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.media.OpenAiMediaSupport;
import org.ruoyi.chat.service.video.AbstractVideoGenerationService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component("zhipuVideo")
public class ZhiPuVideoGenerationServiceImpl extends AbstractVideoGenerationService {

    private static final HttpUrl API_BASE = HttpUrl.get("https://open.bigmodel.cn/api/paas/v4");
    private static final String MODEL_NAME = "cogvideox-3";
    private static final Set<String> SIZES = Set.of(
        "1280x720", "720x1280", "1024x1024", "1920x1080", "1080x1920", "2048x1080", "3840x2160");
    private static final Set<String> QUALITIES = Set.of("speed", "quality");

    private final OkHttpClient client;

    public ZhiPuVideoGenerationServiceImpl() {
        this(new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .build());
    }

    ZhiPuVideoGenerationServiceImpl(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ZHI_PU.getCode();
    }

    @Override
    protected MediaGenerationResponse doGenerateVideo(VideoContext context) {
        HttpUrl endpoint = API_BASE.newBuilder().addPathSegments("videos/generations").build();
        Request request = request(endpoint, context.getChatModelVo())
            .post(RequestBody.create(payload(context).toString(), OpenAiMediaSupport.JSON))
            .build();
        return toResponse(execute(request, "提交智谱视频任务"), null);
    }

    @Override
    protected MediaGenerationResponse doRetrieveVideo(VideoContext context) {
        String taskId = context.getVideoId();
        if (taskId == null || !taskId.matches("[a-zA-Z0-9_-]+")) {
            throw new ServiceException("智谱视频任务编号格式有误");
        }
        HttpUrl endpoint = API_BASE.newBuilder()
            .addPathSegment("async-result")
            .addPathSegment(taskId)
            .build();
        Request request = request(endpoint, context.getChatModelVo()).get().build();
        return toResponse(execute(request, "查询智谱视频任务"), taskId);
    }

    private Request.Builder request(HttpUrl endpoint, ChatModelVo model) {
        if (!MODEL_NAME.equals(model.getModelName())) {
            throw new ServiceException("智谱视频生成支持 " + MODEL_NAME + " 模型");
        }
        String apiKey = model.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceException("请在模型管理中填写智谱开放平台 API Key");
        }
        apiKey = apiKey.trim();
        if (apiKey.chars().anyMatch(c -> c <= 32 || c >= 127)) {
            throw new ServiceException("智谱开放平台 API Key 格式有误");
        }
        return new Request.Builder().url(endpoint).header("Authorization", "Bearer " + apiKey);
    }

    private ObjectNode payload(VideoContext context) {
        String prompt = context.getPrompt();
        if (prompt == null || prompt.isBlank() || prompt.codePointCount(0, prompt.length()) > 512) {
            throw new ServiceException("请填写 512 字符以内的智谱视频提示词");
        }

        ObjectNode payload = OpenAiMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", MODEL_NAME);
        payload.put("prompt", prompt);

        String size = context.getSize();
        if (size != null && !size.isBlank()) {
            if (!SIZES.contains(size)) {
                throw new ServiceException("智谱视频尺寸不受支持，请选择有效的像素尺寸，如 1280x720");
            }
            payload.put("size", size);
        }
        Integer seconds = context.getSeconds();
        if (seconds != null) {
            if (seconds != 5 && seconds != 10) {
                throw new ServiceException("智谱视频时长请选择 5 秒或 10 秒");
            }
            payload.put("duration", seconds);
        }
        String quality = context.getQuality();
        if (quality != null && !quality.isBlank()) {
            if (!QUALITIES.contains(quality)) {
                throw new ServiceException("智谱视频生成模式请选择 speed 或 quality");
            }
            payload.put("quality", quality);
        }
        if (context.getImageUrl() != null && !context.getImageUrl().isBlank()) {
            payload.put("image_url", context.getImageUrl());
        }
        if (context.getGenerateAudio() != null) {
            payload.put("with_audio", context.getGenerateAudio());
        }
        return payload;
    }

    private JsonNode execute(Request request, String operation) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ServiceException(operation + "失败（HTTP " + response.code() + "）");
            }
            ResponseBody body = response.body();
            JsonNode result = body == null ? null : OpenAiMediaSupport.OBJECT_MAPPER.readTree(body.byteStream());
            if (result == null || !result.isObject() || result.hasNonNull("error")) {
                throw new ServiceException(operation + "失败，智谱未返回有效任务数据");
            }
            return result;
        } catch (IOException e) {
            throw new ServiceException(operation + "时未能读取智谱接口的响应");
        }
    }

    private MediaGenerationResponse toResponse(JsonNode result, String requestedId) {
        String taskId = result.path("id").asText(requestedId);
        if (taskId == null || taskId.isBlank() || (requestedId != null && !requestedId.equals(taskId))) {
            throw new ServiceException("智谱视频接口未返回匹配的任务编号");
        }
        String url = result.path("video_result").path(0).path("url").asText(null);
        String status = switch (result.path("task_status").asText()) {
            case "PROCESSING" -> "processing";
            case "FAIL" -> "failed";
            case "SUCCESS" -> completedStatus(url, requestedId);
            default -> throw new ServiceException("智谱视频接口未返回有效的任务状态");
        };
        return MediaGenerationResponse.builder()
            .type("video")
            .mimeType("video/mp4")
            .id(taskId)
            .status(status)
            .url("completed".equals(status) ? url : null)
            .build();
    }

    private String completedStatus(String url, String requestedId) {
        if (url != null && !url.isBlank()) {
            return "completed";
        }
        if (requestedId == null) {
            // The submit endpoint can report SUCCESS before returning the result URL.
            return "processing";
        }
        throw new ServiceException("智谱视频任务已完成，但未返回视频地址");
    }
}
