package org.ruoyi.ipd.common;

/**
 * IPD 业务参数键常量（ROOT-R1 业务参数配置化根治，P0-7）
 * <p>所有业务规则参数从此处引用，禁止 Service/Controller 字面量（防 B-RULE-01/B-RULE-05 复发）。
 * 配对落地：docs/script/sql/update/2026-09-06-ipd-p05-business-config.sql 初始 seed。
 *
 * <p>规则：
 * <ul>
 *   <li>键名须与 ipd_business_config.config_key 完全一致</li>
 *   <li>删除/重命名前必须先查 lint（勘察卡 §7 docs-link-validate.cjs）</li>
 *   <li>新增业务参数先在 DDL seed 注册，再在本类加常量</li>
 * </ul>
 */
public final class BusinessConfigKeys {
    private BusinessConfigKeys() {}

    /** 奖金池基数比例（默认 0.0500；B-RULE-01 / ZK-05） */
    public static final String BONUS_POOL_RATE = "bonus.poolRate";

    /** KPI 停发阈值（默认 60；ZK-13） */
    public static final String KPI_STOP_THRESHOLD = "kpi.stopThreshold";

    /** 删除申请冷静期（默认 3 天；ZK-17） */
    public static final String DELETION_COOLDOWN_DAYS = "deletion.cooldownDays";

    /** Gate 双签最低人数（默认 3；B-RULE-05） */
    public static final String GATE_DUAL_SIGN_COUNT = "gate.dualSignCount";

    /** 奖金池阶梯系数（达成率区间→系数；ZK-14） */
    public static final String BONUS_TIER_50  = "bonus.tierCoefficient_50";
    public static final String BONUS_TIER_80  = "bonus.tierCoefficient_80";
    public static final String BONUS_TIER_100 = "bonus.tierCoefficient_100";
    public static final String BONUS_TIER_120 = "bonus.tierCoefficient_120";

    /** KPI 修订模式（append/replace；B-RULE-02 配套） */
    public static final String KPI_REVISION_MODE = "kpi.revision.mode";

    /** Gate 评审签字期限（自然日，默认 3；B-RULE-05） */
    public static final String GATE_SIGN_DEADLINE_DAYS = "gate.signDeadlineDays";

    /** Gate 签字期限超管可延长次数上限（默认 1；B-RULE-05） */
    public static final String GATE_EXTENSION_MAX_COUNT = "gate.extension.maxCount";

    /** 删除申请组长审核超时自动升级超管的小时数（默认 48；B-RULE-05） */
    public static final String DELETION_ESCALATE_TIMEOUT_HOURS = "deletion.escalateTimeoutHours";

    /** 项目绩效系数取数策略（PROJECT_SCORE / WEIGHTED_AVG / LAST_QUARTER；P3-4.5 BR-INC-07） */
    public static final String BONUS_PERFORMANCE_STRATEGY = "bonus.performance.strategy";
}
