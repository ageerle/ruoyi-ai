package org.ruoyi.ipd.service.ai;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * P4-2.1 百度千帆 Tester。
 * <p>
 * POST {baseUrl}/oauth/2.0/token，Content-Type=application/x-www-form-urlencoded，
 * body=grant_type=client_credentials&amp;client_id={key}&amp;client_secret=（apiKey 即 client_secret）。
 * 注：百度千帆的 apiKey 字段语义上对应 client_credentials 模式下的 client_id，
 * 而 client_secret 走 configJson 或同字段——本 Tester 仅做"凭据可发"探测，
 * 真实模型调用在 P4-2.2 拓展。HTTP 2xx 视为成功。
 */
public final class BaiduTester implements AiProviderTester {

    private static final Set<String> ALIASES = Set.of("baidu", "qianfan");

    private final HttpClient http;

    public BaiduTester() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public BaiduTester(HttpClient http) {
        this.http = http;
    }

    @Override
    public String provider() {
        return "baidu";
    }

    @Override
    public Set<String> aliases() {
        return ALIASES;
    }

    @Override
    public AiTestResult test(AiTestConfig cfg) {
        long start = System.currentTimeMillis();
        try {
            // 凭据探测：模拟 client_credentials 模式，把 apiKey 拆为 client_id + client_secret 占位
            // （生产模式 P4-2.2 再细化密钥分离；本卡只验"凭据可发"）
            String body = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(cfg.apiKey(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(cfg.apiKey(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(cfg.baseUrl()) + "/oauth/2.0/token"))
                .timeout(Duration.ofMillis(cfg.timeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
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
}
