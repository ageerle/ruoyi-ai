package org.ruoyi.service.media;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Delivers Atlas task outputs without exposing a general-purpose URL proxy. */
@Service
public class AtlasMediaContentService {
    static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final Set<String> OUTPUT_HOSTS = Set.of(
        "atlas-media.oss-us-west-1.aliyuncs.com",
        "ark-acg-ap-southeast-1.tos-ap-southeast-1.volces.com"
    );
    private static final Set<String> MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif",
        "audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/flac",
        "video/mp4", "video/webm"
    );

    private final AtlasPredictionService predictions;
    private final OkHttpClient client;

    @Autowired
    public AtlasMediaContentService(AtlasPredictionService predictions) {
        this(predictions, new OkHttpClient.Builder()
            .followRedirects(false).followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build());
    }

    AtlasMediaContentService(AtlasPredictionService predictions, OkHttpClient client) {
        this.predictions = predictions;
        this.client = client;
    }

    public Content retrieve(ChatModelVo model, String predictionId) throws IOException {
        if (!"atlas".equals(model.getProviderCode())
            || !Set.of("image", "audio", "video").contains(model.getCategory())) {
            throw new IllegalArgumentException("此模型不支持 Atlas 资源预览");
        }
        if (predictionId == null || !predictionId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new IllegalArgumentException("无效的任务 ID");
        }
        // Resolve the output using this configured model's credentials, never a caller-supplied URL.
        var result = predictions.retrieve(model, predictionId);
        if (!Set.of("completed", "succeeded", "success").contains(String.valueOf(result.getStatus()))
            || !predictionId.equals(result.getId())) {
            throw new IllegalArgumentException("任务尚未完成、已过期或不存在");
        }
        HttpUrl url = checkedOutputUrl(result.getUrl());
        // The API key belongs only on the prediction API request, never on the media CDN request.
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (response.code() != 200 || body == null) {
                throw new IOException("资源服务暂不可用，请稍后重试");
            }
            String mime = response.header("Content-Type", "").split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (!MIME_TYPES.contains(mime) || !mime.startsWith(model.getCategory() + "/")) {
                throw new IOException("资源格式与模型类型不匹配或不支持预览");
            }
            if (body.contentLength() > MAX_BYTES) {
                throw new IOException("资源超过 64 MiB 预览上限");
            }
            byte[] bytes = body.byteStream().readNBytes(MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > MAX_BYTES) {
                throw new IOException("资源为空或超过 64 MiB 预览上限");
            }
            return new Content(bytes, mime);
        }
    }

    static HttpUrl checkedOutputUrl(String value) {
        HttpUrl url = value == null ? null : HttpUrl.parse(value);
        if (url == null || !url.isHttps() || url.port() != 443
            || !url.username().isEmpty() || !url.password().isEmpty() || url.fragment() != null
            || !OUTPUT_HOSTS.contains(url.host())) {
            throw new IllegalArgumentException("资源地址不在 Atlas 预览白名单内");
        }
        return url;
    }

    public record Content(byte[] bytes, String mimeType) { }
}
