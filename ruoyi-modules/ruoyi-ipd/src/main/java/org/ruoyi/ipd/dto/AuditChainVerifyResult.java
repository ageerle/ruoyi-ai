package org.ruoyi.ipd.dto;

import java.util.List;
import java.util.stream.Stream;

/**
 * DEF-9 / 审计链顶层设计稿 G5：全链校验结果，把「哈希不符」与「seq 缺行」分列报告。
 *
 * <p>两类必须分开，因为成因与修复手段完全不同：
 * <ul>
 *   <li>{@code hashBroken}——{@code curr_hash} 重算不符或 {@code prev_hash} 链接不符：
 *       成因为篡改、哈希协议/算法不一致、或 DEF-6 那类「写入侧与读回侧载荷形态不对称」；
 *       可由 {@code AuditLogService.rebuildChain()} 重算哈希两列修复。</li>
 *   <li>{@code gaps}——seq 不严格连续：成因为删行、事务回滚，或 InnoDB 自增值在
 *       DELETE/回滚后不回填留下的空洞；{@code rebuildChain()} 只重算哈希两列、
 *       <b>治不了缺行</b>，须补齐行或调整判据语义。</li>
 * </ul>
 *
 * <p>混同报告的实测代价（2026-09-05）：seq 1309 的 {@code prev_hash} 与 seq 609 的
 * {@code curr_hash} 相符、该行无载荷，纯因 609→1309 空洞触发连续性判据，却被归因为
 * 「{@code curr_hash} 由旧算法 jar 生成」，导致连跑两次 rebuildChain（fixed=22、fixed=19）
 * 都无法消除断裂。分列后该场景直接报 {@code verdict()=GAP}，归因不再有歧义。
 * 详见 {@code docs/ipd-系统说明/验收/AUDIT-CHAIN-设计稿第二方复核-20260905.md} §6。
 *
 * @param hashBroken 哈希不符的 seq（升序）
 * @param gaps       seq 不连续的 seq（升序，即每处空洞后的首行）
 * @param total      参与校验的总行数
 */
public record AuditChainVerifyResult(
    List<Long> hashBroken,
    List<Long> gaps,
    int total
) {

    /** 链是否完整（两类均空）。 */
    public boolean ok() {
        return hashBroken.isEmpty() && gaps.isEmpty();
    }

    /**
     * 三态诊断，供 HTTP 端点与验收脚本区分「可 rebuild 修复」与「须补行或改判据」。
     *
     * @return {@code OK}（链完整）/ {@code HASH_BROKEN}（仅哈希问题）/
     *         {@code GAP}（仅缺行）/ {@code BROKEN}（两类同时存在）
     */
    public String verdict() {
        if (ok()) {
            return "OK";
        }
        if (gaps.isEmpty()) {
            return "HASH_BROKEN";
        }
        if (hashBroken.isEmpty()) {
            return "GAP";
        }
        return "BROKEN";
    }

    /**
     * 兼容出口：两类合并、去重、升序——语义与分列改造前的 {@code broken} 完全一致
     * （原实现把三条判据写在同一个 {@code if} 的或分支里，同一行只入列一次）。
     * 既有消费者（{@code GET /api/v1/audit-logs/verify} 的 {@code broken} 字段、
     * P0-9.1 验收脚本）据此无需改动。
     */
    public List<Long> mergedBroken() {
        return Stream.concat(hashBroken.stream(), gaps.stream())
            .distinct()
            .sorted()
            .toList();
    }
}
