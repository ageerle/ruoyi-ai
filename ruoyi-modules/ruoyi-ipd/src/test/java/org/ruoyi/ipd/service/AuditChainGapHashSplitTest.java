package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AuditChainVerifyResult;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.util.AuditHashChain;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * DEF-9 / 审计链顶层设计稿 T-2：{@link AuditLogService#verifyChainDetailed()} 的 HASH/GAP 分列契约。
 *
 * <p>与 {@link AuditChainSymmetryTest}（锁合并出口 {@code verifyChain()} 的自洽语义）互补：
 * 本测锁「三条判据按成因分列」的四态诊断，确保 {@code chain} 字段能区分
 * 「可 rebuildChain 修复的哈希断裂」与「rebuild 治不了的 seq 空洞」。
 *
 * <p>核心回归 = 场景②：链接自洽（{@code prev_hash} 等于前一实际行的 {@code curr_hash}）
 * 但 seq 跳跃——正是活库 seq 1309 的形态（{@code prev=609.curr}、无载荷、纯因 609→1309 空洞）。
 * 分列前它被混入 {@code broken} 并被误归因为「旧算法 jar 生成 curr_hash」，连跑两次 rebuild 无法消除；
 * 分列后直报 {@code verdict()=GAP}、{@code hashBroken} 为空，归因不再有歧义。
 */
@Tag("dev")
@DisplayName("DEF-9 审计链验签 HASH/GAP 分列契约")
@ExtendWith(MockitoExtension.class)
class AuditChainGapHashSplitTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService service;

    /** 构造一行「按秒级对称语义自洽」的链行（与 AuditChainSymmetryTest 同款 helper）。 */
    private static AuditLog consistentRow(long id, long seq, String prevHash, String action, Date createTime) {
        AuditLog log = AuditLog.builder()
            .id(id).seq(seq).prevHash(prevHash)
            .operatorId(9L).operatorName("管理员").operatorRole("SUPER_ADMIN")
            .action(action).entityType("projects").entityId(1L)
            .createTime(createTime)
            .build();
        log.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalSecond(log)));
        return log;
    }

    /** 秒级截断的 canonical（与 AuditLogService.canonicalOf 语义一致）。 */
    private static String canonicalSecond(AuditLog log) {
        return AuditHashChain.canonical(log.getSeq(), log.getOperatorId(), log.getOperatorName(),
            log.getOperatorRole(), log.getAction(), log.getEntityType(), log.getEntityId(),
            log.getBeforeData(), log.getAfterData(), log.getReason(),
            log.getCreateTime().getTime() / 1000L * 1000L);
    }

    @Test
    @DisplayName("① 完整链：verdict=OK，两类均空，total 计数正确")
    void intactChainReportsOk() {
        Date t0 = new Date(1788700010000L);
        AuditLog r1 = consistentRow(1L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        AuditLog r2 = consistentRow(2L, 2L, r1.getCurrHash(), "PROJECT_CREATE", new Date(t0.getTime() + 60_000L));
        AuditLog r3 = consistentRow(3L, 3L, r2.getCurrHash(), "EXPORT", new Date(t0.getTime() + 120_000L));
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r2, r3));

        AuditChainVerifyResult result = service.verifyChainDetailed();

        assertThat(result.ok()).isTrue();
        assertThat(result.verdict()).isEqualTo("OK");
        assertThat(result.hashBroken()).isEmpty();
        assertThat(result.gaps()).isEmpty();
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.mergedBroken()).isEmpty();
    }

    @Test
    @DisplayName("② 空洞但链接自洽（seq 1309 形态）：verdict=GAP，gaps=[4]，hashBroken 为空")
    void gapOnlyWhenSeqSkipsButHashLinks() {
        Date t0 = new Date(1788700020000L);
        AuditLog r1 = consistentRow(1L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        AuditLog r2 = consistentRow(2L, 2L, r1.getCurrHash(), "PROJECT_CREATE", new Date(t0.getTime() + 60_000L));
        // seq=4：跳过 3（空洞），但 prev 仍锚定实际前一行 r2 的 currHash → 哈希链自洽，纯 seq 不连续
        AuditLog r4 = consistentRow(4L, 4L, r2.getCurrHash(), "EXPORT", new Date(t0.getTime() + 180_000L));
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r2, r4));

        AuditChainVerifyResult result = service.verifyChainDetailed();

        assertThat(result.ok()).isFalse();
        assertThat(result.verdict()).isEqualTo("GAP");
        assertThat(result.gaps()).containsExactly(4L);
        assertThat(result.hashBroken()).isEmpty();     // 关键：不得因缺行而误报哈希断裂
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.mergedBroken()).containsExactly(4L);
    }

    @Test
    @DisplayName("③ 篡改业务字段：verdict=HASH_BROKEN，hashBroken=[2]，gaps 为空")
    void hashBrokenOnlyWhenFieldTampered() {
        Date t0 = new Date(1788700030000L);
        AuditLog r1 = consistentRow(20L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        AuditLog r2 = consistentRow(21L, 2L, r1.getCurrHash(), "PROJECT_CREATE", new Date(t0.getTime() + 60_000L));
        r2.setAction("PROJECT_DELETE");               // 篡改业务字段，currHash 仍是篡改前算的
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r2));

        AuditChainVerifyResult result = service.verifyChainDetailed();

        assertThat(result.ok()).isFalse();
        assertThat(result.verdict()).isEqualTo("HASH_BROKEN");
        assertThat(result.hashBroken()).containsExactly(2L);
        assertThat(result.gaps()).isEmpty();          // seq 连续，不得误报空洞
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.mergedBroken()).containsExactly(2L);
    }

    @Test
    @DisplayName("④ 空洞与篡改并存：verdict=BROKEN，hashBroken=[2]、gaps=[4]，合并=[2,4]")
    void brokenVerdictWhenGapAndHashCoexist() {
        Date t0 = new Date(1788700040000L);
        AuditLog r1 = consistentRow(30L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        AuditLog r2 = consistentRow(31L, 2L, r1.getCurrHash(), "PROJECT_CREATE", new Date(t0.getTime() + 60_000L));
        r2.setAction("PROJECT_DELETE");               // 篡改 → r2 入 hashBroken
        AuditLog r4 = consistentRow(33L, 4L, r2.getCurrHash(), "EXPORT", new Date(t0.getTime() + 180_000L));
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r2, r4));

        AuditChainVerifyResult result = service.verifyChainDetailed();

        assertThat(result.verdict()).isEqualTo("BROKEN");
        assertThat(result.hashBroken()).containsExactly(2L);
        assertThat(result.gaps()).containsExactly(4L);
        assertThat(result.mergedBroken()).containsExactly(2L, 4L);
    }

    @Test
    @DisplayName("⑤ 同一 seq 既缺行又哈希断裂：mergedBroken 去重=[3]，锁 verifyChain()==合并出口")
    void mergedBrokenDeduplicatesSameSeqInBothBuckets() {
        Date t0 = new Date(1788700050000L);
        AuditLog r1 = consistentRow(40L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        // seq=3 跳过 2（空洞 → gaps）+ 篡改 action（哈希不符 → hashBroken）：同一 seq 落两桶
        AuditLog r3 = consistentRow(41L, 3L, r1.getCurrHash(), "EXPORT", new Date(t0.getTime() + 120_000L));
        r3.setAction("EXPORT_TAMPERED");
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r3));

        AuditChainVerifyResult result = service.verifyChainDetailed();

        assertThat(result.verdict()).isEqualTo("BROKEN");
        assertThat(result.hashBroken()).containsExactly(3L);
        assertThat(result.gaps()).containsExactly(3L);
        // distinct 契约：合并去重后仅一个 3（若无 distinct 会得到 [3,3]）
        assertThat(result.mergedBroken()).containsExactly(3L);
        // 兼容出口回归锁：verifyChain() 必须逐字等于 mergedBroken()
        assertThat(service.verifyChain()).containsExactly(3L);
    }
}
