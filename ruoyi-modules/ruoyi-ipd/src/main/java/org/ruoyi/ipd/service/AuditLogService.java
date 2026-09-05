package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.util.AuditHashChain;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 审计日志服务（只追加 + hash 链，v3 TS-08；AC-AUD-01 篡改可检）
 * ⚠️ 只追加：业务路径不提供任何 update/delete（G-02 配套证据链）。唯一例外：
 * {@link #rebuildChain()} 修复工具（DEF-4）——仅重算哈希列、业务字段只读，超管专属且动作本身落审计。
 *
 * <p>DEF-4 链自洽三修复（2026-09-05）：
 * <ol>
 *   <li>时间戳秒级对称：create_time 列为 datetime(0)，哈希入参统一 {@link #secondMillis} 截秒，
 *       否则写入用毫秒、verify 读回恒 .000 → 全行重算必失配；</li>
 *   <li>verifyChain 升序遍历＋锚定库内首行（原实现误用降序 wrapper 且硬编码 GENESIS/seq=1，
 *       结构性全行断判）；</li>
 *   <li>append 尾行 FOR UPDATE 当前读＋唯一键冲突重试：多实例共库串行化，防 prev/seq 竞态断链。</li>
 * </ol>
 *
 * <p>P0-5.4 补范围查询/导出：{@link #listByOperatorIds} / {@link #countByOperatorIds}
 * 均为「接受预先解析好的 operatorIds」——角色→范围（本人/本组/全局）的判定由 Controller 层
 * 依据 {@code IpdPermission}（SEC-02）与 {@code PersonMapper}（本组人员）完成，Service 不感知角色。
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    /** DEF-4：append 唯一键冲突重试上限（多实例共库竞态自愈）。 */
    private static final int APPEND_MAX_ATTEMPTS = 3;

    private final AuditLogMapper auditLogMapper;

    /** 追加一条审计（独立事务：业务失败不回滚审计） */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public AuditLog append(AuditLog draft) {
        // DEF-4：先定时间再哈希——写入与验链共用同一 Date（原实现 L41/L46 两次取 now 且毫秒被库截断）
        if (draft.getCreateTime() == null) {
            draft.setCreateTime(new Date());
        }
        if (draft.getTenantId() == null) {
            draft.setTenantId("000000");
        }
        DuplicateKeyException conflict = null;
        for (int attempt = 0; attempt < APPEND_MAX_ATTEMPTS; attempt++) {
            AuditLog last = selectLastForUpdate();
            String prevHash = last != null ? nvl(last.getCurrHash()) : AuditHashChain.GENESIS;
            long seq = (last != null && last.getSeq() != null ? last.getSeq() : 0L) + 1;
            draft.setSeq(seq);
            draft.setPrevHash(prevHash);
            draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalOf(draft, seq)));
            try {
                auditLogMapper.insert(draft);
                return draft;
            } catch (DuplicateKeyException e) {
                // 多实例共库：uk_audit_seq 冲突说明他实例已抢先尾行，重读重试（当前读可见新尾）
                conflict = e;
            }
        }
        throw conflict;
    }

    /** 全链校验：返回断裂/篡改的 seq 列表（空 = 链完整） */
    public List<Long> verifyChain() {
        List<AuditLog> all = auditLogMapper.selectList(orderBySeqAsc());
        if (all.isEmpty()) {
            return List.of();
        }
        List<Long> broken = new ArrayList<>();
        // DEF-4：锚点=库内实际首行（原实现硬编码 GENESIS/seq=1，历史首行缺失即误判；且误用降序遍历致结构性全断）
        String expectPrev = nvl(all.get(0).getPrevHash());
        Long expectSeq = all.get(0).getSeq();
        for (AuditLog log : all) {
            String expectHash = AuditHashChain.computeCurrHash(expectPrev, canonicalOf(log, log.getSeq()));
            if (!expectHash.equals(log.getCurrHash())
                || !expectPrev.equals(nvl(log.getPrevHash()))
                || !expectSeq.equals(log.getSeq())) {
                broken.add(log.getSeq());
            }
            expectPrev = log.getCurrHash();
            expectSeq = log.getSeq() + 1;
        }
        return broken;
    }

    /**
     * DEF-4 链重建：按现行 v1 秒级对称语义重算全链 prev/curr 哈希。
     * <p>仅触碰哈希两列，业务字段只读；幂等可重复执行——多实例旧 jar 仍可能写入毫秒污染行，
     * 全实例切新 jar 后终验前需重跑一次。调用方（Controller）负责超管门禁并为动作本身落审计。
     *
     * @return 修正哈希的行数
     */
    @Transactional(rollbackFor = Exception.class)
    public long rebuildChain() {
        List<AuditLog> all = auditLogMapper.selectList(orderBySeqAsc());
        if (all.isEmpty()) {
            return 0L;
        }
        String prev = nvl(all.get(0).getPrevHash());
        long fixed = 0L;
        for (AuditLog log : all) {
            String curr = AuditHashChain.computeCurrHash(prev, canonicalOf(log, log.getSeq()));
            if (!curr.equals(log.getCurrHash()) || !prev.equals(nvl(log.getPrevHash()))) {
                auditLogMapper.updateChainHash(log.getId(), prev, curr);
                fixed++;
            }
            prev = curr;
        }
        return fixed;
    }

    /**
     * 按 operatorIds 范围分页查询（P0-5.4 / AC-AUD-04、AC-AUD-05）。
     *
     * @param operatorIds 允许看到的操作人 id 集合；{@code null} 或空 = 全局（仅 SUPER_ADMIN 之路）。
     * @param pageNo       1-based
     * @param pageSize     上限 200
     * @return 倒序分页
     */
    public IPage<AuditLog> listByOperatorIds(List<Long> operatorIds, int pageNo, int pageSize) {
        int capped = Math.min(Math.max(pageSize, 1), 200);
        Page<AuditLog> page = new Page<>(Math.max(pageNo, 1), capped);
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<>();
        if (operatorIds != null && !operatorIds.isEmpty()) {
            w.in(AuditLog::getOperatorId, operatorIds);
        }
        w.orderByDesc(AuditLog::getSeq);
        return auditLogMapper.selectPage(page, w);
    }

    /** 与 {@link #listByOperatorIds} 相同范围条件的计数（导出写审计前统计覆盖行数）。 */
    public long countByOperatorIds(List<Long> operatorIds) {
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<>();
        if (operatorIds != null && !operatorIds.isEmpty()) {
            w.in(AuditLog::getOperatorId, operatorIds);
        }
        return auditLogMapper.selectCount(w);
    }

    private AuditLog selectLast() {
        List<AuditLog> list = auditLogMapper.selectList(orderBySeq().last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** DEF-4：尾行当前读＋行锁，串行化多实例并发 append（重试兜底见 {@link #append}）。 */
    private AuditLog selectLastForUpdate() {
        List<AuditLog> list = auditLogMapper.selectList(orderBySeq().last("limit 1 for update"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** DEF-4：写读两侧共用的 canonical 构造（时间戳一律截秒，与 datetime(0) 列精度对称）。 */
    private static String canonicalOf(AuditLog log, long seq) {
        return AuditHashChain.canonical(seq, log.getOperatorId(), log.getOperatorName(),
            log.getOperatorRole(), log.getAction(), log.getEntityType(), log.getEntityId(),
            log.getBeforeData(), log.getAfterData(), log.getReason(), secondMillis(log.getCreateTime()));
    }

    /** DEF-4：毫秒→整秒（MySQL datetime(0) 截断语义的 Java 侧对齐）。 */
    private static long secondMillis(Date d) {
        return d == null ? 0L : d.getTime() / 1000L * 1000L;
    }

    private LambdaQueryWrapper<AuditLog> orderBySeq() {
        return new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getSeq);
    }

    /** DEF-4：验链/重建必须升序遍历（原 verifyChain 误用降序 wrapper 致结构性全断）。 */
    private LambdaQueryWrapper<AuditLog> orderBySeqAsc() {
        return new LambdaQueryWrapper<AuditLog>().orderByAsc(AuditLog::getSeq);
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
