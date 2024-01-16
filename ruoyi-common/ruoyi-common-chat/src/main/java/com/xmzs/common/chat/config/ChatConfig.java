package com.xmzs.common.chat.config;


import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import com.xmzs.common.chat.openai.OpenAiStreamClient;
import com.xmzs.common.chat.openai.function.KeyRandomStrategy;
import com.xmzs.common.chat.openai.interceptor.OpenAILogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * chat配置类
 *
 * @author: wangle
 * @date: 2023/5/16
 */
@Configuration
public class ChatConfig {
    @Value("${chat.apiKey}")
    private List<String> apiKey;
    @Value("${chat.apiHost}")
    private String apiHost;

    @Bean(name = "openAiStreamClient")
    public OpenAiStreamClient openAiStreamClient() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient
            .Builder()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(600, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .build();
        return OpenAiStreamClient
            .builder()
            .apiHost(apiHost)
            .apiKey(apiKey)
            //自定义key使用策略 默认随机策略
            .keyStrategy(new KeyRandomStrategy())
            .okHttpClient(okHttpClient)
            .build();
    }
}
