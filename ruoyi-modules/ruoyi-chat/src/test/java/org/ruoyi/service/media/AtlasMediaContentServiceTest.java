package org.ruoyi.service.media;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class AtlasMediaContentServiceTest {
    private static final String URL = "https://atlas-media.oss-us-west-1.aliyuncs.com/assetd-history/result.mp3";
    private final AtlasPredictionService predictions = mock(AtlasPredictionService.class);
    private final OkHttpClient client = mock(OkHttpClient.class);
    private final AtlasMediaContentService service = new AtlasMediaContentService(predictions, client);

    @Test
    void servesMediaBytesWithoutForwardingCredentialsOrDownloadHeaders() throws Exception {
        var model = model();
        model.setApiKey("env:ATLAS_API_KEY");
        completed(model);
        respond(200, ResponseBody.create("ID3audio", MediaType.get("audio/mpeg")), "audio/mpeg");
        var content = service.retrieve(model, "task-123");
        assertEquals("audio/mpeg", content.mimeType());
        assertArrayEquals("ID3audio".getBytes(), content.bytes());
        var capture = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(capture.capture());
        assertEquals(URL, capture.getValue().url().toString());
        assertNull(capture.getValue().header("Authorization"));
        assertNull(capture.getValue().header("Cookie"));
    }

    @Test
    void rejectsUntrustedUrlsAndNonHttpsVariants() {
        for (String url : new String[]{"http://atlas-media.oss-us-west-1.aliyuncs.com/a.mp3",
            "https://127.0.0.1/a", "https://atlas-media.oss-us-west-1.aliyuncs.com.evil.test/a",
            "https://user@atlas-media.oss-us-west-1.aliyuncs.com/a",
            "https://atlas-media.oss-us-west-1.aliyuncs.com:8443/a",
            "https://atlas-media.oss-us-west-1.aliyuncs.com/a#fragment", "file:///etc/passwd"}) {
            assertThrows(IllegalArgumentException.class, () -> AtlasMediaContentService.checkedOutputUrl(url));
        }
        assertEquals("atlas-media.oss-us-west-1.aliyuncs.com", AtlasMediaContentService.checkedOutputUrl(URL).host());
    }

    @Test
    void rejectsInvalidIdsAndOtherProvidersBeforeMakingRequests() {
        var model = model();
        assertThrows(IllegalArgumentException.class, () -> service.retrieve(model, "../task?url=bad"));
        model.setProviderCode("openai");
        assertThrows(IllegalArgumentException.class, () -> service.retrieve(model, "task-123"));
        verifyNoInteractions(predictions, client);
    }

    @Test
    void rejectsUnfinishedOrMismatchedTasksBeforeFetchingContent() {
        var model = model();
        when(predictions.retrieve(model, "task-123")).thenReturn(MediaGenerationResponse.builder()
            .id("task-123").status("processing").url(URL).build());
        assertThrows(IllegalArgumentException.class, () -> service.retrieve(model, "task-123"));
        when(predictions.retrieve(model, "task-123")).thenReturn(MediaGenerationResponse.builder()
            .id("different").status("completed").url(URL).build());
        assertThrows(IllegalArgumentException.class, () -> service.retrieve(model, "task-123"));
        verifyNoInteractions(client);
    }

    @Test
    void rejectsRedirectsAndHtmlResponses() throws Exception {
        var model = model();
        completed(model);
        respond(302, ResponseBody.create("", MediaType.get("audio/mpeg")), "audio/mpeg");
        assertThrows(IOException.class, () -> service.retrieve(model, "task-123"));
        respond(200, ResponseBody.create("<html>error</html>", MediaType.get("text/html")), "text/html");
        assertThrows(IOException.class, () -> service.retrieve(model, "task-123"));
    }

    @Test
    void rejectsOversizedContentBeforeReadingTheBody() throws Exception {
        var model = model();
        completed(model);
        var body = mock(ResponseBody.class);
        when(body.contentLength()).thenReturn((long) AtlasMediaContentService.MAX_BYTES + 1);
        respond(200, body, "audio/mpeg");
        assertThrows(IOException.class, () -> service.retrieve(model, "task-123"));
        verify(body, never()).byteStream();
    }

    private ChatModelVo model() {
        var model = new ChatModelVo();
        model.setCategory("audio");
        model.setProviderCode("atlas");
        return model;
    }

    private void completed(ChatModelVo model) {
        when(predictions.retrieve(model, "task-123")).thenReturn(MediaGenerationResponse.builder()
            .id("task-123").status("completed").url(URL).build());
    }

    private void respond(int status, ResponseBody body, String type) throws IOException {
        Call call = mock(Call.class);
        when(client.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(new Response.Builder().request(new Request.Builder().url(URL).build())
            .protocol(Protocol.HTTP_1_1).code(status).message("test").body(body)
            .header("Content-Type", type).header("Content-Disposition", "attachment")
            .header("x-oss-force-download", "true").header("Location", "http://127.0.0.1/private").build());
    }
}
