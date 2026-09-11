package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditChainHead;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditChainHeadMapper;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.util.AuditHashChain;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ①②③ P 变体（owner 2026-09-05 拍板）append 锚行原子分配契约（Mockito 层）。
 *
 * <p>被锁语义：append 在审计专用 REQUIRES_NEW 事务内按「锁锚行（SELECT ... FOR UPDATE）→
 * 分配 seq/prevHash → advance 前移锚行 → insert」严格串行；锁序单一（先锚行后 audit_logs，
 * 无第二锁源 → 无死锁环）；advance 影响行数恒 1（0 = schema/chain_key 漂移，须 fail-fast）。
 *
 * <p>Mock 层锁不到的语义（由部署后真库冒烟承载，见蜂群 turnkey 包 §4.4）：
 * 100 并发全局串行零冲突、chain_heads.last_hash == 尾行 curr_hash、next_seq 连续、
 * verifyChainDetailed() 全行 OK。Mock/单测绿 ≠ 业务闭环（AGENTS.md）。
 */
@Tag("dev")
@DisplayName("①②③ P 变体：append 锚行原子分配契约")
@ExtendWith(MockitoExtension.class)
class AuditChainHeadAppendContractTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private AuditChainHeadMapper chainHeadMapper;

    @InjectMocks
    private AuditLogService service;

    private static AuditChainHead anchor(Long lastSeq, String lastHash, long nextSeq) {
        AuditChainHead head = new AuditChainHead();
        head.setChainKey("GLOBAL");
        head.setLastSeq(lastSeq);
        head.setLastHash(lastHash);
        head.setNextSeq(nextSeq);
        return head;
    }

    @Test
    @DisplayName("锁序契约：selectForUpdate → advance → insert 严格串行（无第二锁源）")
    void appendLocksAnchorBeforeAdvanceAndInsert() {
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor(1607L, "c".repeat(64), 1608L));
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.append(AuditLog.builder().action("LOGIN").createTime(new Date()).build());

        InOrder order = inOrder(chainHeadMapper, auditLogMapper);
        order.verify(chainHeadMapper).selectForUpdate("GLOBAL");
        order.verify(chainHeadMapper).advance(anyString(), anyLong(), anyString(), anyLong());
        order.verify(auditLogMapper).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("分配契约：seq=锚行 next_seq、prevHash=锚行 last_hash（GENESIS 兕底）且 advance 参数精确前移")
    void appendAllocatesSeqAndPrevHashFromAnchor() {
        String lastHash = "ab".repeat(32);
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor(1607L, lastHash, 1608L));
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        AuditLog draft = AuditLog.builder()
            .operatorId(9L).operatorName("管理员").operatorRole("SUPER_ADMIN")
            .action("LOGIN").entityType("person").entityId(9L)
            .createTime(new Date(1788700000123L))
            .build();
        AuditLog saved = service.append(draft);

        // insert 的 draft 携带锚行分配的 seq/prevHash（NEVER 已去，显式值真正进入 INSERT）
        assertThat(saved.getSeq()).isEqualTo(1608L);
        assertThat(saved.getPrevHash()).isEqualTo(lastHash);
        // advance 精确前移：last_seq=分配值、last_hash=新行 curr_hash、next_seq=分配值+1
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> lastSeqCap = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> nextSeqCap = ArgumentCaptor.forClass(Long.class);
        verify(chainHeadMapper).advance(keyCap.capture(), lastSeqCap.capture(), hashCap.capture(), nextSeqCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("GLOBAL");
        assertThat(lastSeqCap.getValue()).isEqualTo(1608L);
        assertThat(hashCap.getValue()).isEqualTo(saved.getCurrHash());
        assertThat(nextSeqCap.getValue()).isEqualTo(1609L);
    }

    @Test
    @DisplayName("GENESIS 兕底：锚行 last_hash=NULL（清库未 sync-seed 病态）→ 链首 prev=64×'0' 而非空串")
    void appendFallsBackToGenesisWhenAnchorHashNull() {
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor(0L, null, 1L));
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        AuditLog saved = service.append(AuditLog.builder().action("LOGIN").build());

        // 外部验链工具（qa06_restore_check.py 等）硬编码 GENESIS 起验：空串链首会被判 broken
        assertThat(saved.getPrevHash()).isEqualTo(AuditHashChain.GENESIS);
        assertThat(saved.getPrevHash()).hasSize(64);
    }

    @Test
    @DisplayName("advance=0 防御断言：schema/chain_key 漂移 → ISE 且零 insert（与锚行缺失 fail-fast 对称）")
    void appendFailsFastWhenAdvanceMisses() {
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor(5L, "d".repeat(64), 6L));
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(0);

        assertThatThrownBy(() -> service.append(AuditLog.builder().action("LOGIN").build()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("advance missed");
        // 静默继续会劣化为撞 uk 或错链——必须在 insert 前拦下
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("护栏先行：畸形载荷在任何锚行锁/推进之前被拒（①②③ 未动 DEF-6 护栏位置）")
    void appendRejectsMalformedPayloadBeforeTouchingAnchor() {
        assertThatThrownBy(() -> service.append(AuditLog.builder()
            .action("LOGIN").afterData("G1/QA03-JSON").build()))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        verify(chainHeadMapper, never()).selectForUpdate(anyString());
        verify(chainHeadMapper, never()).advance(anyString(), anyLong(), anyString(), anyLong());
        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("连续分配：两次 append 严格按锚行推进接续（第二次从新锚行取号，无重读 audit_logs）")
    void appendTwiceAllocatesSequentiallyFromAnchor() {
        when(chainHeadMapper.selectForUpdate("GLOBAL"))
            .thenReturn(anchor(99L, "e".repeat(64), 100L))
            .thenReturn(anchor(100L, "f".repeat(64), 101L));
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        AuditLog first = service.append(AuditLog.builder().action("LOGIN").build());
        AuditLog second = service.append(AuditLog.builder().action("EXPORT").build());

        assertThat(first.getSeq()).isEqualTo(100L);
        assertThat(second.getSeq()).isEqualTo(101L);
        assertThat(second.getPrevHash()).isEqualTo("f".repeat(64));
        // 旧 selectLast 路径已删：全程不回读 audit_logs 尾行（零 selectList）
        verify(auditLogMapper, never()).selectList(any());
        verify(auditLogMapper, times(2)).insert(any(AuditLog.class));
    }
}
