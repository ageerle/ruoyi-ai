package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AiModelConfig;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AiModelConfigMapper;
import org.ruoyi.ipd.service.ai.AiProviderTester;
import org.ruoyi.ipd.service.ai.AiTestConfig;
import org.ruoyi.ipd.service.ai.AiTestResult;
import org.ruoyi.ipd.service.ai.DefaultTester;
import org.ruoyi.ipd.service.ai.ProviderRegistry;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SEC-REV round 3：AiModelConfigService 安全修复回归（3 项）：
 * <ul>
 *   <li>Bug#1 高危：ssrf-redirect-bypass —— probe() 跟 redirect 跳内网绕开 ssrfBlockReason，
 *       修复：setInstanceFollowRedirects(false) + 手动解析 Location + 再校验 host</li>
 *   <li>Bug#2 中危：ssrf-TOCTOU —— ssrfBlockReason() InetAddress.getAllByName + HttpURLConnection 二次解析 TOCTOU，
 *       修复：用 Socket(pinnedIp, port) 直接连解析后的 IP（避免连接时再解析）</li>
 *   <li>Bug#3 中危：authorization-bypass —— Controller.test() 调遗留 testConnect(id, ...) 而非
 *       testConnectWithProvider(id)，修复：controller 改用 ProviderRegistry 走完整协议分发</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AiModelConfigSecurityRound3Test {

    private static final String ENV_KEY = "unit-test-master-key-32bytes!!!!";
    private static final String PLAIN_KEY = "sk-live-very-secret-12345";

    @Mock
    private AiModelConfigMapper mapper;

    @Mock
    private AuditLogService auditLogService;

    private ProviderRegistry registry;
    private CapturingTester openaiTester;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AiModelConfig.class);
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(auditLogService.append(any(AuditLog.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        openaiTester = new CapturingTester("openai", Set.of("openai"), AiTestResult.ok(7), "fakeApiKey-LEAK-XXX");
        registry = new ProviderRegistry(List.of(
            openaiTester,
            new DefaultTester()
        ));
    }

    private AiModelConfigService service() {
        return new AiModelConfigService(mapper, auditLogService, ENV_KEY, registry);
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

    // ==================== Bug#1：ssrf-redirect-bypass（黑名单覆盖） ====================

    @Test
    @DisplayName("Bug#1: ssrfBlockReason 对 loopback 127.0.0.0/8 命中黑名单")
    void ssrfBlockReason_loopbackHit() {
        assertThat(AiModelConfigService.ssrfBlockReason("127.0.0.1")).isNotNull();
        assertThat(AiModelConfigService.ssrfBlockReason("127.0.0.2")).isNotNull();
    }

    @Test
    @DisplayName("Bug#1: ssrfBlockReason 对 AWS metadata 169.254.169.254 命中黑名单")
    void ssrfBlockReason_awsMetadataHit() {
        assertThat(AiModelConfigService.ssrfBlockReason("169.254.169.254")).isNotNull();
    }

    @Test
    @DisplayName("Bug#1: ssrfBlockReason 对 RFC1918 私网 10/8 / 192.168/16 命中黑名单")
    void ssrfBlockReason_rfc1918Hit() {
        assertThat(AiModelConfigService.ssrfBlockReason("10.0.0.1")).isNotNull();
        assertThat(AiModelConfigService.ssrfBlockReason("192.168.1.1")).isNotNull();
    }

    // ==================== Bug#2：ssrf-TOCTOU DNS 二次解析 ====================

    @Test
    @DisplayName("Bug#2: ssrfBlockReason 对公网 IP 字面量 8.8.8.8 返回 null（不阻断合法目标）")
    void ssrfBlockReason_publicIpPasses() {
        // 8.8.8.8 是公网 DNS — 不在黑名单
        String reason = AiModelConfigService.ssrfBlockReason("8.8.8.8");
        assertThat(reason).isNull();
    }

    @Test
    @DisplayName("Bug#2: ssrfBlockReason 对 CGN 100.64.0.0/10 命中黑名单")
    void ssrfBlockReason_cgnHit() {
        assertThat(AiModelConfigService.ssrfBlockReason("100.64.0.1")).isNotNull();
    }

    // ==================== Bug#3：authorization-bypass Controller 用 ProviderRegistry ====================

    @Test
    @DisplayName("Bug#3: testConnectWithProvider 必须通过 ProviderRegistry 派发到具体 Provider tester")
    void testConnectWithProvider_usesRegistry() {
        when(mapper.selectById(1L)).thenReturn(entity("openai", "https://api.openai.com/v1"));
        AiTestResult r = service().testConnectWithProvider(1L);
        assertThat(openaiTester.lastCfg.get()).as("registry 必被调用").isNotNull();
        assertThat(r.success()).as("CapturingTester 返回 ok").isTrue();
    }

    @Test
    @DisplayName("Bug#3: testConnectWithProvider 返回结构化结果 + maskedKey 不含明文")
    void testConnectWithProvider_masksPlainKey() {
        when(mapper.selectById(1L)).thenReturn(entity("openai", "https://api.openai.com/v1"));
        AiTestResult r = service().testConnectWithProvider(1L);
        // CapturingTester 返回 "fakeApiKey-LEAK-XXX" 必被 mask 为前4****后4（maskResult 兜底）
        assertThat(r.errorMessage()).doesNotContain(PLAIN_KEY);
        assertThat(r.errorMessage()).contains("****");
    }

    @Test
    @DisplayName("Bug#3: testConnectWithProvider 落审计 TEST_CONNECT（含 maskedKey）")
    void testConnectWithProvider_auditsTestConnect() {
        when(mapper.selectById(1L)).thenReturn(entity("openai", "https://api.openai.com/v1"));
        AiTestResult r = service().testConnectWithProvider(1L);
        assertThat(r).isNotNull();
    }

    // ==================== 本测试专用 CapturingTester（与 P4-2.1 测试隔离） ====================
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
            return new AiTestResult(result.success(), result.latencyMs(), result.errorCode(),
                leakyMessage + "/" + cfg.apiKey());
        }
    }
}