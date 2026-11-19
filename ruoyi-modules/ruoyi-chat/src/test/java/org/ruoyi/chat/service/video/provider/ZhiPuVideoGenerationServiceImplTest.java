package org.ruoyi.chat.service.video.provider;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.common.chat.factory.VideoServiceFactory;
import org.ruoyi.common.chat.service.video.IVideoGenerationService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.chat.service.media.OpenAiMediaSupport;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class ZhiPuVideoGenerationServiceImplTest {

    private final AtomicReference<Request> captured = new AtomicReference<>();

    @Test
    void factorySelectsZhipuAndSubmitsDocumentedPayloadToPinnedEndpoint() throws IOException {
        var service = service(200, """
            {"id":"task_123","task_status":"PROCESSING"}
            """);
        var context = mock(ApplicationContext.class);
        when(context.getBeansOfType(IVideoGenerationService.class)).thenReturn(Map.of("zhipuVideo", service));
        var factory = new VideoServiceFactory();
        factory.setApplicationContext(context);
        assertSame(service, factory.getOriginalService("zhipu"));

        var result = factory.getOriginalService("zhipu").generateVideo(VideoContext.builder()
            .chatModelVo(model()).prompt("A cloud drifts over a mountain")
            .size("1280x720").seconds(5).quality("speed").build());

        Request request = captured.get();
        assertEquals("POST", request.method());
        assertEquals("https://open.bigmodel.cn/api/paas/v4/videos/generations", request.url().toString());
        assertEquals("Bearer fake.key", request.header("Authorization"));
        Buffer body = new Buffer();
        request.body().writeTo(body);
        JsonNode payload = OpenAiMediaSupport.OBJECT_MAPPER.readTree(body.readUtf8());
        assertEquals("cogvideox-3", payload.path("model").asText());
        assertEquals("A cloud drifts over a mountain", payload.path("prompt").asText());
        assertEquals("1280x720", payload.path("size").asText());
        assertEquals(5, payload.path("duration").intValue());
        assertEquals("speed", payload.path("quality").asText());
        assertFalse(payload.has("seconds"));
        assertEquals("task_123", result.getId());
        assertEquals("processing", result.getStatus());
        assertNull(result.getUrl());
    }

    @Test
    void retrievesCompletedVideoFromAsyncTask() {
        var service = service(200, """
            {"id":"task_123","task_status":"SUCCESS","video_result":[
              {"url":"https://cdn.example.test/video.mp4","cover_image_url":"https://cdn.example.test/cover.png"}
            ]}
            """);

        var result = service.retrieveVideo(task());
        assertEquals("GET", captured.get().method());
        assertEquals("https://open.bigmodel.cn/api/paas/v4/async-result/task_123", captured.get().url().toString());
        assertEquals("Bearer fake.key", captured.get().header("Authorization"));
        assertEquals("completed", result.getStatus());
        assertEquals("video/mp4", result.getMimeType());
        assertEquals("https://cdn.example.test/video.mp4", result.getUrl());
        assertNull(result.getLastFrameUrl());
    }

    @Test
    void mapsFailureAndDoesNotReturnProviderErrorBodies() {
        var failed = service(200, """
            {"id":"task_123","task_status":"FAIL"}
            """).retrieveVideo(task());
        assertEquals("failed", failed.getStatus());
        assertNull(failed.getUrl());

        var unauthorized = service(401, """
            {"error":{"message":"fake.key or private upstream data"}}
            """);
        var error = assertThrows(ServiceException.class, () -> unauthorized.retrieveVideo(task()));
        assertEquals("查询智谱视频任务失败（HTTP 401）", error.getMessage());
    }

    @Test
    void rejectsUnsupportedModelAndTaskPathBeforeCallingUpstream() {
        var service = service(200, "{}");
        var model = model();
        model.setModelName("glm-4v");
        var context = VideoContext.builder().chatModelVo(model).prompt("Clouds").build();
        assertThrows(ServiceException.class, () -> service.generateVideo(context));

        var task = task();
        task.setVideoId("../videos/generations");
        assertThrows(ServiceException.class, () -> service.retrieveVideo(task));
        assertNull(captured.get());
    }

    private ZhiPuVideoGenerationServiceImpl service(int code, String body) {
        var client = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request request = chain.request();
            captured.set(request);
            return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("fixture")
                .body(ResponseBody.create(body, OpenAiMediaSupport.JSON))
                .build();
        }).build();
        return new ZhiPuVideoGenerationServiceImpl(client);
    }

    private VideoContext task() {
        return VideoContext.builder().chatModelVo(model()).prompt("retrieve").videoId("task_123").build();
    }

    private ChatModelVo model() {
        var model = new ChatModelVo();
        model.setModelName("cogvideox-3");
        model.setProviderCode("zhipu");
        model.setApiHost("https://unconfigured.example.test");
        model.setApiKey("fake.key");
        return model;
    }
}
