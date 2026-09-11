package org.ruoyi.ipd.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * P4-2.1 Ollama 本地 Tester。
 * <p>
 * GET {baseUrl}/api/tags，无鉴权。HTTP 2xx 视为成功；4xx/5xx 视为 HTTP_&lt;code&gt;。
 */
public final class OllamaTester implements AiProviderTester {

    private static final Set<String> ALIASES = Set.of("ollama");

    private final HttpClient http;

    public OllamaTester() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public OllamaTester(HttpClient http) {
        this.http = http;
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public Set<String> aliases() {
        return ALIASES;
    }

    @Override
    public AiTestResult test(AiTestConfig cfg) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl()) + "/api/tags"))
                .timeout(Duration.ofMillis(cfg.timeoutMs()))
                .GET()
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            int code = resp.statusCode();
            if (code >= 200 && code < 300) {
                return AiTestResult.ok(latency);
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
}
