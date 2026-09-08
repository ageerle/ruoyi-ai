package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.KpiRuleSnapshot;
import org.ruoyi.ipd.mapper.KpiRuleSnapshotMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * KPI 规则版本快照服务（P3-2.2 规则版本快照子模块）。
 *
 * <p>规则不可变：每次新增规则版本都新增一行；旧版本的 effectiveTo 由本服务在事务内闭合，
 * 避免同一时间窗多版本共存导致回算歧义。
 *
 * <p>权限：
 * <ul>
 *   <li>{@link #snapshotRuleVersion} — 仅超管（写入规则版本）</li>
 *   <li>{@link #getRuleVersion} / {@link #listVersions} — 仅超管（读取敏感规则变更）</li>
 * </ul>
 *
 * <p>R-P3-2.2-POSTREVIEW：kpi_rule_snapshots 是治理元数据表（与 system_config_versions 同语义），
 * INSERT 时显式写 {@code tenantId = "000000"}，与 {@code tenant.excludes} 登记的排除策略对齐；
 * 即便后续租户插件对 excludes 漂移，租户过滤误应用也不会导致数据不可见。
 */
@Service
@RequiredArgsConstructor
public class KpiRecordRuleVersionService {

    /** kpi_rule_snapshots 治理元数据表统一租户 ID（与 system_config_versions 对齐）。 */
    private static final String GOVERNANCE_TENANT_ID = "000000";

    /** ruleJson 长度上限（64 KiB，足以容纳权重/阈值/分类表等业务 JSON）。 */
    private static final int RULE_JSON_MAX = 65536;

    private final KpiRuleSnapshotMapper snapshotMapper;
    private final IpdPermission permission;

    /**
     * 落盘新规则版本快照，自动闭合前一版本 effectiveTo，返回新版本号。
     *
     * <p>版本号生成规则：当前最大版本号 + 1；首条版本号 = 1。
     * 同一规则 JSON 重复提交视为错误（NO-OP 不创造新版本，避免版本污染）。
     *
     * @param snapshot 规则快照输入（version/effectiveFrom/effectiveTo/ruleJson/createdBy 必填）
     * @return 新版本号
     */
    @Transactional(rollbackFor = Exception.class)
    public Long snapshotRuleVersion(KpiRuleSnapshot snapshot) {
        IpdActor admin = permission.requireAdmin();
        if (snapshot == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "规则快照不能为空");
        }
        if (isBlank(snapshot.getRuleJson())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "ruleJson 不能为空");
        }
        String ruleJson = snapshot.getRuleJson().trim();
        if (ruleJson.length() > RULE_JSON_MAX) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "ruleJson 长度超过上限 " + RULE_JSON_MAX);
        }
        Date effectiveFrom = snapshot.getEffectiveFrom();
        if (effectiveFrom == null) {
            effectiveFrom = new Date();
            snapshot.setEffectiveFrom(effectiveFrom);
        }
        // 1) 取当前最大版本号（行锁防并发同号）
        KpiRuleSnapshot latest = snapshotMapper.selectOne(
            new LambdaQueryWrapper<KpiRuleSnapshot>()
                .orderByDesc(KpiRuleSnapshot::getVersion)
                .last("LIMIT 1 FOR UPDATE"));
        long nextVersion = latest == null || latest.getVersion() == null ? 1L : latest.getVersion() + 1L;

        // 2) 重复内容检查：与当前生效规则一致则拒绝（NO-OP 不入版本）
        if (latest != null && safeEquals(latest.getRuleJson(), ruleJson)) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "规则 JSON 与当前生效版本一致，不创建新版本");
        }

        // 3) 闭合前一版本 effectiveTo（开区间右边界 = 新版本生效起点）
        if (latest != null && latest.getEffectiveTo() == null) {
            int updated = snapshotMapper.update(null,
                new LambdaUpdateWrapper<KpiRuleSnapshot>()
                    .eq(KpiRuleSnapshot::getId, latest.getId())
                    .isNull(KpiRuleSnapshot::getEffectiveTo)
                    .set(KpiRuleSnapshot::getEffectiveTo, effectiveFrom));
            if (updated != 1) {
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                    "闭合前一规则版本失败，并发写入请重试");
            }
        }

        // 4) 入库新版本（治理元数据表：tenantId 显式写 000000，与 tenant.excludes 登记对齐）
        KpiRuleSnapshot row = KpiRuleSnapshot.builder()
            .version(nextVersion)
            .effectiveFrom(effectiveFrom)
            .effectiveTo(null)
            .ruleJson(ruleJson)
            .createdBy(snapshot.getCreatedBy() != null ? snapshot.getCreatedBy() : String.valueOf(admin.id()))
            .createdAt(new Date())
            .tenantId(GOVERNANCE_TENANT_ID)
            .build();
        permission.bindCreateAudit(row, admin);
        snapshotMapper.insert(row);
        return nextVersion;
    }

    /**
     * 按版本号取规则快照；不存在时抛 IpdBusinessException(NOT_FOUND)。
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public KpiRuleSnapshot getRuleVersion(Long version) {
        permission.requireAdmin();
        if (version == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "version 不能为空");
        }
        KpiRuleSnapshot row = snapshotMapper.selectOne(
            new LambdaQueryWrapper<KpiRuleSnapshot>()
                .eq(KpiRuleSnapshot::getVersion, version));
        if (row == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "规则版本不存在: " + version);
        }
        return row;
    }

    /**
     * 列出全部规则版本（仅超管），按 version DESC。
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<KpiRuleSnapshot> listVersions() {
        permission.requireAdmin();
        return snapshotMapper.selectList(
            new LambdaQueryWrapper<KpiRuleSnapshot>()
                .orderByDesc(KpiRuleSnapshot::getVersion));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}
