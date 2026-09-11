package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.AiModelConfigController;
import org.ruoyi.ipd.domain.AiModelConfig;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AiModelSaveReq;
import org.ruoyi.ipd.dto.AiModelView;
import org.ruoyi.ipd.mapper.AiModelConfigMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P4-2.1 AI 模型配置加密密钥与脱敏返回验收（AC-AI-01；密钥 env 方案=用户裁决 2026-09-05）。
 */
@Tag("dev")
@DisplayName("P4-2.1 AI 模型配置加密与脱敏")
class P421AcceptanceTest {

    private static final String PLAIN_KEY = "sk-test-1234567890abcdef";
    private static final String ENV_KEY = "unit-test-master-key-32bytes!!!!";

    private AiModelConfigMapper mapper;
    private AuditLogService audit;
    private AiModelConfigService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AiModelConfig.class);
        mapper = mock(AiModelConfigMapper.class);
        audit = mock(AuditLogService.class);
        when(audit.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new AiModelConfigService(mapper, audit, ENV_KEY);
    }

    private static AiModelSaveReq req(String apiKey) {
        return new AiModelSaveReq("openai", "https://api.openai.com/v1", apiKey,
            "gpt-4o-mini", new BigDecimal("0.70"), 4096);
    }

    private static AiModelConfig stored(Long id, String cipher) {
        return AiModelConfig.builder().id(id).provider("openai")
            .endpointUrl("https://api.openai.com/v1").apiKeyEncrypted(cipher).modelName("gpt-4o-mini")
            .configJson("{\"temperature\":0.70,\"maxTokens\":4096}").isActive(true).build();
    }

    @Test
    @DisplayName("AC-AI-01：全字段可配置；api_key 落库为密文（非明文、base64 形态）")
    void createStoresCipherNotPlaintext() {
        AtomicReference<AiModelConfig> saved = new AtomicReference<>();
        when(mapper.insert(any(AiModelConfig.class))).thenAnswer(inv -> {
            saved.set(inv.getArgument(0, AiModelConfig.class));
            saved.get().setId(1L);
            return 1;
        });
        AiModelView view = service.create(req(PLAIN_KEY), "9");
        assertEquals("openai", view.provider());
        assertEquals("gpt-4o-mini", view.model());
        // AC 语义=数值可配可读；Jackson 树模型会规范化尾零（0.70→0.7），故断数值等价
        assertEquals(0, new BigDecimal("0.70").compareTo(view.temperature()));
        assertEquals(4096, view.maxTokens());
        assertEquals("0", view.enabled());
        String cipher = saved.get().getApiKeyEncrypted();
        assertNotEquals(PLAIN_KEY, cipher, "库内必须是密文");
        assertFalse(cipher.contains("sk-test"), "密文不得含明文片段");
        assertTrue(cipher.length() > PLAIN_KEY.length() / 2, "AES-base64 密文长度合理");
    }

    @Test
    @DisplayName("加解密对称：decryptApiKey(落库实体) == 原明文")
    void roundTripDecrypt() {
        AtomicReference<AiModelConfig> saved = new AtomicReference<>();
        when(mapper.insert(any(AiModelConfig.class))).thenAnswer(inv -> {
            saved.set(inv.getArgument(0, AiModelConfig.class));
            saved.get().setId(1L);
            return 1;
        });
        service.create(req(PLAIN_KEY), "9");
        assertEquals(PLAIN_KEY, service.decryptApiKey(saved.get()));
    }

    @Test
    @DisplayName("脱敏回显：maskedKey=前4****后4（基于密文）；任何输出不含明文")
    void maskedResponseNeverLeaks() {
        String cipher = org.ruoyi.common.encrypt.utils.EncryptUtils.encryptByAes(PLAIN_KEY, ENV_KEY);
        when(mapper.selectById(1L)).thenReturn(stored(1L, cipher));
        AiModelView view = service.get(1L);
        String masked = view.maskedKey();
        assertTrue(masked.startsWith(cipher.substring(0, 4)), "掩码取密文前4");
        assertTrue(masked.contains("****"));
        assertEquals(4 + 4 + 4, masked.length());
        assertFalse(view.toString().contains(PLAIN_KEY), "视图序列化不得含明文");
    }

    @Test
    @DisplayName("update apiKey=null：不触碰密文列（实体 apiKey 为 null，MP 跳过）")
    void updateNullApiKeyKeepsCipher() {
        String cipher = org.ruoyi.common.encrypt.utils.EncryptUtils.encryptByAes(PLAIN_KEY, ENV_KEY);
        when(mapper.selectById(1L)).thenReturn(stored(1L, cipher));
        when(mapper.updateById(any(AiModelConfig.class))).thenReturn(1);
        service.update(1L, req(null), "9");
        ArgumentCaptor<AiModelConfig> cap = ArgumentCaptor.forClass(AiModelConfig.class);
        verify(mapper).updateById(cap.capture());
        assertNull(cap.getValue().getApiKeyEncrypted(), "apiKey=null 不写密文列");
        // 更新后回读仍是原密文（service 返回视图基于回读实体）
        AiModelView view = service.update(1L, req(null), "9");
        assertEquals(cipher.substring(0, 4), view.maskedKey().substring(0, 4));
    }

    @Test
    @DisplayName("update 换钥：新密文替代 + 审计 beforeData 含 keyRotated")
    void updateRotatesKeyWithAudit() {
        String oldCipher = org.ruoyi.common.encrypt.utils.EncryptUtils.encryptByAes(PLAIN_KEY, ENV_KEY);
        AtomicReference<AiModelConfig> patchRef = new AtomicReference<>();
        when(mapper.selectById(1L)).thenAnswer(inv ->
            patchRef.get() == null ? stored(1L, oldCipher) : stored(1L, patchRef.get().getApiKeyEncrypted()));
        when(mapper.updateById(any(AiModelConfig.class))).thenAnswer(inv -> {
            patchRef.set(inv.getArgument(0, AiModelConfig.class));
            return 1;
        });
        service.update(1L, req("sk-new-key-987654321zyx"), "9");
        assertNotEquals(oldCipher, patchRef.get().getApiKeyEncrypted(), "必须换为新密文");
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        // 一次 update 一条审计（beforeData 携带 keyRotated 语义），与 P411/P133 审计惯例一致
        verify(audit, times(1)).append(cap.capture());
        AuditLog rotateAudit = cap.getAllValues().get(0);
        assertEquals("UPDATE", rotateAudit.getAction());
        assertTrue(rotateAudit.getBeforeData().contains("keyRotated"));
        assertFalse(rotateAudit.getAfterData().contains("sk-"), "审计不含明文");
    }

    @Test
    @DisplayName("fail-fast：主密钥（IPD_AIMODEL_ENCRYPT_KEY）缺失 → 拒绝写密钥（不退化明文）")
    void missingEnvKeyFailFast() {
        AiModelConfigService bare = new AiModelConfigService(mapper, audit, "");
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> bare.create(req(PLAIN_KEY), "9"));
        assertEquals(ApiV1ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        verify(mapper, never()).insert(any(AiModelConfig.class));
    }

    @Test
    @DisplayName("参数边界：temperature>2 / ftp 端点 / 短密钥 → 10001")
    void parameterBounds() {
        IpdBusinessException hot = assertThrows(IpdBusinessException.class,
            () -> service.create(new AiModelSaveReq("openai", "https://a.b", "sk-1234567890",
                "m", new BigDecimal("2.50"), 100), "9"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, hot.getErrorCode());
        IpdBusinessException ftp = assertThrows(IpdBusinessException.class,
            () -> service.create(new AiModelSaveReq("openai", "ftp://a.b", "sk-1234567890",
                "m", null, null), "9"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, ftp.getErrorCode());
        IpdBusinessException shortKey = assertThrows(IpdBusinessException.class,
            () -> service.create(new AiModelSaveReq("openai", "https://a.b", "short",
                "m", null, null), "9"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, shortKey.getErrorCode());
        // 边界内合法：temperature 0 与 2
        AtomicReference<AiModelConfig> saved = new AtomicReference<>();
        when(mapper.insert(any(AiModelConfig.class))).thenAnswer(inv -> {
            saved.set(inv.getArgument(0, AiModelConfig.class));
            return 1;
        });
        assertDoesNotThrow(() -> service.create(new AiModelSaveReq("openai", "https://a.b",
            "sk-1234567890", "m", BigDecimal.ZERO, 1), "9"));
    }

    @Test
    @DisplayName("enable 全局唯一：先清其他行再置本行（两次条件 UPDATE）")
    void enableExclusive() {
        when(mapper.selectById(2L)).thenReturn(stored(2L, "cipher-aaaaaaaaaa"));
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        AiModelView view = service.enable(2L, "9");
        assertEquals("1", view.enabled());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<AiModelConfig>> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper, times(2)).update(isNull(), cap.capture());
        String first = cap.getAllValues().get(0).getSqlSegment();
        String second = cap.getAllValues().get(1).getSqlSegment();
        assertTrue(first.contains("is_active"), "第1条清位 SQL 条件含 is_active");
        assertTrue(second.contains("id"), "第2条置位按 id 定位");
    }

    @Test
    @DisplayName("连接失败不泄露凭证：消息只含 host+类别，绝不含 apiKey/密文")
    void connectFailNoLeak() {
        String cipher = org.ruoyi.common.encrypt.utils.EncryptUtils.encryptByAes(PLAIN_KEY, ENV_KEY);
        AiModelConfig config = stored(1L, cipher);
        config.setEndpointUrl("http://127.0.0.1:1");
        when(mapper.selectById(1L)).thenReturn(config);
        AiModelView result = service.testConnect(1L, "9");
        assertTrue(result.maskedKey().contains("fail"), "127.0.0.1:1 必拒连: " + result.maskedKey());
        assertFalse(result.maskedKey().contains(PLAIN_KEY), "不得含明文密钥");
        assertFalse(result.maskedKey().contains(cipher), "不得含密文");
        // SEC-REV 设计：errorCode 白名单输出，host 不进 maskedKey（防 endpoint 信息泄露）
        assertFalse(result.maskedKey().contains("127.0.0.1"), "消息不得含 host（防端点泄露）");
    }

    @Test
    @DisplayName("审计不落明文/密文：afterData 仅 maskedKey/provider/model/enabled")
    void auditNeverStoresKeyMaterial() {
        AtomicReference<AiModelConfig> saved = new AtomicReference<>();
        when(mapper.insert(any(AiModelConfig.class))).thenAnswer(inv -> {
            saved.set(inv.getArgument(0, AiModelConfig.class));
            saved.get().setId(1L);
            return 1;
        });
        service.create(req(PLAIN_KEY), "9");
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).append(cap.capture());
        AuditLog log = cap.getValue();
        assertEquals("AI_MODEL_CONFIG", log.getEntityType());
        assertFalse(log.getAfterData().contains(PLAIN_KEY), "审计不含明文");
        assertFalse(log.getAfterData().contains(saved.get().getApiKeyEncrypted()), "审计不含密文");
        assertTrue(log.getAfterData().contains("maskedKey"));
    }

    @Test
    @DisplayName("HTTP：POST /api/v1/ai-models 脱敏返回；GET 列表 $.code=0")
    void httpShape() throws Exception {
        AiModelConfigService svc = mock(AiModelConfigService.class);
        when(svc.create(any(), anyString())).thenReturn(new AiModelView(1L, "openai",
            "https://api.openai.com/v1", "gpt-4o-mini", new BigDecimal("0.70"), 4096, "0",
            "ciph****tail"));
        when(svc.list()).thenReturn(List.of(new AiModelView(1L, "openai",
            "https://api.openai.com/v1", "gpt-4o-mini", new BigDecimal("0.70"), 4096, "0",
            "ciph****tail")));
        IpdPermission permission = mock(IpdPermission.class);
        IpdActor actor = mock(IpdActor.class);
        when(actor.id()).thenReturn(9L);
        when(permission.requireAdmin()).thenReturn(actor);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiModelConfigController(svc, permission)).build();

        mvc.perform(post("/api/v1/ai-models")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"openai\",\"endpoint\":\"https://api.openai.com/v1\","
                    + "\"apiKey\":\"sk-live-xyz\",\"model\":\"gpt-4o-mini\",\"temperature\":0.7,\"maxTokens\":4096}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.maskedKey").value("ciph****tail"))
            .andExpect(jsonPath("$.data.apiKey").doesNotExist());
        verify(svc).create(any(), eq("9"));

        mvc.perform(get("/api/v1/ai-models"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].provider").value("openai"));
        verify(permission).requireInternal();
    }
}
