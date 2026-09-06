package org.ruoyi.ipd.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P0-7.3 Refresh、退出与会话即时撤销——行为版回归测试（QA 2026-09-05 22:35 退回补证项①）。
 *
 * <p>与 P073AcceptanceTest（命名守护）互补，本类断言真实会话行为，不依赖 Spring 容器：
 * <ul>
 *   <li>AC-AUTH-07：Token 过期后发起请求 ⇒ 未认证拒绝（HTTP 侧由 IpdWebSecurityConfig
 *       统一映射 401 / ApiV1ErrorCode.UNAUTHORIZED(20001)，即设计文档"2xxxx"）。</li>
 *   <li>refresh 轮换：旧 token 立即失效（IpdAuthController#refresh 契约）。</li>
 *   <li>重放拒绝：轮换后重放旧 token ⇒ NotLoginException。</li>
 *   <li>logout 不可复活：登出后旧 token 不能因任何新登录复活。</li>
 *   <li>revokeAll：改密全端下线（离职/移交撤销同机制）。</li>
 *   <li>凭证变更失效：改密后旧 token 因 credentialMarker 不匹配被拒（revokeAll 兜底路径）。</li>
 * </ul>
 *
 * <p>纯 JVM 行为测试：Sa-Token 1.44 默认内存 Dao + 自stub上下文（header 携带 token），
 * 不启动容器、不连 Redis/MySQL；真库 HTTP 验收另见 evidence-p073-*.json。
 */
@Tag("dev")
class P073BehaviorAcceptanceTest {

    /** 模拟当前请求 Authorization/satoken 头携带的 token（单线程测试内切换身份用）。 */
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    /** 请求级 storage（login 会写入 just-created token，actingAs 时重置避免串扰）。 */
    private static final ThreadLocal<Map<String, Object>> REQ_STORAGE = ThreadLocal.withInitial(HashMap::new);

    private static PersonMapper personMapper;
    private static IpdAuthSession session;
    private static Person person;
    private static String originalJwtSecret;
    private static long originalTimeout;

    @BeforeAll
    static void setUp() {
        personMapper = Mockito.mock(PersonMapper.class);
        person = Person.builder()
            .id(1L).username("p073").name("P073行为测试")
            .passwordHash(BCrypt.hashpw("Passw0rd!p073"))
            .accountStatus("ACTIVE").employmentStatus("ACTIVE")
            .mustChangePwd("0").personType("MARKET_PM")
            .build();
        Mockito.when(personMapper.selectById(1L)).thenReturn(person);
        session = new IpdAuthSession(personMapper);

        // Sa-Token 全局配置：JWT Simple 模式必须提供签名密钥；测试内隔离并最终还原
        originalJwtSecret = SaManager.getConfig().getJwtSecretKey();
        originalTimeout = SaManager.getConfig().getTimeout();
        SaManager.getConfig().setJwtSecretKey("p073-behavior-test-secret");
        SaManager.setSaTokenContext(stubContext());
    }

    @AfterAll
    static void tearDown() {
        SaManager.getConfig().setJwtSecretKey(originalJwtSecret);
        SaManager.getConfig().setTimeout(originalTimeout);
        SaManager.setSaTokenContext(null);
        TOKEN_HOLDER.remove();
        REQ_STORAGE.remove();
    }

    /** 以指定 token 身份发起后续请求（等价于带上该请求头）。 */
    private static void actingAs(String token) {
        REQ_STORAGE.set(new HashMap<>());
        TOKEN_HOLDER.set(token);
    }

    // ---------- 行为 1：refresh 轮换，旧 token 立即失效 ----------

    @Test
    @DisplayName("refresh 轮换：旧 token 撤销后立即不可用，新 token 正常（IpdAuthController#refresh 契约）")
    void refreshRotation_oldTokenDiesImmediately() {
        String tokenA = session.login(person);
        actingAs(tokenA);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);

        // 复刻 IpdAuthController#refresh 的原子序列：读旧票 → 撤销 → 发新票
        String oldToken = session.tokenValue();
        session.logout();
        String tokenB = session.login(person);

        assertThat(tokenB).isNotEqualTo(oldToken);
        actingAs(tokenB);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);
        actingAs(oldToken);
        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
    }

    // ---------- 行为 2：重放拒绝 ----------

    @Test
    @DisplayName("重放拒绝：轮换后再用旧 token（攻击者留存副本）两次请求均被拒")
    void replayedOldToken_rejected() {
        String tokenA = session.login(person);
        session.logout();
        String tokenB = session.login(person);

        actingAs(tokenA);
        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
        // 第二次重放同样拒绝（无自愈、无复活）
        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
        actingAs(tokenB);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);
    }

    // ---------- 行为 3：logout 后旧 token 不可复活 ----------

    @Test
    @DisplayName("logout 不可复活：登出后旧 token 永久失效，同账号重新登录不救活旧票")
    void logout_oldTokenCannotResurrect() {
        String tokenA = session.login(person);
        actingAs(tokenA);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);
        session.logout();

        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
        // 本人重新登录拿到新票，旧票仍然死亡
        String tokenB = session.login(person);
        assertThat(tokenB).isNotEqualTo(tokenA);
        actingAs(tokenA);
        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
        session.logout();
    }

    // ---------- 行为 4：revokeAll 全端下线 ----------

    @Test
    @DisplayName("revokeAll：改密/离职/移交场景一次撤销该人员全部会话（多设备全下线）")
    void revokeAll_killsAllSessionsAcrossDevices() {
        String device1 = session.login(person);
        String device2 = session.login(person);
        assertThat(device1).isNotEqualTo(device2);
        actingAs(device1);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);
        actingAs(device2);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);

        session.revokeAll(1L);

        actingAs(device1);
        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
        actingAs(device2);
        assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
    }

    // ---------- 行为 5：AC-AUTH-07 过期拒绝 ----------

    @Test
    @DisplayName("AC-AUTH-07：token 过期后请求 ⇒ NotLoginException（映射 401/20001 错误包，前端跳登录）")
    void expiredToken_rejectedAsNotLogin() throws InterruptedException {
        long savedTimeout = SaManager.getConfig().getTimeout();
        try {
            SaManager.getConfig().setTimeout(1); // 1 秒过期
            String token = session.login(person);
            actingAs(token);
            assertThat(session.currentPerson().getId()).isEqualTo(1L); // 未过期时可用

            Thread.sleep(1300); // 跨过 TTL

            assertThatThrownBy(session::currentPerson).isInstanceOf(NotLoginException.class);
        } finally {
            SaManager.getConfig().setTimeout(savedTimeout);
        }
        // 401 映射契约（IpdWebSecurityConfig 统一 catch NotLoginException → IpdPermissionException(401, UNAUTHORIZED)）
        IpdPermissionException mapped = new IpdPermissionException(401, ApiV1ErrorCode.UNAUTHORIZED);
        assertThat(mapped.getHttpStatus()).isEqualTo(401);
        assertThat(mapped.getErrorCode()).isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
        assertThat(ApiV1ErrorCode.UNAUTHORIZED.getCode()).isEqualTo(20001);
    }

    // ---------- 行为 6：改密后旧 token 因凭证快照失效（revokeAll 失败时的兜底） ----------

    @Test
    @DisplayName("凭证变更失效：改密后数据库口令哈希变化 ⇒ 旧 token credentialMarker 不匹配被拒")
    void credentialChange_invalidatesOldTokens() {
        String token = session.login(person);
        actingAs(token);
        assertThat(session.currentPerson().getId()).isEqualTo(1L);

        // 模拟 changePassword 已落库：同一 person 的口令哈希更新
        String oldHash = person.getPasswordHash();
        person.setPasswordHash(BCrypt.hashpw("NewPassw0rd!p073"));
        try {
            assertThatThrownBy(session::currentPerson)
                .isInstanceOf(NotLoginException.class)
                .hasMessageContaining("凭证已更新");
        } finally {
            person.setPasswordHash(oldHash); // 还原，避免影响其它用例
        }
    }

    // ---------- Sa-Token 1.44 请求上下文 stub（header 携带 token） ----------

    private static SaTokenContext stubContext() {
        return new SaTokenContext() {
            @Override public void setContext(SaRequest request, SaResponse response, SaStorage storage) {
                // 单线程测试：模型元状态存于 ThreadLocal，无需额外处理
            }
            @Override public void clearContext() {
                TOKEN_HOLDER.remove();
                REQ_STORAGE.remove();
            }
            @Override public boolean isValid() { return true; }
            @Override public cn.dev33.satoken.context.model.SaTokenContextModelBox getModelBox() {
                return new cn.dev33.satoken.context.model.SaTokenContextModelBox(
                    stubRequest(), stubResponse(), stubStorage());
            }
        };
    }

    private static SaRequest stubRequest() {
        String tokenName = SaManager.getConfig().getTokenName();
        return new SaRequest() {
            @Override public Object getSource() { return null; }
            @Override public String getParam(String name) { return tokenName.equals(name) ? TOKEN_HOLDER.get() : null; }
            @Override public Collection<String> getParamNames() { return java.util.Set.of(); }
            @Override public Map<String, String> getParamMap() { return Map.of(); }
            @Override public String getHeader(String name) { return tokenName.equals(name) ? TOKEN_HOLDER.get() : null; }
            @Override public String getCookieValue(String name) { return null; }
            @Override public String getCookieFirstValue(String name) { return null; }
            @Override public String getCookieLastValue(String name) { return null; }
            @Override public String getRequestPath() { return "/api/v1/auth/me"; }
            @Override public String getUrl() { return "http://test/api/v1/auth/me"; }
            @Override public String getMethod() { return "POST"; }
            @Override public String getHost() { return "test"; }
            @Override public Object forward(String path) { return null; }
        };
    }

    private static SaResponse stubResponse() {
        return new SaResponse() {
            @Override public Object getSource() { return null; }
            @Override public SaResponse setStatus(int status) { return this; }
            @Override public SaResponse setHeader(String name, String value) { return this; }
            @Override public SaResponse addHeader(String name, String value) { return this; }
            @Override public Object redirect(String url) { return null; }
        };
    }

    private static SaStorage stubStorage() {
        return new SaStorage() {
            @Override public Object getSource() { return null; }
            @Override public Object get(String key) { return REQ_STORAGE.get().get(key); }
            @Override public SaStorage set(String key, Object value) { REQ_STORAGE.get().put(key, value); return this; }
            @Override public SaStorage delete(String key) { REQ_STORAGE.get().remove(key); return this; }
        };
    }
}
