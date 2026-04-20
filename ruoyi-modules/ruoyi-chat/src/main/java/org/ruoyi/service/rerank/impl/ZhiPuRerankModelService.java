package org.ruoyi.service.rerank.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.domain.bo.rerank.RerankRequest;
import org.ruoyi.domain.bo.rerank.RerankResult;
import org.ruoyi.domain.dto.request.ZhipuRerankRequest;
import org.ruoyi.domain.dto.response.ZhipuRerankResponse;
import org.ruoyi.service.rerank.RerankModelService;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 智谱AI 重排序模型实现
 * 参考设计模式：AliBaiLianMultiEmbeddingProvider
 *
 * @author yang
 * @date 2026-04-19
 */
@Slf4j
@Component("zhipuRerank")
public class ZhiPuRerankModelService implements RerankModelService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatModelVo chatModelVo;

    public ZhiPuRerankModelService() {
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
            ZhipuRerankRequest request = buildRequest(rerankRequest);
            ZhipuRerankResponse response = executeRequest(request);

            return response.toRerankResult(
                    rerankRequest.getDocuments().size(),
                    System.currentTimeMillis() - startTime
            );

        } catch (Exception e) {
            log.error("智谱重排序失败: {}", e.getMessage(), e);
            throw new RuntimeException("重排序服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求对象
     */
    private ZhipuRerankRequest buildRequest(RerankRequest rerankRequest) {
        return ZhipuRerankRequest.create(
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
    private ZhipuRerankResponse executeRequest(ZhipuRerankRequest request) throws IOException {
        String jsonBody = request.toJson();
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));

        // 生成智谱认证Token
        String token = generateToken(chatModelVo.getApiKey());

        // 智谱重排序固定端点路径
        String url = chatModelVo.getApiHost() + "/api/paas/v4/rerank";
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", token)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "无错误信息";
                throw new IllegalArgumentException("智谱API调用失败: " + response.code() + " - " + err);
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
    private ZhipuRerankResponse parseResponse(String responseBody) throws IOException {
        return objectMapper.readValue(responseBody, ZhipuRerankResponse.class);
    }

    /**
     * 生成智谱JWT Token
     */
    private String generateToken(String apiKey) {
        try {
            String[] apiKeyParts = apiKey.split("\\.");
            String keyId = apiKeyParts[0];
            String secret = apiKeyParts[1];

            long expireMillis = 1000L * 60 * 30; // 30分钟
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("api_key", keyId);
            payload.put("exp", System.currentTimeMillis() + expireMillis);
            payload.put("timestamp", System.currentTimeMillis());

            // 使用反射创建 MacAlgorithm（兼容不同版本的 jjwt）
            MacAlgorithm macAlgorithm;
            try {
                Class<?> c = Class.forName("io.jsonwebtoken.impl.security.DefaultMacAlgorithm");
                Constructor<?> ctor = c.getDeclaredConstructor(String.class, String.class, int.class);
                ctor.setAccessible(true);
                macAlgorithm = (MacAlgorithm) ctor.newInstance("HS256", "HmacSHA256", 128);
            } catch (Exception e) {
                macAlgorithm = Jwts.SIG.HS256;
            }

            String token = Jwts.builder()
                    .header()
                    .add("alg", "HS256")
                    .add("sign_type", "SIGN")
                    .and()
                    .content(objectMapper.writeValueAsString(payload))
                    .signWith(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), macAlgorithm)
                    .compact();

            return "Bearer " + token;
        } catch (Exception e) {
            throw new RuntimeException("生成智谱Token失败: " + e.getMessage(), e);
        }
    }
}
