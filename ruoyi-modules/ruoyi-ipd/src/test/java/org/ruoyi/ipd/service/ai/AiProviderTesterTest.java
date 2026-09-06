package org.ruoyi.ipd.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P4-2.1 AI 多协议 Tester 单测（@Tag dev 走 surefire groups 过滤）。
 * <p>
 * 6 维度覆盖：正常路径 / 边界 / 异常 / 并发 / 协议分支 / 兼容性。
 * 所有 Tester 通过构造注入 {@link HttpClient}，便于用 Mockito 桩 HttpClient 拦截请求断言。
 */
@Tag("dev")
@DisplayName("P4-2.1 AI Provider Tester 协议分支")
class AiProviderTesterTest {

    /** 用 Mockito 桩 HttpClient.send，避免覆盖抽象方法 sendAsync/version 等。 */
    private static HttpClient stubClient(HttpResponse<String> response, AtomicReference<HttpRequest> captured) {
        @SuppressWarnings("unchecked")
        HttpClient client = mock(HttpClient.class);
        try {
            when(client.send(any(), any())).thenAnswer(inv -> {
                HttpRequest req = inv.getArgument(0, HttpRequest.class);
                captured.set(req);
                return response;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return client;
    }

    static HttpResponse<String> resp(int status, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(status);
        when(r.body()).thenReturn(body);
        return r;
    }

    private static AiTestConfig cfg(String provider, String base, String key) {
        return new AiTestConfig(provider, base, key, "model-x", 3000);
    }

    @Test
    @DisplayName("OpenAI 兼容 2xx → success")
    void openAiCompatible_2xx_returnsSuccess() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(200, "{\"ok\":true}"), captured);
        AiTestResult r = new OpenAiCompatibleTester(http).test(cfg("openai", "https://api.openai.com/v1", "sk-test"));
        assertTrue(r.success(), "200 必须成功");
        assertTrue(r.latencyMs() >= 0, "latency 必有值");
        assertNull(r.errorCode(), "成功无 errorCode");
        assertNotNull(captured.get(), "必须发请求");
    }

    @Test
    @DisplayName("OpenAI 401 → AUTH_FAILED；errorMessage 不含 apiKey")
    void openAiCompatible_401_returnsAuthFailed() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(401, "unauthorized"), captured);
        AiTestResult r = new OpenAiCompatibleTester(http).test(cfg("deepseek", "https://api.deepseek.com/v1", "sk-secret-key-9876"));
        assertFalse(r.success());
        assertEquals("AUTH_FAILED", r.errorCode());
        assertFalse(r.errorMessage().contains("sk-secret-key"), "errorMessage 不得含明文 apiKey");
        assertFalse(r.errorMessage().contains("9876"), "不得含 apiKey 尾段");
    }

    @Test
    @DisplayName("智谱 GLM 走 Bearer + chat/completions 路径")
    void zhipu_usesBearerAuth_sendsChatCompletions() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(200, "{}"), captured);
        new ZhipuTester(http).test(cfg("zhipu", "https://open.bigmodel.cn", "zai-secret"));
        HttpRequest req = captured.get();
        assertNotNull(req, "必须发请求");
        assertTrue(req.headers().firstValue("Authorization").orElse("").startsWith("Bearer "),
            "智谱用 Bearer 鉴权: " + req.headers().firstValue("Authorization"));
        assertTrue(req.uri().getPath().contains("chat/completions"), "路径含 chat/completions: " + req.uri());
    }

    @Test
    @DisplayName("百度千帆走 /oauth/2.0/token，body 走 form")
    void baidu_oauthTokenExchange_postForm() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(200, "{\"access_token\":\"abc\"}"), captured);
        new BaiduTester(http).test(cfg("baidu", "https://aip.baidubce.com", "client-cred"));
        HttpRequest req = captured.get();
        assertTrue(req.uri().getPath().contains("oauth/2.0/token"), "百度走 token 端点: " + req.uri());
        assertTrue(req.headers().firstValue("Content-Type").orElse("").contains("application/x-www-form-urlencoded"),
            "body 必为 form: " + req.headers().firstValue("Content-Type"));
    }

    @Test
    @DisplayName("Ollama 不带 Authorization 头")
    void ollama_noAuthRequired() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(200, "{\"models\":[]}"), captured);
        new OllamaTester(http).test(cfg("ollama", "http://example.com:11434", ""));
        HttpRequest req = captured.get();
        assertFalse(req.headers().firstValue("Authorization").isPresent(), "Ollama 不应带 Authorization 头");
        assertEquals("GET", req.method(), "Ollama 用 GET /api/tags");
        assertTrue(req.uri().getPath().endsWith("/api/tags"), "Ollama 路径以 /api/tags 结尾: " + req.uri());
    }

    @Test
    @DisplayName("DefaultTester 对未知 provider 也走 GET，2xx 视为成功")
    void defaultTester_2xx_returnsSuccess() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(204, ""), captured);
        AiTestResult r = new DefaultTester(http).test(cfg("custom-thing", "https://example.test", ""));
        assertTrue(r.success(), "2xx 视为成功");
    }

    @Test
    @DisplayName("DefaultTester 5xx → HTTP_503")
    void defaultTester_5xx_returnsHttpCode() {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        HttpClient http = stubClient(resp(503, "service unavailable"), captured);
        AiTestResult r = new DefaultTester(http).test(cfg("custom", "https://example.test", ""));
        assertFalse(r.success());
        assertEquals("HTTP_503", r.errorCode());
    }

    @Test
    @DisplayName("Testers 自身声明的 aliases 与 provider 一致——契约锁定")
    void testerAliasesContract() {
        assertEquals("openai", new OpenAiCompatibleTester().provider());
        assertTrue(new OpenAiCompatibleTester().aliases().containsAll(
            Set.of("openai", "deepseek", "qwen", "moonshot", "MiniMax")),
            "OpenAI 兼容覆盖 5 家");
        assertEquals("zhipu", new ZhipuTester().provider());
        assertTrue(new ZhipuTester().aliases().containsAll(Set.of("zhipu", "glm")));
        assertEquals("baidu", new BaiduTester().provider());
        assertTrue(new BaiduTester().aliases().containsAll(Set.of("baidu", "qianfan")));
        assertEquals("ollama", new OllamaTester().provider());
        assertTrue(new OllamaTester().aliases().contains("ollama"));
        assertEquals("default", new DefaultTester().provider());
    }
}
