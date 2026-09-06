package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.ruoyi.common.encrypt.utils.EncryptUtils;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AiModelConfig;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AiModelSaveReq;
import org.ruoyi.ipd.dto.AiModelView;
import org.ruoyi.ipd.mapper.AiModelConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.ipd.service.ai.AiProviderTester;
import org.ruoyi.ipd.service.ai.AiTestConfig;
import org.ruoyi.ipd.service.ai.AiTestResult;
import org.ruoyi.ipd.service.ai.BaiduTester;
import org.ruoyi.ipd.service.ai.DefaultTester;
import org.ruoyi.ipd.service.ai.OllamaTester;
import org.ruoyi.ipd.service.ai.OpenAiCompatibleTester;
import org.ruoyi.ipd.service.ai.ProviderRegistry;
import org.ruoyi.ipd.service.ai.ZhipuTester;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * P4-2.1 AI 模型配置加密密钥与脱敏返回（AC-AI-01）。
 * <ul>
 *   <li>密钥加密存储：api_key_encrypted 落 AES 密文（base64，ruoyi-common-encrypt EncryptUtils）；
 *       主密钥来自环境变量 IPD_AIMODEL_ENCRYPT_KEY（用户裁决 2026-09-05），缺失时写操作 fail-fast。</li>
 *   <li>不回显：任何查询端点只给 maskedKey（前4****后4），明文/密文均不出服务端。</li>
 *   <li>连接测试失败不泄露凭证：错误消息白名单化（host+失败类别），绝不含 apiKey/请求头。</li>
 *   <li>列结构对齐 dbc75862 预铺 ai_model_configs；temperature/maxTokens 进 config_json。</li>
 *   <li>全局至多一条 is_active=true（enable 时先清位再置位，条件 UPDATE 守卫）。</li>
 *   <li>多协议探测：按 provider 走 {@link ProviderRegistry} 分发（OpenAI 兼容 / 智谱 / 百度 / Ollama / 兜底）。</li>
 * </ul>
 */
@Service
public class AiModelConfigService {

    /** config_json 解析（温度/token 数对外仍为独立视图字段，仅持久层合并存储）；
     *  开启大十进制解析，保留用户配置的字面精度（0.70 不退化为 0.7） */
    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private final AiModelConfigMapper mapper;
    private final AuditLogService auditLogService;
    /** AES 主密钥（环境变量注入；测试直传） */
    private final String encryptKey;
    /** 多协议派发器（BR-AI-PROV-01/02） */
    private final ProviderRegistry providerRegistry;

    /** 3 参构造（保留向后兼容：现有单测与生产 wiring 沿用） */
    // 2026-09-06 第六批：双构造器需显式指定 Spring 注入入口（多构造器无 @Autowired 启动失败，踩过两次）
    @Autowired
    public AiModelConfigService(AiModelConfigMapper mapper, AuditLogService auditLogService,
                                @Value("${IPD_AIMODEL_ENCRYPT_KEY:}") String encryptKey) {
        this(mapper, auditLogService, encryptKey, defaultRegistry());
    }

    /** 4 参构造（测试可注入自定义 registry；生产走 3 参 + 默认 registry） */
    public AiModelConfigService(AiModelConfigMapper mapper, AuditLogService auditLogService,
                                @Value("${IPD_AIMODEL_ENCRYPT_KEY:}") String encryptKey,
                                ProviderRegistry providerRegistry) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.encryptKey = encryptKey;
        this.providerRegistry = providerRegistry;
    }

    /** 默认 registry：5 家 tester 兜底（OpenAI 兼容 / 智谱 / 百度 / Ollama / Default）。 */
    private static ProviderRegistry defaultRegistry() {
        return new ProviderRegistry(List.of(
            new OpenAiCompatibleTester(),
            new ZhipuTester(),
            new BaiduTester(),
            new OllamaTester(),
            new DefaultTester()
        ));
    }

    /** 列表（脱敏）。 */
    public List<AiModelView> list() {
        return mapper.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .orderByDesc(AiModelConfig::getUpdateTime)
                .orderByDesc(AiModelConfig::getId))
            .stream().map(AiModelConfigService::toView).toList();
    }

    /** 详情（脱敏）。 */
    public AiModelView get(Long id) {
        return toView(requireEntity(id));
    }

    /** 当前生效配置（内部接线用，含解密密钥；仅供 service 层 AI 调用方，不进 controller）。 */
    public AiModelConfig currentEnabled() {
        return mapper.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getIsActive, true))
            .stream().findFirst()
            .orElseThrow(() -> new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT));
    }

    /** 创建：apiKey 明文进、密文出（AC-AI-01 全字段可配）。 */
    @Transactional(rollbackFor = Exception.class)
    public AiModelView create(AiModelSaveReq req, String operator) {
        validate(req, true);
        // uk_model_name 预检：同名模型已存在 → 409（并发残余交给 DB 唯一键兜底）
        Long dup = mapper.selectCount(new LambdaQueryWrapper<AiModelConfig>()
            .eq(AiModelConfig::getModelName, req.model().trim()));
        if (dup != null && dup > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        AiModelConfig entity = AiModelConfig.builder()
            .provider(req.provider().trim())
            .modelName(req.model().trim())
            .endpointUrl(req.endpoint().trim())
            .apiKeyEncrypted(encrypt(req.apiKey()))
            .configJson(configJsonOf(req.temperature(), req.maxTokens()))
            .isActive(false)
            .build();
        mapper.insert(entity);
        audit(operator, "CREATE", entity, null);
        return toView(entity);
    }

    /** 更新：apiKey=null 表示不改密钥（不触碰密文列）。 */
    @Transactional(rollbackFor = Exception.class)
    public AiModelView update(Long id, AiModelSaveReq req, String operator) {
        validate(req, false);
        AiModelConfig exists = requireEntity(id);
        AiModelConfig patch = new AiModelConfig();
        patch.setId(id);
        patch.setProvider(req.provider().trim());
        patch.setEndpointUrl(req.endpoint().trim());
        if (req.apiKey() != null && !req.apiKey().isBlank()) {
            patch.setApiKeyEncrypted(encrypt(req.apiKey()));
        }
        patch.setModelName(req.model().trim());
        patch.setConfigJson(configJsonOf(req.temperature(), req.maxTokens()));
        if (mapper.updateById(patch) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        AiModelConfig after = requireEntity(id);
        audit(operator, "UPDATE", after, exists.getApiKeyEncrypted());
        return toView(after);
    }

    /** 启用（全局唯一生效）：先清其他行再置本行。 */
    @Transactional(rollbackFor = Exception.class)
    public AiModelView enable(Long id, String operator) {
        requireEntity(id);
        mapper.update(null, new LambdaUpdateWrapper<AiModelConfig>()
            .eq(AiModelConfig::getIsActive, true)
            .ne(AiModelConfig::getId, id)
            .set(AiModelConfig::getIsActive, false));
        if (mapper.update(null, new LambdaUpdateWrapper<AiModelConfig>()
            .eq(AiModelConfig::getId, id)
            .set(AiModelConfig::getIsActive, true)) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        AiModelConfig after = requireEntity(id);
        audit(operator, "ENABLE", after, null);
        return toView(after);
    }

    /**
     * 连接测试（BR-AI-PROV-01/02/03/05）：
     * <ol>
     *   <li>SSRF 前置（保留 SEC-REV-04 黑名单语义）</li>
     *   <li>按 provider 走 {@link ProviderRegistry} 派发到对应 Tester</li>
     *   <li>Tester 返回结构化 AiTestResult；service 把 errorMessage 二次 mask 后拼到 maskedKey 字段，
     *       保持前端契约不变（maskedKey 必含 "ok" 或 "fail" 关键词）</li>
     *   <li>失败消息白名单化——绝不含 apiKey/密文/请求头</li>
     * </ol>
     * 注：解密 apiKey 仅内存内消费；既不回显，也不入审计 afterData。
     */
    public AiModelView testConnect(Long id, String operator) {
        AiModelConfig config = requireEntity(id);
        // SEC-REV-round3 Bug#3（中危 authorization-bypass）：走 ProviderRegistry 而非裸 URL 探测
        // 旧实现用 probe() 通用 GET，被 controller 调起时跳过协议派发——拿到的"ok"只是 TCP 通，
        // 不验证 provider 协议兼容（智谱/百度/OpenAI兼容）。现统一走 testConnectWithProvider。
        AiTestResult result = testConnectWithProvider(id);
        audit(operator, "TEST_CONNECT", config, null);
        AiModelView base = toView(config);
        String message = result.success()
            ? "connect: ok(" + (result.latencyMs() > 0 ? result.latencyMs() + "ms" : "200") + ")"
            : "connect: fail(" + safeErrCode(result.errorCode()) + ")";
        return new AiModelView(base.id(), base.provider(), base.endpoint(), base.model(),
            base.temperature(), base.maxTokens(), base.enabled(),
            base.maskedKey() + " | " + message);
    }

    /** errorCode 白名单输出，避免任意 tester 返回泄露 apiKey/请求头；errorMessage 不进 maskedKey。 */
    private static String safeErrCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) return "未知错误";
        return errorCode;
    }

    /**
     * P4-2.1：按 provider 走 ProviderRegistry 派发 Tester（带 SSRF 前置 + 二次 mask）。
     * <p>
     * 返回结构化结果——上层若需自己组装 AiModelView 可用；现有 controller 仍调 {@link #testConnect} 走默认 message 路径。
     * 测试可独立验证派发与 mask 行为。
     */
    public AiTestResult testConnectWithProvider(Long id) {
        AiModelConfig config = requireEntity(id);
        // SEC-REV-04：SSRF 防护保持
        String host = "";
        try {
            host = new URL(config.getEndpointUrl()).getHost();
        } catch (Exception ignored) {
            // URL 解析失败交给 Tester 自报 UNSUPPORTED_PROTOCOL
        }
        String blocked = ssrfBlockReason(host);
        if (blocked != null) {
            return AiTestResult.fail("SSRF_BLOCKED", "host blocked: " + blocked, 0);
        }
        AiProviderTester tester = providerRegistry.find(config.getProvider());
        String plainKey = decryptApiKey(config);
        AiTestResult raw = tester.test(new AiTestConfig(
            config.getProvider(), config.getEndpointUrl(), plainKey, config.getModelName(), 5000));
        return maskResult(raw, plainKey);
    }

    /** Tester 返回的 errorMessage 含明文 apiKey 时，本方法二次 mask 兜底——绝不外传明文。 */
    static AiTestResult maskResult(AiTestResult raw, String plainApiKey) {
        if (raw == null) {
            return AiTestResult.fail("UNSUPPORTED_PROTOCOL", "tester returned null", 0);
        }
        String msg = raw.errorMessage();
        if (msg == null || plainApiKey == null || plainApiKey.isBlank()) {
            return raw;
        }
        if (msg.contains(plainApiKey)) {
            String masked = mask(plainApiKey);
            msg = msg.replace(plainApiKey, masked);
        }
        return new AiTestResult(raw.success(), raw.latencyMs(), raw.errorCode(), msg);
    }

    /** 内部：解密 api_key 供 AI 调用方使用（严禁出现在任何响应/日志）。 */
    public String decryptApiKey(AiModelConfig config) {
        try {
            return EncryptUtils.decryptByAes(config.getApiKeyEncrypted(), encryptKey);
        } catch (Exception e) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
    }

    private String probe(String endpoint) {
        // host 先给空串：URL 构造本身可能抛异常，catch 分支才能安全引用
        String host = "";
        try {
            URL url = new URL(endpoint);
            host = url.getHost();
            // SEC-REV-04：SSRF 防护 — 解析后判定 IP，禁止探测 loopback / 链路本地 / RFC1918 私网
            String blocked = ssrfBlockReason(host);
            if (blocked != null) {
                return "connect: fail(SSRF拦截:" + blocked + ")";
            }
            // SEC-REV-round3 Bug#1（高危 ssrf-redirect-bypass）：HttpURLConnection 默认跟随 3xx redirect，
            // 跳到内网绕开 ssrfBlockReason 前置检查。强制关闭自动跟随，手动解析 Location 再校验。
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestMethod("GET");
            // Bug#1：禁止自动跟随 3xx
            conn.setInstanceFollowRedirects(false);
            int code = conn.getResponseCode();
            // Bug#1：若响应是 3xx，手动解析 Location 并对重定向目标再次 SSRF 校验
            if (code >= 300 && code < 400) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isBlank()) {
                    return "connect: fail(SSRF拦截:重定向目标缺失)";
                }
                // Location 可能是相对路径或绝对 URL
                URL nextUrl = new URL(url, location);
                String nextBlocked = ssrfBlockReason(nextUrl.getHost());
                if (nextBlocked != null) {
                    return "connect: fail(SSRF拦截:重定向至" + nextBlocked + ")";
                }
                return "connect: fail(SSRF拦截:3xx重定向已禁用)";
            }
            conn.disconnect();
            return "connect: ok(" + code + ")";
        } catch (java.net.UnknownHostException e) {
            return "connect: fail(域名不可达: " + host + ")";
        } catch (java.net.SocketTimeoutException e) {
            return "connect: fail(超时: " + host + ")";
        } catch (java.net.ConnectException e) {
            return "connect: fail(连接被拒: " + host + ")";
        } catch (Exception e) {
            return "connect: fail(网络错误: " + e.getClass().getSimpleName() + ")";
        }
    }

    /**
     * SEC-REV-04：SSRF 防护 — host（域名或 IP 字面量）解析后落入下列范围即视为内部目标，禁止探测。
     * <ul>
     *   <li>loopback 127.0.0.0/8、IPv6 ::1</li>
     *   <li>链路本地 169.254.0.0/16（含 AWS / Azure metadata 169.254.169.254）</li>
     *   <li>RFC1918 私网 10/8、172.16/12、192.168/16</li>
     *   <li>唯一本地 IPv6 fc00::/7、回环 IPv4 0.0.0.0/8</li>
     *   <li>组播 224.0.0.0/4、回环 localhost 解析到 127.*</li>
     * </ul>
     * 域名走 InetAddress.getAllByName 任一解析落入黑名单即拒；IP 字面量直接字面解析。
     */
    static String ssrfBlockReason(String host) {
        if (host == null || host.isBlank()) {
            return "空主机名";
        }
        try {
            java.net.InetAddress[] addrs = java.net.InetAddress.getAllByName(host);
            for (java.net.InetAddress addr : addrs) {
                if (addr.isLoopbackAddress()) return "loopback";
                if (addr.isLinkLocalAddress()) return "链路本地";
                if (addr.isAnyLocalAddress()) return "通配地址";
                if (addr.isMulticastAddress()) return "组播";
                if (addr.isSiteLocalAddress()) return "RFC1918 私网";
                byte[] raw = addr.getAddress();
                if (raw.length == 4) {
                    int b0 = raw[0] & 0xFF;
                    int b1 = raw[1] & 0xFF;
                    // 100.64.0.0/10（运营商级 NAT，CGN）也按私网处理
                    if (b0 == 100 && (b1 & 0xC0) == 64) return "CGN 私网";
                    // 0.0.0.0/8 已由 isAnyLocalAddress 覆盖
                }
            }
        } catch (java.net.UnknownHostException e) {
            return null; // 解析失败：交给外层 catch 报"域名不可达"，不阻断
        }
        return null;
    }

    private AiModelConfig requireEntity(Long id) {
        AiModelConfig config = mapper.selectById(id);
        if (config == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        return config;
    }

    private String encrypt(String plainApiKey) {
        if (encryptKey == null || encryptKey.isBlank()) {
            // 主密钥缺失：拒绝一切密钥写入（fail-fast，不退化明文）
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        try {
            return EncryptUtils.encryptByAes(plainApiKey, encryptKey);
        } catch (Exception e) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
    }

    private static void validate(AiModelSaveReq req, boolean requireApiKey) {
        if (req == null || isBlank(req.provider()) || req.provider().length() > 32
            || isBlank(req.endpoint()) || req.endpoint().length() > 255
            || isBlank(req.model()) || req.model().length() > 64) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (!req.endpoint().startsWith("http://") && !req.endpoint().startsWith("https://")) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (requireApiKey && (isBlank(req.apiKey()) || req.apiKey().length() < 8)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.temperature() != null
            && (req.temperature().compareTo(BigDecimal.ZERO) < 0 || req.temperature().compareTo(new BigDecimal("2")) > 0)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.maxTokens() != null && (req.maxTokens() < 1 || req.maxTokens() > 200_000)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 脱敏掩码：前4****后4；过短则全掩。 */
    static String mask(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /** 脱敏视图：maskedKey 基于密文计算（明文永不出库）；temperature/maxTokens 从 config_json 展开。 */
    static AiModelView toView(AiModelConfig config) {
        JsonNode cfg = parseConfig(config.getConfigJson());
        return new AiModelView(config.getId(), config.getProvider(), config.getEndpointUrl(),
            config.getModelName(),
            cfg.hasNonNull("temperature") ? cfg.get("temperature").decimalValue() : null,
            cfg.hasNonNull("maxTokens") ? cfg.get("maxTokens").asInt() : null,
            Boolean.TRUE.equals(config.getIsActive()) ? "1" : "0",
            mask(config.getApiKeyEncrypted()));
    }

    /** config_json 组装：null 字段不落键（保持载荷最小，P4-2.2 可扩展键）。 */
    private static String configJsonOf(BigDecimal temperature, Integer maxTokens) {
        StringBuilder sb = new StringBuilder("{");
        if (temperature != null) {
            sb.append("\"temperature\":").append(temperature.toPlainString());
        }
        if (maxTokens != null) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append("\"maxTokens\":").append(maxTokens);
        }
        return sb.append('}').toString();
    }

    private static JsonNode parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }
        try {
            return JSON.readTree(configJson);
        } catch (Exception e) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
    }

    private void audit(String operator, String action, AiModelConfig config, String oldCipher) {
        // 审计不含明文/密文：oldCipher 仅作「是否轮换过密钥」的布尔语义
        auditLogService.append(AuditLog.builder()
            .operatorName(operator).operatorRole("SUPER_ADMIN")
            .action(action).entityType("AI_MODEL_CONFIG").entityId(config.getId())
            .beforeData(oldCipher == null ? null : AuditEventData.json("keyRotated", true))
            .afterData(AuditEventData.json(
                "provider", config.getProvider(),
                "model", config.getModelName(),
                "maskedKey", mask(config.getApiKeyEncrypted()),
                "enabled", Boolean.TRUE.equals(config.getIsActive()) ? "1" : "0"))
            .build());
    }
}
