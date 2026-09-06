package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditChainHead;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditChainHeadMapper;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEF-6 应用层 JSON 护栏契约（owner 2026-09-05 选定「方案 A + 护栏配套」的护栏侧回归锁）。
 *
 * <p>缺陷史：{@code audit_logs.before_data/after_data} 原为 MySQL {@code json} 列，读回被服务端
 * 规范化渲染（键排序按 UTF-8 字节长度→字典序、成员间插 {@code ", "}、{@code 1e3}→{@code 1000.0}），
 * 与写入侧算 {@code curr_hash} 的 Jackson 紧凑串永不相等 → 带载荷审计行写完即被 verify 判断裂
 * （实证 seq 466/485/502，断裂行 100% 携带载荷，P0-9.1 三跑 8 项链断言恒 FAIL）。
 *
 * <p>方案 A 把两列改 {@code longtext} 换取字节精确往返，但 {@code json} 列类型原本<b>同时</b>是
 * DEF-1 的 DB 层 fail-fast 护栏（纯文本直写 → {@code MysqlDataTruncation} → 同事务整体回滚 →
 * 接口 500 + DB 零写入，修复 {@code b74f46bf}）。改列型即移除该护栏，畸形载荷会退化为 fail-late
 * （静默入库、读取/导出/前端解析时才炸，且脏载荷进入 hash 链）。本类锁死补回的应用层护栏：
 * <ol>
 *   <li>畸形 before/afterData 立即被拒，且<b>零 DB 写入</b>（不 insert、不 selectLast）；</li>
 *   <li>护栏位于锚行锁<b>之前</b>（旧 uk_audit_seq 重试循环已随 ①②③ P 变体删除）——
 *       畸形载荷立即抛出，不进入任何锁/推进路径
 *       （{@code DataIntegrityViolationException} 是 {@code DuplicateKeyException}  的父类）；</li>
 *   <li>合法载荷与空载荷照常放行，且护栏<b>不改写</b>载荷字节（护栏 ≠ 规范化器，
 *       否则又制造写读不对称）。</li>
 * </ol>
 *
 * <p>本类是 Mock 层形状/语义锁；「改列型后真的字节精确往返、链断言转 PASS」的语义证据由真库
 * 真 HTTP 给出：{@code docs/ipd-系统说明/验收/P0-9.1-业务链真实验收-20260905.py} 的 8 项链断言
 * 与 {@code attribute_broken} 归因探针。
 */
@Tag("dev")
@DisplayName("DEF-6 审计载荷应用层 JSON 护栏契约")
@ExtendWith(MockitoExtension.class)
class AuditPayloadJsonGuardTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @Mock
    private AuditChainHeadMapper chainHeadMapper;

    @InjectMocks
    private AuditLogService service;

    /** ①②③ P 变体锚行（append 从 chain_heads 原子分配；旧 selectLast 路径已删）。 */
    private static AuditChainHead anchor() {
        AuditChainHead head = new AuditChainHead();
        head.setChainKey("GLOBAL");
        head.setLastSeq(0L);
        head.setLastHash(null);
        head.setNextSeq(1L);
        return head;
    }

    private static AuditLog draft(String beforeData, String afterData) {
        return AuditLog.builder()
            .operatorId(1L).operatorName("超管").operatorRole("SUPER_ADMIN")
            .action("CREATE").entityType("GATE_ELEMENT").entityId(66L)
            .beforeData(beforeData).afterData(afterData)
            .build();
    }

    @Test
    @DisplayName("DEF-1 原样的纯文本载荷仍被拒，且零 DB 写入（fail-fast 未随列型丢失）")
    void plainTextAfterDataIsRejectedWithoutAnyDbWrite() {
        // 这正是 DEF-1 的畸形载荷形状：gateCode + "/" + elementCode 纯文本
        AuditLog bad = draft(null, "G1/QA03-JSON");

        assertThatThrownBy(() -> service.append(bad))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("after_data")
            .hasMessageContaining("不是合法 JSON");

        verify(auditLogMapper, never()).insert(any(AuditLog.class));
        verify(auditLogMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("畸形 before_data 同样被拒（护栏覆盖两列，不只 after_data）")
    void malformedBeforeDataIsRejected() {
        assertThatThrownBy(() -> service.append(draft("{not json", null)))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("before_data");

        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("截断 JSON 被拒（现实损坏形态：未闭合括号）")
    void truncatedJsonIsRejected() {
        assertThatThrownBy(() -> service.append(draft(null, "{\"detail\":\"x\"")))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("after_data");

        verify(auditLogMapper, never()).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("护栏在任何锁/推进路径之前：不会被当成链分配冲突吞掉（父子类型双锁）")
    void guardIsNotSwallowedByDuplicateKeyRetry() {
        assertThatThrownBy(() -> service.append(draft(null, "oops")))
            // 若护栏误置于锁/推进之后，畸形载荷可能被链路径的异常类型掩盖；
            // 二者是父子关系，故必须显式锁「不是子类」+「insert 零次」两条。
            .isInstanceOf(DataIntegrityViolationException.class)
            .isNotInstanceOf(DuplicateKeyException.class);

        verify(auditLogMapper, never()).insert(any(AuditLog.class));
        // 零次 selectList 即证明护栏在最前生效（旧 selectLast 路径已删，verify 链不在此测）
        verify(auditLogMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("合法 JSON 载荷放行，且护栏不改写载荷字节（护栏 ≠ 规范化器）")
    void validJsonPayloadPassesAndIsNotRewritten() {
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor());
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);
        // 故意用「MySQL json 列会重排」的键序与字节长度组合：zz(2)/a(1)/mm(2)
        String payload = "{\"zz\":1,\"a\":2,\"mm\":3}";

        service.append(draft(null, payload));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        // 写入 DB 的必须是原字节：一旦护栏顺手规范化，就重新制造了 DEF-6 的写读不对称
        assertThat(captor.getValue().getAfterData()).isEqualTo(payload);
        assertThat(captor.getValue().getCurrHash()).isNotBlank();
    }

    @Test
    @DisplayName("AuditEventData.json 产出的紧凑串放行（6 个业务写入点的实际形状）")
    void compactPayloadFromAuditEventDataPasses() {
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor());
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.append(draft(null, AuditEventData.json("detail", "G1/QA03-JSON", "outcome", "PASS")));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getAfterData())
            .isEqualTo("{\"detail\":\"G1/QA03-JSON\",\"outcome\":\"PASS\"}");
    }

    @Test
    @DisplayName("null 与空串载荷放行（两列可空，多数审计行无载荷，护栏不得误伤）")
    void nullAndEmptyPayloadsStayLegal() {
        when(chainHeadMapper.selectForUpdate("GLOBAL")).thenReturn(anchor());
        when(chainHeadMapper.advance(anyString(), anyLong(), anyString(), anyLong())).thenReturn(1);
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        service.append(draft(null, null));
        service.append(draft("", ""));

        verify(auditLogMapper, times(2)).insert(any(AuditLog.class));
    }
}
