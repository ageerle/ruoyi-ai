package org.ruoyi.ipd.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * P4-2.1 OpenAI 兼容协议 Tester。
 * <p>
 * 覆盖：openai / deepseek / qwen / moonshot / MiniMax（OpenAI Chat Completions 兼容）。
 * POST {baseUrl}/chat/completions，Authorization: Bearer {key}，body 为最小 chat 请求（max_tokens=1）。
 * HTTP 2xx 视为成功；401/403 → AUTH_FAILED；其他 4xx/5xx → HTTP_&lt;code&gt;。
 */
public final class OpenAiCompatibleTester implements AiProviderTester {

    private static final Set<String> ALIASES = Set.of("openai", "deepseek", "qwen", "moonshot", "MiniMax");

    private final HttpClient http;

    public OpenAiCompatibleTester() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    /** 测试桩入口：注入自定义 HttpClient（单测拦截）。 */
    public OpenAiCompatibleTester(HttpClient http) {
        this.http = http;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public Set<String> aliases() {
        return ALIASES;
    }

    @Override
    public AiTestResult test(AiTestConfig cfg) {
        long start = System.currentTimeMillis();
        try {
            String body = "{\"model\":\"" + jsonEscape(cfg.modelName()) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\".\"}],"
                + "\"max_tokens\":1}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl()) + "/chat/completions"))
                .timeout(Duration.ofMillis(cfg.timeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cfg.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                return AiTestResult.ok(latency);
            }
            if (code == 401 || code == 403) {
                return AiTestResult.fail("AUTH_FAILED", "HTTP " + code, latency);
            }
            return AiTestResult.fail("HTTP_" + code, "HTTP " + code, latency);
        } catch (java.net.http.HttpTimeoutException e) {
            return AiTestResult.fail("TIMEOUT", "connect: timeout", System.currentTimeMillis() - start);
        } catch (java.net.ConnectException e) {
            return AiTestResult.fail("UNREACHABLE", "connect: refused", System.currentTimeMillis() - start);
        } catch (java.net.UnknownHostException e) {
            return AiTestResult.fail("UNREACHABLE", "connect: unknown host", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return AiTestResult.fail("UNSUPPORTED_PROTOCOL",
                "connect: " + e.getClass().getSimpleName(), System.currentTimeMillis() - start);
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
