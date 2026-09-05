package org.ruoyi.ipd.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * hash 链纯函数单测（@Tag("dev") 必打：Surefire groups 过滤，无 tag 静默跳过）
 */
@Tag("dev")
class AuditHashChainTest {

    private static final long T0 = 1693800000000L;

    private String canonical(long seq, String action, Long entityId) {
        return AuditHashChain.canonical(seq, 9L, "张三", "MARKET_PM", action, "projects", entityId,
                null, "{\"k\":1}", "测试原因", T0);
    }

    @Test
    @DisplayName("链首：GENESIS 为 64 个 0")
    void genesisIs64Zeros() {
        assertThat(AuditHashChain.GENESIS).hasSize(64).matches("^0+$");
    }

    @Test
    @DisplayName("同内容同 hash（确定性），任一字段变动则 hash 变化（防篡改）")
    void deterministicAndTamperEvident() {
        String c1 = canonical(1, "CREATE", 100L);
        String h1 = AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, c1);
        String h1again = AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, c1);
        assertThat(h1).isEqualTo(h1again).hasSize(64);

        String hTampered = AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, canonical(1, "CREATE", 101L));
        assertThat(hTampered).isNotEqualTo(h1);

        String hNewPrev = AuditHashChain.computeCurrHash(h1, c1);
        assertThat(hNewPrev).isNotEqualTo(h1);
    }

    @Test
    @DisplayName("null 字段规范化为空串，不抛异常且跨次稳定")
    void nullFieldsCanonicalStable() {
        String a = AuditHashChain.canonical(2L, null, null, null, "LOGIN", "persons", null, null, null, null, T0);
        String b = AuditHashChain.canonical(2L, null, null, null, "LOGIN", "persons", null, null, null, null, T0);
        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("三节点链式推演：逐环验证通过、中间篡改即断")
    void chainWalk() {
        String h0 = AuditHashChain.GENESIS;
        String c1 = canonical(1, "CREATE", 1L);
        String h1 = AuditHashChain.computeCurrHash(h0, c1);
        String c2 = AuditHashChain.canonical(2L, 9L, "张三", "MARKET_PM", "UPDATE", "projects", 1L,
            "{}", "{\"k\":2}", null, T0 + 1);
        String h2 = AuditHashChain.computeCurrHash(h1, c2);
        String c3 = AuditHashChain.canonical(3L, 9L, "张三", "MARKET_PM", "APPROVE", "gates", 5L,
            null, null, "同意", T0 + 2);
        String h3 = AuditHashChain.computeCurrHash(h2, c3);

        // 完整链逐环重算 = 一致
        assertThat(AuditHashChain.computeCurrHash(h0, c1)).isEqualTo(h1);
        assertThat(AuditHashChain.computeCurrHash(h1, c2)).isEqualTo(h2);
        assertThat(AuditHashChain.computeCurrHash(h2, c3)).isEqualTo(h3);

        // 篡改第 2 环内容 → 第 2、3 环验证全断（雪崩式暴露）
        String fakeC2 = AuditHashChain.canonical(2L, 9L, "张三", "MARKET_PM", "DELETE", "projects", 1L,
            "{}", "{\"k\":2}", null, T0 + 1);
        assertThat(AuditHashChain.computeCurrHash(h1, fakeC2)).isNotEqualTo(h2);
    }

    @Test
    @DisplayName("RISK-04 A：canonical 与 canonicalV1 字节完全一致（历史链零破坏）")
    void canonicalEqualsCanonicalV1() {
        String viaDefault = AuditHashChain.canonical(1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 100L,
            null, "{\"k\":1}", "测试原因", T0);
        String viaV1 = AuditHashChain.canonicalV1(1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 100L,
            null, "{\"k\":1}", "测试原因", T0);
        assertThat(viaDefault).isEqualTo(viaV1).doesNotStartWith(AuditHashChain.V2_PREFIX);
        assertThat(AuditHashChain.ACTIVE_CANONICAL_VERSION).isEqualTo(1);
    }

    @Test
    @DisplayName("RISK-04 A：canonicalV2 带 v2| 前缀且与 v1 隔离")
    void canonicalV2PrefixedAndIsolated() {
        String v1 = AuditHashChain.canonicalV1(1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 100L,
            null, "{\"k\":1}", "测试原因", T0);
        String v2 = AuditHashChain.canonicalV2(1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 100L,
            null, "{\"k\":1}", "测试原因", T0, "{\"ext\":true}");
        assertThat(v2).startsWith(AuditHashChain.V2_PREFIX).isNotEqualTo(v1).endsWith("|{\"ext\":true}");
        assertThat(AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, v2))
            .isNotEqualTo(AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, v1));
    }

    @Test
    @DisplayName("RISK-04 A：canonicalByVersion 路由 1/2，未知版本拒绝")
    void canonicalByVersionRouting() {
        String v1 = AuditHashChain.canonicalByVersion(1, 1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 1L,
            null, null, null, T0, "{\"ignored\":true}");
        String v2 = AuditHashChain.canonicalByVersion(2, 1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 1L,
            null, null, null, T0, "{\"ext\":1}");
        assertThat(v1).isEqualTo(AuditHashChain.canonicalV1(1L, 9L, "张三", "MARKET_PM", "CREATE", "projects", 1L,
            null, null, null, T0));
        assertThat(v2).startsWith(AuditHashChain.V2_PREFIX);
        assertThatThrownBy(() ->
                AuditHashChain.canonicalByVersion(9, 1L, null, null, null, "X", "y", null, null, null, null, T0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不支持的 canonical 版本");
    }
}