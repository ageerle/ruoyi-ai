package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.util.AuditHashChain;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEF-4 审计哈希链自洽契约（毫秒对称 / 升序锚定 / 竞态重试 / 链重建）。
 * <p>
 * 缺陷史（2026-09-05，v7 矩阵实测 verify chain=BROKEN 断裂 368/368）：
 * ① create_time=datetime(0) 截毫秒 vs append 用 currentTimeMillis 哈希 → 全行重算失配；
 * ② verifyChain 误用降序 wrapper 且硬编码 GENESIS/seq=1 → 结构性全断；
 * ③ selectLast→insert 无锁，多实例共库竞态断链（实证 seq=150）。
 * 本测试锁修复后的写读对称语义：模拟「写入 → 库截毫秒 → 读回 → 验链」完整闭环。
 */
@Tag("dev")
@DisplayName("DEF-4 审计哈希链自洽契约")
@ExtendWith(MockitoExtension.class)
class AuditChainSymmetryTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogService service;

    /**
     * 库侧真实语义：MySQL datetime(0) 对毫秒「四舍五入」（≥.500 进位到下一秒），**非截断**。
     * 实测证据：INSERT '00:00:00.500' → 存 '00:00:01'（tz_probe 临时表，2026-09-05）。
     */
    private static Date dbRounded(Date d) {
        return new Date((d.getTime() + 500L) / 1000L * 1000L);
    }

    /** 构造一行「按秒级对称语义自洽」的链行。 */
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

    /** 毫秒污染的 currHash（旧实现语义：不截秒）。 */
    private static String canonicalMillis(AuditLog log) {
        return AuditHashChain.canonical(log.getSeq(), log.getOperatorId(), log.getOperatorName(),
            log.getOperatorRole(), log.getAction(), log.getEntityType(), log.getEntityId(),
            log.getBeforeData(), log.getAfterData(), log.getReason(),
            log.getCreateTime().getTime());
    }

    @Test
    @DisplayName("append→库四舍五入毫秒→verify 闭环：链自洽零断裂（毫秒不对称已修）")
    void appendThenVerifyAfterDbRounding() {
        Date nowWithMillis = new Date(1788700000123L);
        when(auditLogMapper.selectList(any()))
            .thenReturn(List.of())                                   // append: 空库（GENESIS 首行）
            .thenReturn(List.of());                                  // 占位（第二轮 stub 由下方重设）
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        AuditLog draft = AuditLog.builder()
            .operatorId(9L).operatorName("管理员").operatorRole("SUPER_ADMIN")
            .action("LOGIN").entityType("person").entityId(9L)
            .createTime(nowWithMillis)
            .build();
        AuditLog saved = service.append(draft);

        // 写入侧即用截秒哈希：currHash 与「库截断后读回」的重算一致
        AuditLog dbView = AuditLog.builder()
            .id(1L).seq(saved.getSeq()).prevHash(saved.getPrevHash())
            .operatorId(9L).operatorName("管理员").operatorRole("SUPER_ADMIN")
            .action("LOGIN").entityType("person").entityId(9L)
            .currHash(saved.getCurrHash())
            .createTime(dbRounded(saved.getCreateTime()))              // 模拟 datetime(0) 四舍五入读回
            .build();
        assertThat(saved.getPrevHash()).isEqualTo(AuditHashChain.GENESIS);
        assertThat(saved.getCurrHash())
            .isEqualTo(AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, canonicalSecond(dbView)));

        // verify 读回库视图 → 零断裂
        when(auditLogMapper.selectList(any())).thenReturn(List.of(dbView));
        assertThat(service.verifyChain()).isEmpty();
    }

    @Test
    @DisplayName("append 写库前毫秒归零：≥.500 进位场景仍自洽（实测 seq=406/407 REBUILD_CHAIN 断裂根因）")
    void appendNormalizesMillisToAvoidDbRounding() {
        // .700 毫秒：datetime(0) 四舍五入会进位到下一秒；若写库前不归零，读回 +1s → 重算哈希失配
        Date highMillis = new Date(1788700000700L);
        when(auditLogMapper.selectList(any())).thenReturn(List.of());
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        AuditLog saved = service.append(AuditLog.builder()
            .operatorId(9L).operatorName("管理员").operatorRole("SUPER_ADMIN")
            .action("REBUILD_CHAIN").entityType("audit_logs")
            .createTime(highMillis)
            .build());

        // 契约①：写库的 createTime 毫秒已归零 → DB 四舍五入不会再进位（存读同值）
        assertThat(saved.getCreateTime().getTime() % 1000L).isZero();
        assertThat(dbRounded(saved.getCreateTime())).isEqualTo(saved.getCreateTime());
        assertThat(dbRounded(highMillis)).isNotEqualTo(saved.getCreateTime()); // 对照：不归零则差 1s

        // 契约②：库读回视图与写入哈希自洽 → verify 零断裂
        AuditLog dbView = AuditLog.builder()
            .id(1L).seq(saved.getSeq()).prevHash(saved.getPrevHash())
            .operatorId(9L).operatorName("管理员").operatorRole("SUPER_ADMIN")
            .action("REBUILD_CHAIN").entityType("audit_logs")
            .currHash(saved.getCurrHash())
            .createTime(dbRounded(saved.getCreateTime()))
            .build();
        when(auditLogMapper.selectList(any())).thenReturn(List.of(dbView));
        assertThat(service.verifyChain()).isEmpty();
    }

    @Test
    @DisplayName("verify 锚定库内首行：seq 不从 1 起、prev 非 GENESIS 也不误判（升序遍历）")
    void verifyAnchorsAtFirstStoredRow() {
        Date t0 = new Date(1788700001000L);
        String anchorPrev = "f".repeat(64);                          // 历史首行 prev（非 GENESIS）
        AuditLog r1 = consistentRow(10L, 5L, anchorPrev, "LOGIN", t0);
        AuditLog r2 = consistentRow(11L, 6L, r1.getCurrHash(), "PROJECT_CREATE", new Date(t0.getTime() + 60_000L));
        AuditLog r3 = consistentRow(12L, 7L, r2.getCurrHash(), "EXPORT", new Date(t0.getTime() + 120_000L));
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r2, r3));

        assertThat(service.verifyChain()).isEmpty();
    }

    @Test
    @DisplayName("verify 篡改可检：改一行业务字段 → 仅该 seq 报断（下游锚定实际 currHash 不受连坐）")
    void verifyDetectsTampering() {
        Date t0 = new Date(1788700002000L);
        AuditLog r1 = consistentRow(20L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        AuditLog r2 = consistentRow(21L, 2L, r1.getCurrHash(), "PROJECT_CREATE", new Date(t0.getTime() + 1000L));
        r2.setAction("PROJECT_DELETE");                              // 篡改业务字段
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1, r2));

        assertThat(service.verifyChain()).containsExactly(2L);
    }

    @Test
    @DisplayName("append 竞态自愈：uk_audit_seq 冲突 → 重读尾行重试成功（多实例共库）")
    void appendRetriesOnDuplicateSeq() {
        Date t0 = new Date(1788700003000L);
        AuditLog oldLast = consistentRow(30L, 9L, AuditHashChain.GENESIS, "LOGIN", t0);
        AuditLog rivalLast = consistentRow(31L, 10L, oldLast.getCurrHash(), "EXPORT", t0); // 他实例抢先
        when(auditLogMapper.selectList(any()))
            .thenReturn(List.of(oldLast))                            // 第一次读到旧尾
            .thenReturn(List.of(rivalLast));                         // 冲突重试后当前读见新尾
        when(auditLogMapper.insert(any(AuditLog.class)))
            .thenThrow(new DuplicateKeyException("uk_audit_seq"))
            .thenReturn(1);

        AuditLog saved = service.append(AuditLog.builder().action("LOGIN").createTime(t0).build());

        assertThat(saved.getSeq()).isEqualTo(11L);
        assertThat(saved.getPrevHash()).isEqualTo(rivalLast.getCurrHash());
        verify(auditLogMapper, times(2)).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("append 重试超限：连续冲突 3 次 → 抛出（不静默丢审计）")
    void appendGivesUpAfterMaxAttempts() {
        when(auditLogMapper.selectList(any())).thenReturn(List.of());
        when(auditLogMapper.insert(any(AuditLog.class)))
            .thenThrow(new DuplicateKeyException("uk_audit_seq"));

        assertThatThrownBy(() -> service.append(AuditLog.builder().action("LOGIN").build()))
            .isInstanceOf(DuplicateKeyException.class);
        verify(auditLogMapper, times(3)).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("rebuildChain：毫秒污染行全量修复 → verify 归零；仅动哈希列")
    void rebuildFixesMillisPollutedChain() {
        Date t0 = new Date(1788700004123L);                          // 带毫秒（污染特征）
        List<AuditLog> rows = new ArrayList<>();
        AuditLog r1 = consistentRow(40L, 1L, AuditHashChain.GENESIS, "LOGIN", t0);
        r1.setCurrHash(AuditHashChain.computeCurrHash(AuditHashChain.GENESIS, canonicalMillis(r1))); // 旧毫秒语义
        rows.add(r1);
        AuditLog r2 = consistentRow(41L, 2L, r1.getCurrHash(), "EXPORT", new Date(t0.getTime() + 1000L));
        r2.setCurrHash(AuditHashChain.computeCurrHash(r1.getCurrHash(), canonicalMillis(r2)));
        rows.add(r2);

        when(auditLogMapper.selectList(any())).thenReturn(rows, rows); // rebuild 一次 + verify 一次（行对象原地更新）

        long fixed = service.rebuildChain();
        assertThat(fixed).isEqualTo(2L);

        // 捕获 updateChainHash 参数并回放到行对象（模拟库内生效）
        ArgumentCaptor<Long> idCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> prevCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> currCap = ArgumentCaptor.forClass(String.class);
        verify(auditLogMapper, times(2)).updateChainHash(idCap.capture(), prevCap.capture(), currCap.capture());
        for (int i = 0; i < idCap.getAllValues().size(); i++) {
            for (AuditLog row : rows) {
                if (row.getId().equals(idCap.getAllValues().get(i))) {
                    row.setPrevHash(prevCap.getAllValues().get(i));
                    row.setCurrHash(currCap.getAllValues().get(i));
                }
            }
        }
        // 业务字段未被触碰（只动哈希两列）
        assertThat(r1.getAction()).isEqualTo("LOGIN");
        assertThat(r1.getCreateTime()).isEqualTo(t0);

        assertThat(service.verifyChain()).isEmpty();
    }

    @Test
    @DisplayName("rebuildChain 幂等：自洽链重跑 fixed=0 且零 UPDATE")
    void rebuildIsIdempotent() {
        Date t0 = new Date(1788700005000L);
        AuditLog r1 = consistentRow(50L, 3L, "a".repeat(64), "LOGIN", t0);
        when(auditLogMapper.selectList(any())).thenReturn(List.of(r1));

        assertThat(service.rebuildChain()).isZero();
        verify(auditLogMapper, times(0)).updateChainHash(anyLong(), anyString(), anyString());
        assertThat(service.verifyChain()).isEmpty();
    }
}
