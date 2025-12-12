package org.ruoyi.common.core.utils;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OkHttpUtil {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.SECONDS)
            .writeTimeout(3000, TimeUnit.SECONDS)
            .readTimeout(3000, TimeUnit.SECONDS)
            .build();
    @Setter
    private String apiHost;
    @Setter
    private String apiKey;

    public String executeRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Request failed: {}", response);
                throw new IOException("Unexpected code " + response);
            }
            return response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            log.error("Request execution failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public Request createPostRequest(String url, String json) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        return new Request.Builder()
                .url(apiHost + url)
                .post(body)
                .header("Authorization", apiKey)
                .build();
    }

    public Request createGetRequest(String url) {
        return new Request.Builder()
                .url(apiHost + url)
                .header("Authorization", apiKey)
                .build();
    }
}
