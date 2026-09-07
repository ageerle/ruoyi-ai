package org.ruoyi.ipd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AiDocument;
import org.ruoyi.ipd.domain.AiModelConfig;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AiGenerateReq;
import org.ruoyi.ipd.mapper.AiDocumentMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.service.ai.AiChatClient;
import org.ruoyi.ipd.service.ai.AiTestConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * P4-2.2 AI 生成编排（AC-AI-02 / AC-AI-08 / AC-AI-09 / AC-AI-10；BR-AI-01/02/04）。
 * <ul>
 *   <li>生效配置：取全局唯一 is_active 配置（P4-2.1 currentEnabled），无配置 → STATE_CONFLICT；</li>
 *   <li>SSRF 前置：与 P4-2.1 testConnect 同源黑名单（SEC-REV-04）；</li>
 *   <li>月度 token 预算（AC-AI-08）：config_json.budgetTokens（缺省 0=不限，BR-AI-01 配置项）；
 *       已用 = ai_documents 本月 token 聚合（成功落库才计入 ⇒ 预算天然原子占用，失败零占用、
 *       无需预扣/回滚表）；预估 = 已用 + prompt 字符数上界 + maxTokens 配置；</li>
 *   <li>并发限流（BR-AI-01）：进程级信号量，满载 2 秒拿不到 → RATE_LIMITED（40011）；
 *       finally 释放 ⇒ 失败自动让出配额；</li>
 *   <li>超时（BR-AI-01）：config_json.generateTimeoutMs，缺省 60s，钳 [10s, 120s]；</li>
 *   <li>成功：{@link AiDocumentService#createGenerated} 登记版本链 v1（GENERATED=待审核，
 *       BR-AI-02，AC-AI-02「标记待审核」）+ 审计含 token 消耗与耗时（AC-AI-09）；</li>
 *   <li>透传不过滤（BR-AI-04）：模型输出原样入库，风险把控在人工审核 + UI 风险提示；
 *       prompt 全文不入审计（PM 原始资料敏感，仅记长度）。</li>
 * </ul>
 */
@Service
public class AiGenerationService {

    /** 生成默认超时 60s（卡 P4-2.2 基线） */
    static final int DEFAULT_TIMEOUT_MS = 60_000;
    /** 超时上界钳制（防配置把线程挂死） */
    static final int MAX_TIMEOUT_MS = 120_000;
    static final int MIN_TIMEOUT_MS = 10_000;
    /** 并发闸常量（BR-AI-01 限流配置键 maxConcurrent 预留，本版走常量） */
    static final int MAX_CONCURRENT = 3;
    /** prompt 字符上界（防滥用；合同未定值，工程上限） */
    static final int MAX_PROMPT_LEN = 30_000;
    /** config_json 扩展键（P4-2.1 domain 注释预留） */
    static final String EXT_TIMEOUT = "generateTimeoutMs";
    static final String EXT_BUDGET = "budgetTokens";
    /** 未配置 maxTokens 时预算预估的兜底 */
    static final int DEFAULT_PLAN_MAX_TOKENS = 4096;

    /** package-private：同包测试可直接占满/释放，验证限流与失败让位 */
    final Semaphore gate = new Semaphore(MAX_CONCURRENT);

    private final AiDocumentMapper documentMapper;
    private final AiDocumentService documentService;
    private final AiModelConfigService modelConfigService;
    private final AuditLogService auditLogService;
    private final AiChatClient chatClient;
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    public AiGenerationService(AiDocumentMapper documentMapper, AiDocumentService documentService,
                               AiModelConfigService modelConfigService, AuditLogService auditLogService,
                               AiChatClient chatClient) {
        this.documentMapper = documentMapper;
        this.documentService = documentService;
        this.modelConfigService = modelConfigService;
        this.auditLogService = auditLogService;
        this.chatClient = chatClient;
    }

    /** 测试口：注入固定时钟（月度预算窗口断言）；生产走系统时钟。 */
    AiGenerationService withClock(java.time.Clock fixed) {
        this.clock = fixed;
        return this;
    }

    /**
     * 生成并登记 v1。任何失败路径均写 AI_GENERATE_FAILED 审计（排查/对账），
     * 成功路径写 AI_GENERATE 审计（AC-AI-09：token 消耗与耗时）。
     *
     * @return 版本链首环（versionNo=1，status=GENERATED 待审核，含 model/token 用量）
     */
    public AiDocument generate(IpdActor actor, AiGenerateReq req) {
        validateActorAndReq(actor, req);

        AiModelConfig config = modelConfigService.currentEnabled();
        JsonNode cfg = parseConfigJson(config.getConfigJson());
        int timeoutMs = clampTimeout(cfg.path(EXT_TIMEOUT).asInt(0));

        // SEC-REV-04 同源 SSRF 前置：端点解析落入黑名单直接拒（不发起任何出站请求）
        String blocked = AiModelConfigService.ssrfBlockReason(hostOf(config.getEndpointUrl()));
        if (blocked != null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "模型端点不可用: " + blocked);
        }

        // AC-AI-08 月度预算预检（budgetTokens 缺省 0 = 不限）
        if (budgetOf(cfg) > 0) {
            long used = usedTokensThisMonth();
            long planMax = cfg.path("maxTokens").asInt(0) > 0
                ? cfg.path("maxTokens").asInt(0) : DEFAULT_PLAN_MAX_TOKENS;
            if (used + req.prompt().length() + planMax > budgetOf(cfg)) {
                failAudit(actor, config, req, "BUDGET_EXCEEDED", 0);
                throw new IpdBusinessException(ApiV1ErrorCode.AI_BUDGET_EXCEEDED);
            }
        }

        boolean acquired = false;
        try {
            acquired = gate.tryAcquire(2, TimeUnit.SECONDS);
            if (!acquired) {
                failAudit(actor, config, req, "RATE_LIMITED", 0);
                throw new IpdBusinessException(ApiV1ErrorCode.RATE_LIMITED);
            }
            Integer maxTokens = cfg.path("maxTokens").asInt(0) > 0 ? cfg.path("maxTokens").asInt(0) : null;
            BigDecimal temperature = cfg.hasNonNull("temperature") ? cfg.get("temperature").decimalValue() : null;
            AiChatClient.AiChatResult result = chatClient.chat(new AiTestConfig(
                config.getProvider(), config.getEndpointUrl(),
                modelConfigService.decryptApiKey(config), config.getModelName(), timeoutMs),
                req.prompt(), maxTokens, temperature);
            if (!result.success()) {
                failAudit(actor, config, req, result.errorCode(), result.latencyMs());
                throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR,
                    "AI 生成失败: " + safeErr(result.errorCode()));
            }
            if (result.content() == null || result.content().isBlank()) {
                failAudit(actor, config, req, "EMPTY_RESPONSE", result.latencyMs());
                throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR, "AI 生成失败: EMPTY_RESPONSE");
            }
            AiDocument doc = documentService.createGenerated(req.projectId(), req.docType(), req.title(),
                result.content(), config.getModelName(), result.promptTokens(), result.completionTokens(),
                actor.id());
            auditLogService.append(AuditLog.builder()
                .operatorId(actor.id()).operatorName(actor.name()).operatorRole(actor.role())
                .action("AI_GENERATE").entityType("AI_DOCUMENT").entityId(doc.getId())
                .afterData(AuditEventData.json(
                    "projectId", req.projectId(),
                    "docType", doc.getDocType() == null ? "" : doc.getDocType(),
                    "model", config.getModelName(),
                    "promptLen", req.prompt().length(),
                    "tokenPrompt", result.promptTokens(),
                    "tokenCompletion", result.completionTokens(),
                    "latencyMs", result.latencyMs(),
                    "status", doc.getStatus()))
                .build());
            return doc;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failAudit(actor, config, req, "RATE_LIMITED", 0);
            throw new IpdBusinessException(ApiV1ErrorCode.RATE_LIMITED);
        } finally {
            if (acquired) {
                gate.release();
            }
        }
    }

    private void validateActorAndReq(IpdActor actor, AiGenerateReq req) {
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED);
        }
        requireArg(req != null && req.projectId() != null, "projectId 必填");
        requireArg(req.title() != null && !req.title().isBlank(), "title 必填");
        requireArg(req.title().length() <= 200, "title 超长（≤200）");
        requireArg(req.prompt() != null && !req.prompt().isBlank(), "prompt 必填（PM 原始资料）");
        requireArg(req.prompt().length() <= MAX_PROMPT_LEN, "prompt 超长（≤30000 字符）");
        if (req.docType() != null && req.docType().length() > 32) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    /** 本月已用 token（v1 AI 原始输出才有 token 计量；人工改版 model/token 为 NULL 不计入）。 */
    private long usedTokensThisMonth() {
        Date monthStart = Date.from(java.time.YearMonth.now(clock).atDay(1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Long used = documentMapper.sumTokensSince(monthStart);
        return used == null ? 0L : used;
    }

    private static long budgetOf(JsonNode cfg) {
        return cfg.path(EXT_BUDGET).asLong(0);
    }

    private static int clampTimeout(int configured) {
        if (configured <= 0) {
            return DEFAULT_TIMEOUT_MS;
        }
        return Math.min(Math.max(configured, MIN_TIMEOUT_MS), MAX_TIMEOUT_MS);
    }

    private static String hostOf(String endpoint) {
        try {
            return new java.net.URL(endpoint).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    /** errorCode 已是白名单类别（Tester 风格），此处仅兜空值。 */
    private static String safeErr(String errorCode) {
        return errorCode == null || errorCode.isBlank() ? "UNKNOWN" : errorCode;
    }

    private static JsonNode parseConfigJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
        } catch (Exception e) {
            return MissingNode.getInstance();
        }
    }

    private void failAudit(IpdActor actor, AiModelConfig config, AiGenerateReq req, String code, long latencyMs) {
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id()).operatorName(actor.name()).operatorRole(actor.role())
            .action("AI_GENERATE_FAILED").entityType("AI_DOCUMENT")
            .afterData(AuditEventData.json(
                "projectId", req.projectId(),
                "model", config.getModelName(),
                "promptLen", req.prompt().length(),
                "errorCode", code,
                "latencyMs", latencyMs))
            .build());
    }

    private static void requireArg(boolean ok, String message) {
        if (!ok) {
            throw new IpdBusinessException(message);
        }
    }
}
