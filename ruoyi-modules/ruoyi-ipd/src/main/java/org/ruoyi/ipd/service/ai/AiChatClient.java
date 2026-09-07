package org.ruoyi.ipd.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * P4-2.2 生成调用器：OpenAI 兼容 chat/completions 非流式单轮。
 * <p>
 * 覆盖 openai / deepseek / qwen / moonshot / MiniMax 及其兼容层（智谱/百度/Ollama 均提供
 * OpenAI 兼容端点）；P4-2.1 的多协议探测仍走 ProviderRegistry，生成本版统一 OpenAI 兼容面。
 * <p>错误码与 P4-2.1 Tester 白名单同源（AUTH_FAILED / HTTP_n / TIMEOUT / UNREACHABLE /
 * EMPTY_RESPONSE / UNSUPPORTED_PROTOCOL）；apiKey 仅拼入请求头内存消费，
 * 绝不进日志/审计/异常消息。模型输出透传不过滤（BR-AI-04）。
 */
@Component
public class AiChatClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;

    public AiChatClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    /** 测试桩入口：注入自定义 HttpClient（单测拦截）。 */
    public AiChatClient(HttpClient http) {
        this.http = http;
    }

    /**
     * 生成结果。
     *
     * @param content          模型全文（原样透传）
     * @param promptTokens     usage.prompt_tokens（响应缺失记 0）
     * @param completionTokens usage.completion_tokens（响应缺失记 0）
     * @param errorCode        失败类别（成功时 null）
     */
    public record AiChatResult(boolean success, String content, int promptTokens, int completionTokens,
                               long latencyMs, String errorCode, String errorMessage) {

        public static AiChatResult ok(String content, int promptTokens, int completionTokens, long latencyMs) {
            return new AiChatResult(true, content, Math.max(0, promptTokens), Math.max(0, completionTokens),
                Math.max(0, latencyMs), null, null);
        }

        public static AiChatResult fail(String code, String message, long latencyMs) {
            return new AiChatResult(false, null, 0, 0, Math.max(0, latencyMs), code, message);
        }
    }

    /**
     * 单轮生成。cfg 复用 P4-2.1 的 {@link AiTestConfig}（provider/endpoint/key/model/timeoutMs）；
     * maxTokens/temperature 为 null 时不携带对应请求键（由服务端默认值决定）。
     */
    public AiChatResult chat(AiTestConfig cfg, String prompt, Integer maxTokens, BigDecimal temperature) {
        long start = System.currentTimeMillis();
        try {
            StringBuilder body = new StringBuilder("{\"model\":\"").append(jsonEscape(cfg.modelName()))
                .append("\",\"messages\":[{\"role\":\"user\",\"content\":\"").append(jsonEscape(prompt))
                .append("\"}");
            if (maxTokens != null && maxTokens > 0) {
                body.append(",\"max_tokens\":").append(maxTokens);
            }
            if (temperature != null) {
                body.append(",\"temperature\":").append(temperature.toPlainString());
            }
            body.append('}');
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(cfg.timeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cfg.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            int code = resp.statusCode();
            if (code < 200 || code >= 300) {
                if (code == 401 || code == 403) {
                    return AiChatResult.fail("AUTH_FAILED", "HTTP " + code, latency);
                }
                return AiChatResult.fail("HTTP_" + code, "HTTP " + code, latency);
            }
            JsonNode root = JSON.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                return AiChatResult.fail("EMPTY_RESPONSE", "模型返回空内容", latency);
            }
            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);
            return AiChatResult.ok(content, promptTokens, completionTokens, latency);
        } catch (java.net.http.HttpTimeoutException e) {
            return AiChatResult.fail("TIMEOUT", "generate: timeout", System.currentTimeMillis() - start);
        } catch (java.net.ConnectException e) {
            return AiChatResult.fail("UNREACHABLE", "connect: refused", System.currentTimeMillis() - start);
        } catch (java.net.UnknownHostException e) {
            return AiChatResult.fail("UNREACHABLE", "connect: unknown host", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return AiChatResult.fail("UNSUPPORTED_PROTOCOL",
                "generate: " + e.getClass().getSimpleName(), System.currentTimeMillis() - start);
        }
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
