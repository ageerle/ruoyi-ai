package org.ruoyi.service.rerank.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.domain.dto.request.AliBaiLianRerankRequest;
import org.ruoyi.domain.dto.response.AliBaiLianRerankResponse;
import org.ruoyi.service.rerank.RerankModelService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 阿里百炼重排序模型实现
 * 参考设计模式：AliBaiLianMultiEmbeddingProvider
 *
 * @author yang
 * @date 2026-04-20
 */
@Slf4j
@Component("qianwenRerank")
public class AliBaiLianRerankModelService implements RerankModelService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatModelVo chatModelVo;

    public AliBaiLianRerankModelService() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void configure(ChatModelVo config) {
        this.chatModelVo = config;
    }

    @Override
    public RerankResult rerank(RerankRequest rerankRequest) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求
            AliBaiLianRerankRequest request = buildRequest(rerankRequest);
            AliBaiLianRerankResponse response = executeRequest(request);

            return response.toRerankResult(
                    rerankRequest.getDocuments().size(),
                    System.currentTimeMillis() - startTime
            );

        } catch (Exception e) {
            log.error("阿里百炼重排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("重排序服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求对象
     */
    private AliBaiLianRerankRequest buildRequest(RerankRequest rerankRequest) {
        return AliBaiLianRerankRequest.create(
                chatModelVo.getModelName(),
                rerankRequest.getQuery(),
                rerankRequest.getDocuments(),
                rerankRequest.getTopN(),
                rerankRequest.getReturnDocuments()
        );
    }

    /**
     * 执行HTTP请求并解析响应
     */
    private AliBaiLianRerankResponse executeRequest(AliBaiLianRerankRequest request) throws IOException {
        String jsonBody = request.toJson();
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

        // 阿里百炼重排序 OpenAI兼容端点
        String url = chatModelVo.getApiHost() + "/compatible-api/v1/reranks";
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + chatModelVo.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "无错误信息";
                throw new IllegalArgumentException("阿里百炼API调用失败: " + response.code() + " - " + err);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IllegalArgumentException("响应体为空");
            }

            return parseResponse(responseBody.string());
        }
    }

    /**
     * 解析响应
     */
    private AliBaiLianRerankResponse parseResponse(String responseBody) throws IOException {
        return objectMapper.readValue(responseBody, AliBaiLianRerankResponse.class);
    }
}
