package org.ruoyi.ipd.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * host allowlist（可选）。逗号分隔的公网供应商域名；为空则放行所有公网（向后兼容，留警告）。
     * Why: P1-3/P1-15 安全审查——AiModelConfig.baseUrl 来自运营配置可被注入内网 / loopback /
     * 169.254.169.254 元数据端点，配合 Bearer 转发可能让 SSRF 命中点伪装为已认证用户。
     */
    private final String allowedHosts;

    public AiChatClient(@Value("${ai.debug.enabled:false}") boolean debugEnabled,
                        @Value("${ai.allowed-hosts:}") String allowedHosts) {
        this.debugEnabled = debugEnabled;
        this.allowedHosts = allowedHosts == null ? "" : allowedHosts;
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
        this.allowedHosts = "";
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
            // P4-2.2 收口：改用 Jackson Map→JSON 序列化，消除手工拼串 + 自写 jsonEscape 带来的
            // 转义不全 / 结构漂移 / messages 数组闭合遗漏 等历史缺陷（dea95fc0 / baf90683）。
            // LinkedHashMap 保 key 顺序，便于日志 reqHash 稳定可对账。
            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("model", cfg.modelName());
            bodyMap.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            if (maxTokens != null && maxTokens > 0) {
                bodyMap.put("max_tokens", maxTokens);
            }
            if (temperature != null) {
                bodyMap.put("temperature", temperature);
            }
            // writeValueAsString 失败属于协议层异常（无法构造请求体），由下方 catch (Exception e)
            // 统一归类为 UNSUPPORTED_PROTOCOL（与 Jackson 解析响应错同源处理）。
            String reqBody = JSON.writeValueAsString(bodyMap);
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
            // SSRF 防御（SEC P1-3/P1-15）：发请求前解析 host + IP，做内网黑名单 + 公网 allowlist 校验。
            validateEndpoint(cfg.baseUrl());
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


    /**
     * SSRF 防御（SEC P1-3/P1-15）：先解析 baseUrl 的 host，再解析 IP。
     * 拒绝：RFC1918 10/8、172.16/12、192.168/16、loopback 127/8、link-local 169.254/16（含 AWS 元数据）、
     * IPv6 ::1、IPv6 fc00::/7。allowlist 留空时仅做内网黑名单（向后兼容）。
     */
    private void validateEndpoint(String baseUrl) {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost();
        if (host == null) {
            throw new IpdBusinessException("endpoint host missing");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IpdBusinessException("endpoint host unresolvable: " + host);
        }
        byte[] ip = addr.getAddress();
        if (isBlockedIp(ip)) {
            log.warn("[AI] SSRF blocked endpoint host={} ip={}", host, addr.getHostAddress());
            throw new IpdBusinessException("SSRF blocked: private/loopback/link-local endpoint " + host);
        }
        String allowList = allowedHosts.trim();
        if (!allowList.isEmpty()) {
            boolean ok = Arrays.stream(allowList.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .anyMatch(h -> host.equalsIgnoreCase(h) || host.endsWith("." + h));
            if (!ok) {
                log.warn("[AI] host not in allowlist host={}", host);
                throw new IpdBusinessException("host not in allowlist: " + host);
            }
        }
    }

    /** 内网 / loopback / link-local IP 黑名单，覆盖 IPv4 + IPv6。 */
    private static boolean isBlockedIp(byte[] ip) {
        if (ip.length == 4) {
            int b0 = ip[0] & 0xFF;
            if (b0 == 127) return true;
            if (b0 == 10) return true;
            if (b0 == 172 && (ip[1] & 0xF0) == 16) return true;
            if (b0 == 192 && (ip[1] & 0xFF) == 168) return true;
            if (b0 == 169 && (ip[1] & 0xFF) == 254) return true;
            return false;
        }
        if (ip.length == 16) {
            if (ip[0] == (byte) 0x7f) return true;
            if ((ip[0] & (byte) 0xFE) == (byte) 0xFC) return true;
        }
        return false;
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
