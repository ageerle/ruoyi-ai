package org.ruoyi.ipd.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * P4-2.1 兜底 Tester（BR-AI-PROV-01：未匹配 provider 走 default）。
 * <p>
 * GET {baseUrl}，无鉴权；2xx 成功；其他视为 HTTP_&lt;code&gt;。保留简单回退形态以兼容不可识别供应商。
 */
public final class DefaultTester implements AiProviderTester {

    private static final Set<String> ALIASES = Set.of("default");

    private final HttpClient http;

    public DefaultTester() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public DefaultTester(HttpClient http) {
        this.http = http;
    }

    @Override
    public String provider() {
        return "default";
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
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl())))
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
}
