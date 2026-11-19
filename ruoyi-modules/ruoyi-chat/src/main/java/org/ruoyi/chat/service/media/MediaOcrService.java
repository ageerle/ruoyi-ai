package org.ruoyi.chat.service.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class MediaOcrService {

    public static final String MODEL = "glm-ocr";
    private static final String ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/layout_parsing";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_IMAGE_BYTES = ChatRequest.Image.MAX_BYTES;
    private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024;

    private final IChatModelService models;
    private final ObjectMapper json;
    private final OkHttpClient client;

    @Autowired
    public MediaOcrService(IChatModelService models, ObjectMapper json) {
        this(models, json, new OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build());
    }

    MediaOcrService(IChatModelService models, ObjectMapper json, OkHttpClient client) {
        this.models = models;
        this.json = json;
        this.client = client;
    }

    public String recognize(MultipartFile file, String modelName) {
        if (!MODEL.equals(modelName)) {
            throw new OcrException("请选择 GLM-OCR 识别图片");
        }
        byte[] image = readImage(file);
        return recognize(image, validateImage(image));
    }

    public String recognize(byte[] image, String mimeType) {
        String key = loadApiKey();
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(image);
        String payload = json.createObjectNode().put("model", MODEL).put("file", dataUri).toString();
        Request request = new Request.Builder()
            .url(ENDPOINT)
            .header("Authorization", "Bearer " + key)
            .post(RequestBody.create(payload, JSON))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new OcrException(switch (response.code()) {
                    case 401, 403 -> "智谱拒绝了识别请求，请联系管理员检查 GLM-OCR 的 API 密钥和调用权限";
                    case 429 -> "识别请求过多，请稍后重试";
                    default -> "智谱暂未完成识别，请稍后重试";
                });
            }
            return extractText(response.body());
        } catch (SocketTimeoutException e) {
            throw new OcrException("图片识别超时，请重试");
        } catch (IOException e) {
            throw new OcrException("识别服务暂不可用，请稍后重试");
        }
    }

    private String loadApiKey() {
        ChatModelVo model;
        try {
            model = models.selectModelByName(MODEL);
        } catch (ServiceException e) {
            throw new OcrException("GLM-OCR 厂商未启用，请联系管理员");
        }
        if (model == null || !"ocr".equals(model.getCategory())
            || !ChatModeType.ZHI_PU.getCode().equals(model.getProviderCode())) {
            throw new OcrException("GLM-OCR 模型尚未配置，请联系管理员");
        }
        String key = model.getApiKey();
        if (key == null || key.isBlank() || key.chars().anyMatch(c -> c <= ' ' || c >= 127)) {
            throw new OcrException("GLM-OCR 的 API 密钥未正确配置，请联系管理员");
        }
        return key;
    }

    private static byte[] readImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new OcrException("请上传 JPG 或 PNG 图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new OcrException("图片不能超过 10 MB");
        }
        try (var input = file.getInputStream()) {
            byte[] image = input.readNBytes(MAX_IMAGE_BYTES + 1);
            if (image.length > MAX_IMAGE_BYTES) {
                throw new OcrException("图片不能超过 10 MB");
            }
            return image;
        } catch (IOException e) {
            throw new OcrException("无法读取图片，请重新上传");
        }
    }

    public static String validateImage(byte[] image) {
        if (image.length == 0) {
            throw new OcrException("请添加 JPG 或 PNG 图片");
        }
        if (image.length > MAX_IMAGE_BYTES) {
            throw new OcrException("图片不能超过 10 MB");
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(image))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new OcrException("请上传有效的 JPG 或 PNG 图片");
            }
            ImageReader reader = readers.next();
            try {
                String mimeType = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> throw new OcrException("请上传 JPG 或 PNG 图片");
                };
                // Read image metadata without allocating a pixel buffer.
                reader.setInput(input, true, true);
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0) {
                    throw new OcrException("无法读取图片尺寸，请重新上传");
                }
                return mimeType;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new OcrException("图片无法读取，请检查文件后重新上传");
        }
    }

    private String extractText(ResponseBody body) throws IOException {
        if (body == null || body.contentLength() > MAX_RESPONSE_BYTES) {
            throw new OcrException("无法读取识别结果，请重试");
        }
        byte[] response = body.byteStream().readNBytes(MAX_RESPONSE_BYTES + 1);
        if (response.length > MAX_RESPONSE_BYTES) {
            throw new OcrException("无法读取识别结果，请重试");
        }
        JsonNode result = json.readTree(response);
        if (result == null || !result.path("md_results").isTextual()) {
            throw new OcrException("无法读取识别结果，请重试");
        }
        return result.get("md_results").textValue();
    }

    public static final class OcrException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        OcrException(String message) {
            super(message);
        }
    }
}
