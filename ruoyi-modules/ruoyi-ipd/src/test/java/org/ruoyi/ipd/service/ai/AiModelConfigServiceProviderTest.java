package org.ruoyi.ipd.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.domain.AiModelConfig;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AiModelView;
import org.ruoyi.ipd.mapper.AiModelConfigMapper;
import org.ruoyi.ipd.service.AiModelConfigService;
import org.ruoyi.ipd.service.AuditLogService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P4-2.1 Service → ProviderRegistry 派发 + apiKey 脱敏保证（@Tag dev）。
 * <p>
 * 锁定契约：service.testConnect 必须走 registry，response maskedKey 不含明文 apiKey，
 * 且 audit 仍按现有规范落 "TEST_CONNECT" 动作。
 */
@Tag("dev")
@DisplayName("P4-2.1 AiModelConfigService 走 ProviderRegistry")
class AiModelConfigServiceProviderTest {

    private static final String ENV_KEY = "unit-test-master-key-32bytes!!!!";
    private static final String PLAIN_KEY = "sk-live-very-secret-12345";

    private AiModelConfigMapper mapper;
    private AuditLogService audit;
    private ProviderRegistry registry;

    @BeforeEach
    void setUp() {
        mapper = mock(AiModelConfigMapper.class);
        audit = mock(AuditLogService.class);
        when(audit.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        registry = new ProviderRegistry(List.of(
            new CapturingTester("openai", Set.of("openai"), AiTestResult.ok(7), "fakeApiKey-LEAK-XXX"),
            new DefaultTester()
        ));
    }

    private AiModelConfigService service() {
        return new AiModelConfigService(mapper, audit, ENV_KEY, registry);
    }

    private AiModelConfig entity(String provider, String endpoint) {
        return AiModelConfig.builder()
            .id(1L).provider(provider)
            .endpointUrl(endpoint)
            .apiKeyEncrypted(encrypt(PLAIN_KEY))
            .modelName("gpt-x")
            .isActive(true)
            .build();
    }

    private static String encrypt(String s) {
        return org.ruoyi.common.encrypt.utils.EncryptUtils.encryptByAes(s, ENV_KEY);
    }

    @Test
    @DisplayName("Service.testConnectWithProvider 走 registry：openai 分发到 CapturingTester")
    void serviceDispatchesToRegistry() {
        when(mapper.selectById(1L)).thenReturn(entity("openai", "https://api.openai.com/v1"));
        CapturingTester openai = (CapturingTester) registry.find("openai");
        org.ruoyi.ipd.service.ai.AiTestResult r = service().testConnectWithProvider(1L);
        assertNotNull(openai.lastCfg.get(), "registry 必被调用");
        assertEquals(PLAIN_KEY, openai.lastCfg.get().apiKey(), "Tester 收到明文（内存内调用，不落库不外传）");
        assertTrue(r.success(), "CapturingTester 返回 ok: " + r);
    }

    @Test
    @DisplayName("Tester 返回的 errorMessage 含明文 apiKey 片段 → service.testConnectWithProvider 必须 mask 掉")
    void serviceMasksApiKeyLeakingFromTester() {
        when(mapper.selectById(1L)).thenReturn(entity("openai", "https://api.openai.com/v1"));
        org.ruoyi.ipd.service.ai.AiTestResult r = service().testConnectWithProvider(1L);
        assertNotNull(r.errorMessage(), "errorMessage 必有值");
        assertFalse(r.errorMessage().contains(PLAIN_KEY), "result 不得含明文: " + r.errorMessage());
        // 锁定契约：明文 apiKey 被替换为前4****后4
        String masked = "sk-l****2345";
        assertTrue(r.errorMessage().contains(masked),
            "明文 apiKey 应被 mask 替换为 '" + masked + "': " + r.errorMessage());
    }

    @Test
    @DisplayName("未匹配 provider 走 DefaultTester fallback，不抛异常")
    void serviceUnknownProviderFallsBack() {
        when(mapper.selectById(1L)).thenReturn(entity("weird-provider", "https://example.com"));
        // DefaultTester 会去连 https://example.com（可能超时），断言 service 不抛业务异常
        assertDoesNotThrow(() -> service().testConnectWithProvider(1L));
    }

    @Test
    @DisplayName("audit 仍落 TEST_CONNECT，afterData 不含明文")
    void auditKeepsTestConnectShape() {
        when(mapper.selectById(1L)).thenReturn(entity("openai", "https://api.openai.com/v1"));
        service().testConnect(1L, "9");
        org.mockito.ArgumentCaptor<AuditLog> cap = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(audit).append(cap.capture());
        AuditLog log = cap.getValue();
        assertEquals("TEST_CONNECT", log.getAction());
        assertEquals("AI_MODEL_CONFIG", log.getEntityType());
        assertFalse(log.getAfterData().contains(PLAIN_KEY), "审计 afterData 不得含明文: " + log.getAfterData());
    }

    /** 桩 tester：捕获 cfg 且 errorMessage 含明文（用于测试 service mask）。 */
    static final class CapturingTester implements AiProviderTester {
        private final String provider;
        private final Set<String> aliases;
        private final AiTestResult result;
        private final String leakyMessage;
        final AtomicReference<AiTestConfig> lastCfg = new AtomicReference<>();
        CapturingTester(String provider, Set<String> aliases, AiTestResult result, String leakyMessage) {
            this.provider = provider;
            this.aliases = aliases;
            this.result = result;
            this.leakyMessage = leakyMessage;
        }
        @Override public String provider() { return provider; }
        @Override public Set<String> aliases() { return aliases; }
        @Override public AiTestResult test(AiTestConfig cfg) {
            lastCfg.set(cfg);
            // 故意把 apiKey 拼进 errorMessage（模拟不安全 tester）；service 必须 mask
            return new AiTestResult(result.success(), result.latencyMs(), result.errorCode(),
                leakyMessage + "/" + cfg.apiKey());
        }
    }
}
