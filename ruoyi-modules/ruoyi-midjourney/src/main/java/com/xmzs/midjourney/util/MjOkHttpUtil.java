package com.xmzs.midjourney.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author WangLe
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class MjOkHttpUtil {

    @Value("${chat.apiKey}")
    private List<String> apiKey;
    @Value("${chat.apiHost}")
    private String apiHost;

    private static final String API_SECRET_HEADER = "mj-api-secret";

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .build();

    public String executeRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            // 这里应根据实际情况使用适当的日志记录方式
            log.error("请求失败: {}",e.getMessage());
            return null;
        }
    }

    public Request createPostRequest(String url, String json) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        return new Request.Builder()
            .url(apiHost + url)
            .post(body)
            .header(API_SECRET_HEADER, apiKey.get(0))
            .build();
    }

    public Request createGetRequest(String url) {
        return new Request.Builder()
            .url(apiHost + url)
            .header(API_SECRET_HEADER, apiKey.get(0))
            .build();
    }

}

