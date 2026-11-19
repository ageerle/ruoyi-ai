package org.ruoyi.chat.service.image.provider;

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
import org.ruoyi.common.chat.security.CustomApiCredentialPolicy;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.image.AbstractImageGenerationService;
import org.ruoyi.chat.service.media.OpenAiMediaSupport;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("openaiImage")
public class OpenAiImageGenerationServiceImpl extends AbstractImageGenerationService {

    private static final String GENERATIONS_PATH = "/images/generations";

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build();

    @Override
    protected String doGenerateImage(ChatModelVo chatModelVo, String prompt, String size, Integer seed, String image) {
        boolean ark = CustomApiCredentialPolicy.ARK_PROVIDER.equals(chatModelVo.getProviderCode());
        ObjectNode payload = OpenAiMediaSupport.OBJECT_MAPPER.createObjectNode();
        payload.put("model", chatModelVo.getModelName());
        payload.put("prompt", prompt);
        if (ark) {
            payload.put("size", StrUtil.blankToDefault(StrUtil.trim(size), "2K"));
            payload.put("output_format", "png");
            payload.put("response_format", "url");
        } else {
            payload.put("n", 1);
            if (StrUtil.isNotBlank(size)) {
                payload.put("size", size);
            }
            if (seed != null && !isGptImageModel(chatModelVo.getModelName())) {
                payload.put("seed", seed);
            }
        }

        try {
            String response = postJson(chatModelVo, payload.toString(), ark);
            JsonNode root = OpenAiMediaSupport.OBJECT_MAPPER.readTree(response);
            JsonNode first = root.path("data").isArray() && !root.path("data").isEmpty() ? root.path("data").get(0) : null;
            if (first == null) {
                if (ark) {
                    throw new IllegalArgumentException("火山方舟未返回图片");
                }
                return "";
            }
            String url = OpenAiMediaSupport.text(first, "url");
            if (StrUtil.isNotBlank(url)) {
                return url;
            }
            String b64 = OpenAiMediaSupport.text(first, "b64_json");
            if (StrUtil.isBlank(b64) && ark) {
                throw new IllegalArgumentException("火山方舟未返回图片");
            }
            return StrUtil.isBlank(b64) ? "" : OpenAiMediaSupport.dataUrl("image/png", b64);
        } catch (IOException e) {
            throw new RuntimeException("图片生成失败: " + e.getMessage(), e);
        }
    }

    /** gpt-image 系列不支持 seed 参数，传入会被拒。 */
    private static boolean isGptImageModel(String modelName) {
        return StrUtil.isNotBlank(modelName) && modelName.contains("gpt-image");
    }

    @Override
    protected Object buildImageModel(ChatModelVo chatModelVo) {
        return chatModelVo;
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

    private String imageEndpoint(String apiHost, boolean ark) {
        if (!ark) {
            return OpenAiMediaSupport.endpoint(apiHost, GENERATIONS_PATH);
        }
        String base = CustomApiCredentialPolicy.normalizeBaseUrl(CustomApiCredentialPolicy.ARK_PROVIDER,
            StrUtil.trim(apiHost));
        return base.endsWith(GENERATIONS_PATH) ? base : base + GENERATIONS_PATH;
    }

    private String postJson(ChatModelVo model, String jsonBody, boolean ark) throws IOException {
        Request request = new Request.Builder()
            .url(imageEndpoint(model.getApiHost(), ark))
            .addHeader("Authorization", "Bearer " + model.getApiKey())
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonBody, OpenAiMediaSupport.JSON))
            .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (ark && !response.isSuccessful()) {
                throw new IllegalArgumentException("火山方舟图片生成失败（HTTP " + response.code() + "）");
            }
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new IllegalArgumentException("OpenAI Images API调用失败: " + response.code() + " - " + responseText);
            }
            return responseText;
        }
    }
}
