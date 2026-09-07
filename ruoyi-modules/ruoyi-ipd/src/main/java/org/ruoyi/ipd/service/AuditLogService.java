package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditChainHead;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.dto.AuditChainVerifyResult;
import org.ruoyi.ipd.mapper.AuditChainHeadMapper;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.util.AuditHashChain;
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
 *   <li>时间戳秒级对称：create_time 列为 datetime(0)，MySQL 对其毫秒是「四舍五入」（≥.500
 *       进位到下一秒）而哈希入参是截断——因此写库前必须先把 {@code createTime} 毫秒归零，
 *       否则约半数新行读回时间 +1s → 重算哈希失配（实测 seq=406/407 断裂）；</li>
 *   <li>verifyChain 升序遍历＋锚定库内首行（原实现误用降序 wrapper 且硬编码 GENESIS/seq=1，
 *       结构性全行断判）；</li>
 *   <li>append 竞态防护（2026-09-05 晚升级为 ①②③ P 变体，owner 拍板）：由 uk_audit_seq
 *       冲突自愈重试改为 audit_log_chain_heads 单行锚悲观锁原子分配——SELECT ... FOR UPDATE
 *       锁 GLOBAL 锚行 → seq=next_seq、prevHash=last_hash → advance 前移锚行 → insert
 *      （NEVER 已去，seq 显式入 INSERT）。全局串行、零重试零 CAS 竞态；DEF-4「禁锁定读」指
 *       旧 audit_logs 路径，chain_heads 表级 SELECT,UPDATE 已授、锁定读合法（Q6 REVOKE 后亦然）。
 *       旧重试循环/catch(DuplicateKeyException)/selectLast()/orderBySeq() 已删；陈旧 seed 撞
 *       uk 时响亮失败不自愈（防线 = 停写窗口 sync-seed + 部署后 verifyChain 冒烟）。</li>
 * </ol>
 *
 * <p>DEF-6 载荷列往返对称（2026-09-05，owner 选定方案 A + 护栏配套）：
 * {@code before_data/after_data} 原为 MySQL {@code json} 列，读回时被服务端规范化渲染
 * （键排序按 UTF-8 字节长度→字典序、成员间插 {@code ", "}、{@code 1e3}→{@code 1000.0}），
 * 与写入侧用于算 {@code curr_hash} 的 Jackson 紧凑串永不相等 → 带载荷审计行写完即被
 * {@link #verifyChain()} 判断裂（实证 seq 466/485/502，断裂行 100% 携带载荷）。
 * 修复 = DDL 把两列改 {@code longtext}（{@code docs/script/sql/update/2026-09-05-ipd-audit-payload-longtext.sql}）
 * 保字节精确往返，不动已冻结哈希协议 v1。代价：{@code json} 列类型原本同时是 DEF-1 的 DB 层
 * fail-fast 护栏，故 {@link #append} 入口补上 {@link AuditEventData#requireJson} 应用层校验，
 * 非法载荷仍立即抛出并回滚（校验必须在重试循环之外——它是 {@code DuplicateKeyException} 的父类）。
 *
 * <p>P0-5.4 补范围查询/导出：{@link #listByOperatorIds} / {@link #countByOperatorIds}
 * 均为「接受预先解析好的 operatorIds」——角色→范围（本人/本组/全局）的判定由 Controller 层
 * 依据 {@code IpdPermission}（SEC-02）与 {@code PersonMapper}（本组人员）完成，Service 不感知角色。
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    /** ①②③：审计链分配器锚行键（audit_log_chain_heads 单行 GLOBAL）。 */
    private static final String CHAIN_KEY_GLOBAL = "GLOBAL";

    private final AuditLogMapper auditLogMapper;
    private final AuditChainHeadMapper chainHeadMapper;

    /** 追加一条审计（独立事务：业务失败不回滚审计；①②③ P 变体：锚行悲观锁原子分配 seq/prevHash） */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public AuditLog append(AuditLog draft) {
        // DEF-6 护栏：列类型改 longtext 后 DB 不再校验 JSON 合法性，在此复刻原 fail-fast。
        // 必须位于锚行锁之前：畸形载荷须立即抛出回滚，不得进入任何锁/推进路径。
        AuditEventData.requireJson(draft.getBeforeData(), "before_data");
        AuditEventData.requireJson(draft.getAfterData(), "after_data");
        // DEF-4：先定时间再哈希——写入与验链共用同一 Date，且毫秒必须归零后再写库：
        // datetime(0) 对毫秒四舍五入（≥.500 进位），而 secondMillis 是截断，不归零则读回 +1s 哈希失配
        Date base = draft.getCreateTime() == null ? new Date() : draft.getCreateTime();
        draft.setCreateTime(new Date(secondMillis(base)));
        if (draft.getTenantId() == null) {
            draft.setTenantId("000000");
        }
        // ①②③ P 变体：锚行悲观锁 → 原子分配 seq/prevHash（全局串行，零重试零 CAS 竞态；锁序单一无死锁环）
        AuditChainHead head = chainHeadMapper.selectForUpdate(CHAIN_KEY_GLOBAL);
        if (head == null) {
            // 锚行缺失 = seed 未初始化/被清：fail-fast，禁止代码自举（自举会与并发方竞态；修复走停写窗口 sync-seed runbook）
            throw new IllegalStateException(
                "audit_log_chain_heads missing GLOBAL anchor — run seed-sync (PR就绪包 §4.3) before appending");
        }
        long seq = head.getNextSeq();
        // GENESIS 兕底而非 nvl 空串：锚行 last_hash=NULL（清库后未 sync-seed 的病态）时，
        // 链首 prevHash 必须是 64×'0'（外部验链工具硬编码 GENESIS 起验）
        String prevHash = head.getLastHash() == null ? AuditHashChain.GENESIS : head.getLastHash();
        draft.setSeq(seq);                                   // NEVER 已去：显式值真正进入 INSERT
        draft.setPrevHash(prevHash);
        draft.setCurrHash(AuditHashChain.computeCurrHash(prevHash, canonicalOf(draft, seq)));
        // advance=1 防御断言（锁保护下正常必 1；0 = schema/chain_key 漂移，静默继续会劣化为
        // 撞 uk 或错链——与 head==null fail-fast 对称）
        if (chainHeadMapper.advance(CHAIN_KEY_GLOBAL, seq, draft.getCurrHash(), seq + 1) != 1) {
            throw new IllegalStateException("audit chain anchor advance missed — schema/config drift suspected");
        }
        auditLogMapper.insert(draft);
        return draft;
    }

    /**
     * 全链校验（兼容出口）：返回断裂/缺行的 seq 合并列表（空 = 链完整）。
     *
     * <p>语义与分列改造前完全一致，供既有消费者与契约测继续使用；
     * 需区分「哈希不符」与「seq 缺行」时请用 {@link #verifyChainDetailed()}。
     */
    public List<Long> verifyChain() {
        return verifyChainDetailed().mergedBroken();
    }

    /**
     * 全链校验（分列出口，DEF-9 / 设计稿 G5）：把三条判据按**成因**拆为 HASH 与 GAP 两类。
     *
     * <p>判据③（seq 严格连续）单独归 {@code gaps}：它由缺行触发（删行 / 事务回滚 /
     * InnoDB 自增值不回填），{@link #rebuildChain()} 治不了；而判据①②归 {@code hashBroken}，
     * 可由 rebuild 重算修复。三类判据混在一个 {@code broken} 里时，「篡改」与「缺行」无法区分：
     * 实测 seq 1309（{@code prev_hash} 与 seq 609 的 {@code curr_hash} 相符、无载荷，纯因
     * 609→1309 空洞触发③）曾被归因为「{@code curr_hash} 由旧算法 jar 生成」，
     * 导致连跑两次 rebuild 仍无法消除。分列后该场景直报 {@code verdict()=GAP}。
     *
     * <p>注：同一行可同时入两类（例如缺行且哈希也不符），故 {@code mergedBroken()}
     * 需去重才能等价于原 {@code broken}。
     */
    public AuditChainVerifyResult verifyChainDetailed() {
        List<AuditLog> all = auditLogMapper.selectList(orderBySeqAsc());
        if (all.isEmpty()) {
            return new AuditChainVerifyResult(List.of(), List.of(), 0);
        }
        List<Long> hashBroken = new ArrayList<>();
        List<Long> gaps = new ArrayList<>();
        // DEF-4：锚点=库内实际首行（原实现硬编码 GENESIS/seq=1，历史首行缺失即误判；且误用降序遍历致结构性全断）
        String expectPrev = nvl(all.get(0).getPrevHash());
        Long expectSeq = all.get(0).getSeq();
        for (AuditLog log : all) {
            String expectHash = AuditHashChain.computeCurrHash(expectPrev, canonicalOf(log, log.getSeq()));
            // DEF-9：连续性判据先单独归档，再判哈希——两个 if 不互斥，不可合并回单个或分支
            if (!expectSeq.equals(log.getSeq())) {
                gaps.add(log.getSeq());
            }
            if (!expectHash.equals(log.getCurrHash())
                || !expectPrev.equals(nvl(log.getPrevHash()))) {
                hashBroken.add(log.getSeq());
            }
            expectPrev = log.getCurrHash();
            expectSeq = log.getSeq() + 1;
        }
        return new AuditChainVerifyResult(hashBroken, gaps, all.size());
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

    /** 审计导出硬上限（QA-05-P3 / PERF-AUD P2-2 防全量物化 OOM）。 */
    public static final long EXPORT_HARD_LIMIT = 50_000L;

    /**
     * 按 operatorIds 范围分页查询（P0-5.4 / AC-AUD-04、AC-AUD-05）。
     *
     * <p>QA-05-P3 游标分页重载：{@code beforeSeq} 非 null 时走 {@code seq < beforeSeq ORDER BY seq DESC LIMIT n}
     * 窗口扫描（命中 {@code idx_al_operator_seq} 覆盖索引），消除深页 OFFSET + filesort。
     * {@code beforeSeq} 为 null 时保持旧 OFFSET 行为完全兼容（不传 = 旧调用路径）。
     *
     * @param operatorIds 允许看到的操作人 id 集合；{@code null} 或空 = 全局（仅 SUPER_ADMIN 之路）。
     * @param pageNo       1-based
     * @param pageSize     上限 200
     * @param beforeSeq    游标（仅取 seq 严格小于此值，按 seq DESC）；null = 旧 OFFSET 行为
     * @return 倒序分页
     */
    public IPage<AuditLog> listByOperatorIds(List<Long> operatorIds, int pageNo, int pageSize, Long beforeSeq) {
        int capped = Math.min(Math.max(pageSize, 1), 200);
        Page<AuditLog> page = new Page<>(Math.max(pageNo, 1), capped);
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<>();
        if (operatorIds != null && !operatorIds.isEmpty()) {
            w.in(AuditLog::getOperatorId, operatorIds);
        }
        // QA-05-P3：beforeSeq 非 null 走游标窗口（旧 OFFSET 调用方传 null 走全兼容旧路径）
        if (beforeSeq != null) {
            w.lt(AuditLog::getSeq, beforeSeq);
        }
        w.orderByDesc(AuditLog::getSeq);
        return auditLogMapper.selectPage(page, w);
    }

    /** 兼容旧调用方：未传 beforeSeq 走旧 OFFSET 路径（AC-AUD-04/05 历史消费者）。 */
    public IPage<AuditLog> listByOperatorIds(List<Long> operatorIds, int pageNo, int pageSize) {
        return listByOperatorIds(operatorIds, pageNo, pageSize, null);
    }

    /**
     * 与 {@link #listByOperatorIds} 相同范围条件的计数（导出写审计前统计覆盖行数）。
     *
     * <p>QA-05-P3：超 {@link #EXPORT_HARD_LIMIT} 抛业务异常（中文文案），禁止全量物化 OOM。
     * 上限 5 万行覆盖 50 万行库的全量导出场景，超出强制客户端缩小范围或按 seq 窗口分批导出。
     */
    public long countByOperatorIds(List<Long> operatorIds) {
        LambdaQueryWrapper<AuditLog> w = new LambdaQueryWrapper<>();
        if (operatorIds != null && !operatorIds.isEmpty()) {
            w.in(AuditLog::getOperatorId, operatorIds);
        }
        long count = auditLogMapper.selectCount(w);
        if (count > EXPORT_HARD_LIMIT) {
            throw new ServiceException("审计导出行数 " + count + " 超过硬上限 " + EXPORT_HARD_LIMIT
                + " 行，请缩小时间/人员范围后重试（QA-05-P3 防 OOM）");
        }
        return count;
    }

    /** DEF-4：写读两侧共用的 canonical 构造（时间戳一律截秒，与 datetime(0) 列精度对称）。 */
    private static String canonicalOf(AuditLog log, long seq) {
        return AuditHashChain.canonical(seq, log.getOperatorId(), log.getOperatorName(),
            log.getOperatorRole(), log.getAction(), log.getEntityType(), log.getEntityId(),
            log.getBeforeData(), log.getAfterData(), log.getReason(), secondMillis(log.getCreateTime()));
    }

    /** DEF-4：毫秒→整秒截断（写库前归零用，避开 MySQL datetime(0) 的四舍五入进位）。 */
    private static long secondMillis(Date d) {
        return d == null ? 0L : d.getTime() / 1000L * 1000L;
    }

    /** DEF-4：验链/重建必须升序遍历（原 verifyChain 误用降序 wrapper 致结构性全断）。 */
    private LambdaQueryWrapper<AuditLog> orderBySeqAsc() {
        return new LambdaQueryWrapper<AuditLog>().orderByAsc(AuditLog::getSeq);
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
