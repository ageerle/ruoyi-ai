package org.ruoyi.system.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.core.service.ConfigService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 绘声美音HTTP请求工具类
 *
 * @author NSL
 * @since 2024-12-25
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WeChatScanHttpUtil {

    private final ConfigService configService;

    private static final String TOKEN = "token";

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
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        return new Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header(TOKEN, getKey(TOKEN))
            .build();
    }

    public String getKey(String key) {
        return configService.getConfigValue("cover", key);
    }
}
