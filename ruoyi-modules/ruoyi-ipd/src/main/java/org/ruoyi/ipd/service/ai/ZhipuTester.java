package org.ruoyi.ipd.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * P4-2.1 智谱 GLM Tester。
 * <p>
 * POST {baseUrl}/api/paas/v4/chat/completions，Authorization: Bearer {key}，body 形态与 OpenAI 兼容。
 */
public final class ZhipuTester implements AiProviderTester {

    private static final Set<String> ALIASES = Set.of("zhipu", "glm");

    private final HttpClient http;

    public ZhipuTester() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public ZhipuTester(HttpClient http) {
        this.http = http;
    }

    @Override
    public String provider() {
        return "zhipu";
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
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl()) + "/api/paas/v4/chat/completions"))
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
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
