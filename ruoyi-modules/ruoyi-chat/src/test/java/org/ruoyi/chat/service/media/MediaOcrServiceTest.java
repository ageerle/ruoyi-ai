package org.ruoyi.chat.service.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class MediaOcrServiceTest {

    private final IChatModelService models = mock(IChatModelService.class);
    private final OkHttpClient client = mock(OkHttpClient.class);
    private final ObjectMapper json = new ObjectMapper();
    private final MediaOcrService service = new MediaOcrService(models, json, client);

    @Test
    void sendsImageDataToOfficialEndpointAndReturnsMarkdown() throws Exception {
        configureModel("ocr", "zhipu");
        respond(200, "{\"md_results\":\"# 发票\\n金额 100 元\"}");
        byte[] png = image("png");

        String result = service.recognize(
            new MockMultipartFile("file", "scan.png", "application/octet-stream", png), "glm-ocr");

        assertEquals("# 发票\n金额 100 元", result);
        var capture = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(capture.capture());
        Request request = capture.getValue();
        assertEquals("https://open.bigmodel.cn/api/paas/v4/layout_parsing", request.url().toString());
        assertEquals("POST", request.method());
        assertEquals("Bearer mock-provider-key", request.header("Authorization"));
        var body = new Buffer();
        request.body().writeTo(body);
        var payload = json.readTree(body.readUtf8());
        assertEquals("glm-ocr", payload.path("model").textValue());
        assertEquals("data:image/png;base64," + Base64.getEncoder().encodeToString(png),
            payload.path("file").textValue());
    }

    @Test
    void rejectsUnsupportedAndOversizedImagesBeforeLoadingKey() throws Exception {
        var fakePng = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());
        var oversized = mock(MultipartFile.class);
        when(oversized.getSize()).thenReturn(10L * 1024 * 1024 + 1);
        var gif = new MockMultipartFile("file", "image.png", "image/png", image("gif"));

        assertThrows(MediaOcrService.OcrException.class, () -> service.recognize(fakePng, "glm-ocr"));
        assertThrows(MediaOcrService.OcrException.class, () -> service.recognize(oversized, "glm-ocr"));
        assertThrows(MediaOcrService.OcrException.class, () -> service.recognize(gif, "glm-ocr"));
        verifyNoInteractions(models, client);
    }

    @Test
    void rejectsAlternativeModelsAndMismatchedProvider() throws Exception {
        configureModel("ocr", "other");
        var jpeg = new MockMultipartFile("file", "photo.jpg", "image/jpeg", image("jpg"));

        assertThrows(MediaOcrService.OcrException.class, () -> service.recognize(jpeg, "other-model"));
        assertThrows(MediaOcrService.OcrException.class, () -> service.recognize(jpeg, "glm-ocr"));
        verifyNoInteractions(client);
    }

    @Test
    void hidesUpstreamErrorContents() throws Exception {
        configureModel("ocr", "zhipu");
        respond(401, "{\"error\":{\"message\":\"sensitive upstream details\"}}");
        var jpeg = new MockMultipartFile("file", "photo.jpg", "image/jpeg", image("jpg"));

        var error = assertThrows(MediaOcrService.OcrException.class, () -> service.recognize(jpeg, "glm-ocr"));
        assertFalse(error.getMessage().contains("sensitive upstream details"));
        assertEquals("智谱拒绝了识别请求，请联系管理员检查 GLM-OCR 的 API 密钥和调用权限", error.getMessage());
    }

    private void configureModel(String category, String providerCode) {
        var model = new ChatModelVo();
        model.setModelName("glm-ocr");
        model.setCategory(category);
        model.setProviderCode(providerCode);
        model.setApiHost("https://example.invalid/");
        model.setApiKey("mock-provider-key");
        when(models.selectModelByName("glm-ocr")).thenReturn(model);
    }

    private void respond(int code, String body) throws IOException {
        var call = mock(Call.class);
        var response = new Response.Builder()
            .request(new Request.Builder().url("https://open.bigmodel.cn/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("Test response")
            .body(ResponseBody.create(body, MediaType.get("application/json")))
            .build();
        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenReturn(response);
    }

    private static byte[] image(String format) throws IOException {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        var output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) {
            throw new IOException("Image writer unavailable: " + format);
        }
        return output.toByteArray();
    }
}
