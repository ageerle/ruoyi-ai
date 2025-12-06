package org.ruoyi.chat.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.domain.bo.ChatModelBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatModelService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author WangLe
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class MjOkHttpUtil {

    private static final String API_SECRET_HEADER = "mj-api-secret";
    private final IChatModelService chatModelService;
    private final ConfigService configService;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();
    private String apiKey;
    private String apiHost;

    public String executeRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            // 这里应根据实际情况使用适当的日志记录方式
            log.error("请求失败: {}", e.getMessage());
            return null;
        }
    }

    public Request createPostRequest(String url, String json) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        return new Request.Builder()
                .url(apiHost + url)
                .post(body)
                .header(API_SECRET_HEADER, apiKey)
                .build();
    }

    public Request createGetRequest(String url) {
        return new Request.Builder()
                .url(apiHost + url)
                .header(API_SECRET_HEADER, apiKey)
                .build();
    }

    @PostConstruct
    public void init() {
        ChatModelBo sysModelBo = new ChatModelBo();
        sysModelBo.setModelName("");
        ChatModelVo midjourney = chatModelService.selectModelByName("midjourney");
        if (midjourney != null) {
            this.apiKey = midjourney.getApiKey();
            this.apiHost = midjourney.getApiHost();
        }
    }

    public String getKey(String key) {
        return configService.getConfigValue("mj", key);
    }
}

