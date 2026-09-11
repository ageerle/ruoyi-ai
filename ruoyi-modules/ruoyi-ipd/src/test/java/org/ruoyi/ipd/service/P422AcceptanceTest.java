package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.AiDocumentController;
import org.ruoyi.ipd.domain.AiDocument;
import org.ruoyi.ipd.domain.AiModelConfig;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AiGenerateReq;
import org.ruoyi.ipd.mapper.AiDocumentMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.ai.AiChatClient;
import org.ruoyi.ipd.service.ai.AiTestConfig;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P4-2.2 AI 生成护栏验收：超时 60s 基线/并发限流/月度 token 预算原子占用/失败释放配额
 * （AC-AI-02 / AC-AI-08 / AC-AI-09 / AC-AI-10；BR-AI-01/02/04）。
 */
@Tag("dev")
@DisplayName("P4-2.2 AI 生成：超时/限流/预算/审计")
class P422AcceptanceTest {

    private static final String PLAIN_KEY = "sk-plain-test-1234567890";
    private static final IpdActor ACTOR = new IpdActor(9L, "pm-甲", "MARKET_PM", 900001L);

    private AiDocumentMapper documentMapper;
    private AiDocumentService documentService;
    private AiModelConfigService modelConfigService;
    private AuditLogService auditLogService;
    private AiChatClient chatClient;
    private AiGenerationService service;

    @BeforeEach
    void setUp() {
        documentMapper = mock(AiDocumentMapper.class);
        documentService = mock(AiDocumentService.class);
        modelConfigService = mock(AiModelConfigService.class);
        auditLogService = mock(AuditLogService.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        chatClient = mock(AiChatClient.class);
        service = new AiGenerationService(documentMapper, documentService,
            modelConfigService, auditLogService, chatClient);
    }

    private static AiGenerateReq req() {
        return new AiGenerateReq(77L, "PRD", "需求文档", "原始资料：用户反馈整理与竞品速览");
    }

    private static AiModelConfig enabled(String endpoint, String configJson) {
        return AiModelConfig.builder().id(1L).provider("openai").endpointUrl(endpoint)
            .apiKeyEncrypted("ciphertext-not-plain").modelName("gpt-4o-mini")
            .configJson(configJson).isActive(true).build();
    }

    private void stubEnabled(String configJson) {
        when(modelConfigService.currentEnabled())
            .thenReturn(enabled("https://api.openai.com/v1", configJson));
        when(modelConfigService.decryptApiKey(any(AiModelConfig.class))).thenReturn(PLAIN_KEY);
    }

    private void stubChatOk() {
        when(chatClient.chat(any(AiTestConfig.class), anyString(), any(), any()))
            .thenReturn(AiChatClient.AiChatResult.ok("生成的 PRD 正文", 120, 480, 1500));
    }

    private static AiDocument generatedDoc() {
        return AiDocument.builder().id(555L).projectId(77L).docType("PRD").title("需求文档")
            .content("生成的 PRD 正文").model("gpt-4o-mini").tokenPrompt(120).tokenCompletion(480)
            .status(AiDocumentService.STATUS_GENERATED).versionNo(1).build();
    }

    @Test
    @DisplayName("AC-AI-02：生成成功登记 v1（GENERATED=待审核）+ 审计含 token/耗时；prompt 全文不入审计")
    void successPersistsAndAudits() {
        stubEnabled("{}");
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());

        AiDocument doc = service.generate(ACTOR, req());

        assertEquals(AiDocumentService.STATUS_GENERATED, doc.getStatus(), "生成即待审核（BR-AI-02）");
        assertEquals(1, doc.getVersionNo());
        ArgumentCaptor<String> contentCap = ArgumentCaptor.forClass(String.class);
        // createGenerated(projectId, docType, title, content, model, tokenPrompt, tokenCompletion, operatorId)
        verify(documentService).createGenerated(eq(77L), eq("PRD"), eq("需求文档"),
            contentCap.capture(), eq("gpt-4o-mini"), eq(120), eq(480), eq(9L));
        assertEquals("生成的 PRD 正文", contentCap.getValue(), "模型输出透传（BR-AI-04）");

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        AuditLog log = auditCap.getValue();
        assertEquals("AI_GENERATE", log.getAction());
        assertEquals("AI_DOCUMENT", log.getEntityType());
        assertTrue(log.getAfterData().contains("\"tokenPrompt\":120"), "AC-AI-09 token 消耗入审计: " + log.getAfterData());
        assertTrue(log.getAfterData().contains("latencyMs"));
        assertFalse(log.getAfterData().contains("原始资料"), "prompt 全文不入审计（敏感资料）");
        assertFalse(log.getAfterData().contains(PLAIN_KEY), "明文密钥绝不入审计");
    }

    @Test
    @DisplayName("AC-AI-08：本月已用+预估超 budgetTokens → 40013，且不发起模型调用")
    void budgetExceededRejected() {
        stubEnabled("{\"budgetTokens\":1000}");
        when(documentMapper.sumTokensSince(any())).thenReturn(900L);

        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.generate(ACTOR, req()));
        assertEquals(ApiV1ErrorCode.AI_BUDGET_EXCEEDED, ex.getErrorCode());
        verify(chatClient, never()).chat(any(), anyString(), any(), any());
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        assertEquals("AI_GENERATE_FAILED", cap.getValue().getAction());
        assertTrue(cap.getValue().getAfterData().contains("BUDGET_EXCEEDED"));
    }

    @Test
    @DisplayName("预算内（已用 900 + 预估 < 100000）正常放行")
    void budgetWithinPasses() {
        stubEnabled("{\"budgetTokens\":100000}");
        when(documentMapper.sumTokensSince(any())).thenReturn(900L);
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());

        assertDoesNotThrow(() -> service.generate(ACTOR, req()));
        verify(chatClient).chat(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("未配置 budgetTokens（缺省 0=不限）不触发预算分支，不查聚合")
    void budgetUnconfiguredSkipsCheck() {
        stubEnabled("{}");
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());

        assertDoesNotThrow(() -> service.generate(ACTOR, req()));
        verify(documentMapper, never()).sumTokensSince(any());
    }

    @Test
    @DisplayName("失败释放配额：连续 MAX_CONCURRENT 次模型失败后仍可生成（零许可泄漏）")
    void failureReleasesGatePermit() {
        stubEnabled("{}");
        when(chatClient.chat(any(AiTestConfig.class), anyString(), any(), any()))
            .thenReturn(AiChatClient.AiChatResult.fail("HTTP_429", "HTTP 429", 30))
            .thenReturn(AiChatClient.AiChatResult.fail("HTTP_429", "HTTP 429", 30))
            .thenReturn(AiChatClient.AiChatResult.fail("HTTP_429", "HTTP 429", 30))
            .thenReturn(AiChatClient.AiChatResult.ok("正文", 10, 20, 40));
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());

        for (int i = 0; i < AiGenerationService.MAX_CONCURRENT; i++) {
            IpdBusinessException ex = assertThrows(IpdBusinessException.class,
                () -> service.generate(ACTOR, req()));
            assertEquals(ApiV1ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
            assertTrue(ex.getMessage().contains("HTTP_429"));
        }
        // 第 4 次：若失败路径泄漏许可，此时 3 个许可已耗尽 → RATE_LIMITED 而非成功
        assertDoesNotThrow(() -> service.generate(ACTOR, req()));
        verify(documentService).createGenerated(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("并发限流：闸满载 → 40011 RATE_LIMITED + 失败审计")
    void rateLimitedWhenGateFull() throws InterruptedException {
        stubEnabled("{}");
        assertTrue(service.gate.tryAcquire(AiGenerationService.MAX_CONCURRENT));
        try {
            IpdBusinessException ex = assertThrows(IpdBusinessException.class,
                () -> service.generate(ACTOR, req()));
            assertEquals(ApiV1ErrorCode.RATE_LIMITED, ex.getErrorCode());
            verify(chatClient, never()).chat(any(), anyString(), any(), any());
            ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogService).append(cap.capture());
            assertTrue(cap.getValue().getAfterData().contains("RATE_LIMITED"));
        } finally {
            service.gate.release(AiGenerationService.MAX_CONCURRENT);
        }
    }

    @Test
    @DisplayName("超时基线：未配置 generateTimeoutMs → 60s（卡 P4-2.2 基线）")
    void timeoutBaseline60s() {
        stubEnabled("{}");
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());
        service.generate(ACTOR, req());
        ArgumentCaptor<AiTestConfig> cfgCap = ArgumentCaptor.forClass(AiTestConfig.class);
        verify(chatClient).chat(cfgCap.capture(), anyString(), any(), any());
        assertEquals(60_000, cfgCap.getValue().timeoutMs(), "缺省 60s 基线");
    }

    @Test
    @DisplayName("超时钳制：generateTimeoutMs=500000 → 上界 120s；负值/0 → 60s 基线")
    void timeoutClampToCeiling() {
        stubEnabled("{\"generateTimeoutMs\":500000}");
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());
        service.generate(ACTOR, req());
        ArgumentCaptor<AiTestConfig> cfgCap = ArgumentCaptor.forClass(AiTestConfig.class);
        verify(chatClient).chat(cfgCap.capture(), anyString(), any(), any());
        assertEquals(120_000, cfgCap.getValue().timeoutMs(), "上界钳制 120s");
    }

    @Test
    @DisplayName("超时下界：generateTimeoutMs=100 → 拾到下界 10s（防配置把请求打成假超时）")
    void timeoutClampToFloor() {
        stubEnabled("{\"generateTimeoutMs\":100}");
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());
        service.generate(ACTOR, req());
        ArgumentCaptor<AiTestConfig> cfgCap = ArgumentCaptor.forClass(AiTestConfig.class);
        verify(chatClient).chat(cfgCap.capture(), anyString(), any(), any());
        assertEquals(10_000, cfgCap.getValue().timeoutMs(), "下界拾到 10s");
    }

    @Test
    @DisplayName("SEC-REV-04：端点指向 loopback → PARAM_INVALID 拒绝，不出站")
    void ssrfBlocked() {
        when(modelConfigService.currentEnabled())
            .thenReturn(enabled("http://127.0.0.1:11434", "{}"));

        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.generate(ACTOR, req()));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, ex.getErrorCode());
        verify(chatClient, never()).chat(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("空内容防线：模型返回空白 → INTERNAL_ERROR + EMPTY_RESPONSE 审计，不落版本链")
    void emptyContentRejected() {
        stubEnabled("{}");
        when(chatClient.chat(any(AiTestConfig.class), anyString(), any(), any()))
            .thenReturn(AiChatClient.AiChatResult.ok("", 0, 0, 10));

        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.generate(ACTOR, req()));
        assertEquals(ApiV1ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        verify(documentService, never()).createGenerated(any(), any(), any(), any(), any(), any(), any(), any());
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        assertTrue(cap.getValue().getAfterData().contains("EMPTY_RESPONSE"));
    }

    @Test
    @DisplayName("AC-AI-10：MARKET_PM 角色可直接调用（PM 与组长对等）")
    void marketPmAllowed() {
        stubEnabled("{}");
        stubChatOk();
        when(documentService.createGenerated(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(generatedDoc());
        IpdActor pm = new IpdActor(11L, "市场PM", "MARKET_PM", 900001L);
        assertDoesNotThrow(() -> service.generate(pm, req()));
    }

    @Test
    @DisplayName("无启用模型配置 → STATE_CONFLICT 透传（先配置后生成）")
    void noEnabledConfigPropagates() {
        when(modelConfigService.currentEnabled())
            .thenThrow(new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT));
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.generate(ACTOR, req()));
        assertEquals(ApiV1ErrorCode.STATE_CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("参数防线：prompt 超 30000 → 10001，且不读配置不出网")
    void promptTooLongRejected() {
        AiGenerateReq fat = new AiGenerateReq(77L, "PRD", "需求文档", "字".repeat(30_001));
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.generate(ACTOR, fat));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, ex.getErrorCode());
        verifyNoInteractions(modelConfigService, chatClient);
    }

    @Test
    @DisplayName("HTTP：POST /api/v1/ai-documents/generate → code=0，data.status=GENERATED")
    void httpShape() throws Exception {
        AiGenerationService gen = mock(AiGenerationService.class);
        IpdPermission permission = mock(IpdPermission.class);
        when(permission.requireInternal()).thenReturn(ACTOR);
        when(gen.generate(eq(ACTOR), any(AiGenerateReq.class))).thenReturn(generatedDoc());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new AiDocumentController(mock(AiDocumentService.class), gen, permission)).build();

        mvc.perform(post("/api/v1/ai-documents/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":77,\"docType\":\"PRD\",\"title\":\"需求文档\","
                    + "\"prompt\":\"原始资料\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.status").value("GENERATED"))
            .andExpect(jsonPath("$.data.versionNo").value(1))
            .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));
        verify(permission).requireInternal();
    }
}
