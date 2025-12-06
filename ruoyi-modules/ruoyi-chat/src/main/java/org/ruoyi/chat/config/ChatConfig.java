package org.ruoyi.chat.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.ruoyi.common.chat.openai.OpenAiStreamClient;
import org.ruoyi.common.chat.openai.function.KeyRandomStrategy;
import org.ruoyi.common.chat.openai.interceptor.OpenAILogger;
import org.ruoyi.common.core.service.ConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Chat配置类
 *
 * @date: 2023/5/16
 */
@Configuration
@RequiredArgsConstructor
public class ChatConfig {

    private final ConfigService configService;
    @Getter
    private OpenAiStreamClient openAiStreamClient;

    public static OpenAiStreamClient createOpenAiStreamClient(String apiHost, String apiKey) {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .build();
        return OpenAiStreamClient.builder()
                .apiHost(apiHost)
                .apiKey(Collections.singletonList(apiKey))
                .keyStrategy(new KeyRandomStrategy())
                .okHttpClient(okHttpClient)
                .build();
    }

    @Bean
    public OpenAiStreamClient openAiStreamClient() {
        String apiHost = configService.getConfigValue("chat", "apiHost");
        String apiKey = configService.getConfigValue("chat", "apiKey");
        openAiStreamClient = createOpenAiStreamClient(apiHost, apiKey);
        return openAiStreamClient;
    }
}
