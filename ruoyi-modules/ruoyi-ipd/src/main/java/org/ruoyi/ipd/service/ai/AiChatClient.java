package org.ruoyi.ipd.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * P4-2.2 生成调用器：OpenAI 兼容 chat/completions 非流式单轮。
 * <p>
 * 覆盖 openai / deepseek / qwen / moonshot / MiniMax 及其兼容层（智谱/百度/Ollama 均提供
 * OpenAI 兼容端点）；P4-2.1 的多协议探测仍走 ProviderRegistry，生成本版统一 OpenAI 兼容面。
 * <p>错误码与 P4-2.1 Tester 白名单同源（AUTH_FAILED / HTTP_n / TIMEOUT / UNREACHABLE /
 * EMPTY_RESPONSE / UNSUPPORTED_PROTOCOL）；apiKey 仅拼入请求头内存消费，
 * 绝不进日志/审计/异常消息。模型输出透传不过滤（BR-AI-04）。
 */
@Slf4j
@Component
public class AiChatClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    /**
     * 排障 DEBUG 开关。默认 false —— prompt/响应原文一律不进日志、审计、异常消息（BR-AI-04）。
     * 仅当 owner 在 yml 显式打开（如生产排障）才会以 DEBUG 级输出 body 完整内容。
     * Why: P4-2.2 真机收口 commit dea95fc0 自动安全审查触发：
     *   - [HIGH] bodyTail 落日志（即使不含 apiKey，prompt 本身属用户隐私）
     *   - [MEDIUM] respBody.substring(0,300) 落日志（供应商响应可能含用户数据）
     *   改用结构化字段 + SHA256 短指纹，保留请求/响应配对能力。
     */
    private final boolean debugEnabled;

    public AiChatClient(@Value("${ai.debug.enabled:false}") boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
        // 显式 HTTP/1.1：避免部分模型网关（MiniMax 等）对 h2 协商 POST 的兼容性差异（P4-2.2 真机验收 400 排查）
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** 测试桩入口：注入自定义 HttpClient（单测拦截）。默认 debugEnabled=false。 */
    public AiChatClient(HttpClient http) {
        this(http, false);
    }

    /** 测试桩入口（显式控制 debug）。 */
    public AiChatClient(HttpClient http, boolean debugEnabled) {
        this.http = http;
        this.debugEnabled = debugEnabled;
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
                // P4-2.2 真机验收修复：此处必须闭 messages 数组——缺 "]" 产生非法 JSON，
                // MiniMax 返回 400 Syntax error（单测 Mock 了 chatClient，无法暴露拼串缺陷）
                .append("\"}]");
            if (maxTokens != null && maxTokens > 0) {
                body.append(",\"max_tokens\":").append(maxTokens);
            }
            if (temperature != null) {
                body.append(",\"temperature\":").append(temperature.toPlainString());
            }
            body.append('}');
            String reqBody = body.toString();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(cfg.timeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cfg.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                .build();
            // 排障观测：仅落非敏感结构化字段（model/bodyLen/promptLen/maxTokens/temperature），
            // 配合 reqHash 短指纹便于请求/响应配对。prompt 与响应原文一律不进日志（BR-AI-04 / P4-2.2 安全审查闭环）。
            int promptLen = prompt == null ? 0 : prompt.length();
            String reqHash = shortHash(reqBody);
            log.warn("[AI] chat request: model={} bodyLen={} promptLen={} maxTokens={} temperature={} reqHash={}",
                cfg.modelName(), reqBody.length(), promptLen,
                maxTokens == null ? "default" : maxTokens.toString(),
                temperature == null ? "default" : temperature.toPlainString(),
                reqHash);
            if (debugEnabled) {
                log.debug("[AI][debug] reqBody={}", reqBody);
            }
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            int code = resp.statusCode();
            if (code < 200 || code >= 300) {
                // 排障观测：非 2xx 时仅落 code / respLen / respHash（短指纹），响应原文不进日志；
                // 不含 apiKey、不进 API 响应/审计。
                String respBody = resp.body() == null ? "" : resp.body();
                log.warn("[AI] chat non-2xx: code={} latency={}ms respLen={} respHash={} reqHash={}",
                    code, latency, respBody.length(), shortHash(respBody), reqHash);
                if (debugEnabled) {
                    log.debug("[AI][debug] non2xxBody={}", respBody);
                }
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

    /** SHA-256 短指纹（16 hex chars ≈ 64 bit），用于日志中请求/响应配对，不暴露原文。 */
    private static String shortHash(String s) {
        if (s == null || s.isEmpty()) return "0";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "na";
        }
    }
}
