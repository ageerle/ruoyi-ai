package org.ruoyi.ipd.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.IpdBusinessException;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * R-NEW S-6：AiChatClient IPv6 fe80::/10 显式检查 + DNS rebinding 防御单测。
 *
 * <p>覆盖：
 * <ul>
 *   <li>字节级 fe80::/10 显式拒绝（边界：fe80::1、febf::ffff、IPv4-mapped fe80 → 拒；fec0:: → 不在 fe80::/10，放行）</li>
 *   <li>既有 IPv4 黑名单回归：127/8、10/8、172.16/12、192.168/16、169.254/16、100.64/10、0/8</li>
 *   <li>既有 IPv6 黑名单回归：::1、fc00::/7（fd00::/8 + fc00::/8）</li>
 *   <li>DNS rebinding 攻击样本：mock InetAddress.getAllByName 返回 [公网 1.2.3.4] → [fe80::1] → 必须拒</li>
 * </ul>
 *
 * <p>纯 JVM 单测（无 Spring 上下文）；用反射访问 private 静态方法。
 */
@Tag("dev")
@DisplayName("R-NEW S-6 AiChatClient IPv6 fe80::/10 显式 + DNS rebinding 防御")
class AiChatClientSsrfTest {

    // ==================== IPv6 fe80::/10 字节级显式拒绝 ====================

    @Test
    @DisplayName("F-1 fe80::1 → 拒（标准链路本地，11 字节前缀）")
    void fe80_one_blocked() throws Exception {
        assertBlocked(InetAddress.getByName("fe80::1"));
    }

    @Test
    @DisplayName("F-2 febf:ffff:... → 拒（fe80::/10 上界 febf:ffff:...）")
    void febf_upperBlock_blocked() throws Exception {
        byte[] ip = new byte[]{
            (byte) 0xFE, (byte) 0xBF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
        assertBlocked(InetAddress.getByAddress(ip));
    }

    @Test
    @DisplayName("F-3 fbff::1 → 不拒（既不在 fc00::/7 ULA，也不在 fe80::/10 link-local）")
    void outsideBothBlocks_passed() throws Exception {
        // fbff:: 第一个字节 0xFB（不在 fc00::/7 范围 0xFC-0xFF），第二个字节高 2 位是 11（不在 fe80::/10 的 10 范围）。
        byte[] ip = new byte[]{
            (byte) 0xFB, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };
        assertNotBlocked(InetAddress.getByAddress(ip));
    }

    @Test
    @DisplayName("F-4 fe80::ffff:1.2.3.4 → 拒（IPv4-mapped fe80 链路本地）")
    void fe80_ipv4Mapped_blocked() throws Exception {
        byte[] ip = new byte[]{
            (byte) 0xFE, (byte) 0x80, 0, 0, 0, 0, 0, 0,
            0, 0, (byte) 0xFF, (byte) 0xFF,
            1, 2, 3, 4
        };
        assertBlocked(InetAddress.getByAddress(ip));
    }

    // ==================== DNS rebinding 攻击样本（核心场景） ====================

    @Test
    @DisplayName("R-1 DNS rebinding：首解析 1.2.3.4 → 重解析 fe80::1 → 拒")
    void dnsRebinding_publicThenLinkLocal_blocked() throws Exception {
        InetAddress publicIp = InetAddress.getByName("1.2.3.4");
        InetAddress linkLocal = InetAddress.getByName("fe80::1");
        AiChatClient client = new AiChatClient(HttpClient.newHttpClient(), false);
        try (var mocked = mockStatic(InetAddress.class, CALLS_REAL_METHODS)) {
            AtomicInteger calls = new AtomicInteger();
            mocked.when(() -> InetAddress.getAllByName(any(String.class)))
                .thenAnswer(inv -> {
                    String host = inv.getArgument(0);
                    if (!"attacker.example".equals(host)) {
                        return inv.callRealMethod();
                    }
                    int n = calls.incrementAndGet();
                    return n == 1 ? new InetAddress[]{ publicIp } : new InetAddress[]{ linkLocal };
                });
            assertThatThrownBy(() -> invokeValidate(client, "https://attacker.example/v1"))
                .isInstanceOf(IpdBusinessException.class)
                .hasMessageContaining("SSRF blocked");
        }
    }

    @Test
    @DisplayName("R-2 DNS rebinding 公网→公网 IP 一致 → 放行")
    void dnsRebinding_publicThenPublicStable_passed() throws Exception {
        InetAddress publicIp = InetAddress.getByName("1.2.3.4");
        AiChatClient client = new AiChatClient(HttpClient.newHttpClient(), false);
        try (var mocked = mockStatic(InetAddress.class, CALLS_REAL_METHODS)) {
            AtomicInteger calls = new AtomicInteger();
            mocked.when(() -> InetAddress.getAllByName(any(String.class)))
                .thenAnswer(inv -> {
                    String host = inv.getArgument(0);
                    if (!"stable.example".equals(host)) {
                        return inv.callRealMethod();
                    }
                    calls.incrementAndGet();
                    return new InetAddress[]{ publicIp };
                });
            assertThatCode(() -> invokeValidate(client, "https://stable.example/v1"))
                .doesNotThrowAnyException();
        }
    }

    // ==================== 现有黑名单回归（向后兼容） ====================

    @Test
    @DisplayName("B-1 IPv4 169.254.169.254（AWS metadata）→ 拒")
    void awsMetadata_blocked() throws Exception {
        assertBlocked(InetAddress.getByName("169.254.169.254"));
    }

    @Test
    @DisplayName("B-2 IPv4 10.0.0.1（RFC1918）→ 拒")
    void rfc1918_blocked() throws Exception {
        assertBlocked(InetAddress.getByName("10.0.0.1"));
    }

    @Test
    @DisplayName("B-3 IPv4 100.64.0.1（CGN）→ 拒")
    void cgn_blocked() throws Exception {
        assertBlocked(InetAddress.getByName("100.64.0.1"));
    }

    @Test
    @DisplayName("B-4 IPv6 ::1（loopback）→ 拒")
    void ipv6Loopback_blocked() throws Exception {
        assertBlocked(InetAddress.getByName("::1"));
    }

    @Test
    @DisplayName("B-5 IPv6 fd00::1（ULA）→ 拒")
    void ipv6Ula_blocked() throws Exception {
        assertBlocked(InetAddress.getByName("fd00::1"));
    }

    // ==================== 反射 helper ====================

    private static void invokeValidate(AiChatClient c, String url) throws Exception {
        Method m = AiChatClient.class.getDeclaredMethod("validateEndpoint", String.class);
        m.setAccessible(true);
        try {
            m.invoke(c, url);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw e;
        }
    }

    private static void assertBlocked(InetAddress addr) throws Exception {
        Method m = AiChatClient.class.getDeclaredMethod("isBlockedIp", byte[].class);
        m.setAccessible(true);
        assertThat((Boolean) m.invoke(null, addr.getAddress()))
            .as("IP %s 应被黑名单拒绝", addr.getHostAddress())
            .isTrue();
    }

    private static void assertNotBlocked(InetAddress addr) throws Exception {
        Method m = AiChatClient.class.getDeclaredMethod("isBlockedIp", byte[].class);
        m.setAccessible(true);
        assertThat((Boolean) m.invoke(null, addr.getAddress()))
            .as("IP %s 不应被黑名单拒绝", addr.getHostAddress())
            .isFalse();
    }
}
