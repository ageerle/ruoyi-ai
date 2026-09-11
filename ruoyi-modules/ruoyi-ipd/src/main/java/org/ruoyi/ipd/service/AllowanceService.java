package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 月度津贴台账服务（P3-3.1/3.2/3.3；AC-INC-03/04/05/06/07/08；BR-INC-02/03/11）
 *
 * <p>核心规则：
 * <ul>
 *   <li>AC-INC-03：L3 级 PM 同时绑定 4 个项目 ⇒ 2000×4=8000，封顶 2000×2=4000，实发 4000</li>
 *   <li>AC-INC-04：绑定 2 个项目 ⇒ 2000×2=4000，未超封顶，全额发放</li>
 *   <li>AC-INC-06：津贴不乘绩效系数（与奖金分离）</li>
 *   <li>BR-INC-02：评级在绑定 ProjectMember 时锁定（lockedLevel/lockedAmount 来自 P2-4.1）</li>
 *   <li>BR-INC-03：多项目叠加，封顶 2×基准额 capMultiplier</li>
 *   <li>AC-INC-05：绩效综合 < 60 当月停发（BR-INC-11）</li>
 *   <li>AC-INC-07：附加项目连续 60 天无产出（动作/交付物/记录/Gate 四类并集）⇒ 触发待确认停发单</li>
 *   <li>AC-INC-08：主项目（非附加）无产出 ⇒ 不触发停发</li>
 *   <li>幂等：(personId, projectId, month) 唯一，重复执行不重复台账（P3-3.3）</li>
 *   <li>P3-3.3：月中移交当月按月初人员、新人次月生效；退出次月停发</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AllowanceService {

    private final AllowanceLedgerMapper allowanceLedgerMapper;
    private final ProjectMemberMapper projectMemberMapper;

    /** AC-INC-03/04：默认 2 倍封顶 */
    private static final BigDecimal DEFAULT_CAP_MULTIPLIER = new BigDecimal("2");

    /** P3-3.2 AC-INC-05/07：绩效低于 60 停发阈值 */
    public static final BigDecimal SCORE_STOP_THRESHOLD = new BigDecimal("60");

    /** P3-3.2 AC-INC-07：附加项目连续无产出天数阈值 */
    public static final int NO_OUTPUT_DAYS_THRESHOLD = 60;

    /**
     * 计算单项目津贴（绑定 ProjectMember 时的 lockedAmount 已锁定）
     * 不会乘绩效系数（AC-INC-06）
     */
    public BigDecimal calculateProjectAllowance(ProjectMember member) {
        if (member == null || member.getLockedAmount() == null) {
            return BigDecimal.ZERO;
        }
        return member.getLockedAmount();
    }

    /**
     * AC-INC-03/04：月度多项目叠加 + 2 倍封顶
     * 实发 = MIN(Σ(项目津贴), 2 × 基准额)
     * 基准额取当月最高 lockedAmount（即最高级别项目的锁定额）
     */
    public BigDecimal calculateMonthlyAllowance(Long personId, String month) {
        List<ProjectMember> members = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getPersonId, personId)
                .isNull(ProjectMember::getExitDate)); // 排除已退出
        if (members == null || members.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (ProjectMember m : members) {
            BigDecimal amt = calculateProjectAllowance(m);
            sum = sum.add(amt);
            if (amt.compareTo(base) > 0) {
                base = amt;
            }
        }
        BigDecimal cap = base.multiply(DEFAULT_CAP_MULTIPLIER);
        return sum.min(cap);
    }

    /**
     * AC-INC-04/03：构造台账（不写库）
     */
    public AllowanceLedger buildLedger(Long personId, String month, BigDecimal finalAmount,
                                       String lockedLevel, BigDecimal baseAmount, boolean capApplied) {
        return AllowanceLedger.builder()
            .personId(personId)
            .month(month)
            .finalAmount(finalAmount)
            .lockedLevel(lockedLevel)
            .baseAmount(baseAmount)
            .capApplied(capApplied ? "1" : "0")
            .build();
    }

    /**
     * 幂等：personId + projectId + month 唯一
     * 已存在则跳过（不重复台账）
     */
    @Transactional(rollbackFor = Exception.class)
    public AllowanceLedger recordOrSkip(AllowanceLedger ledger, Long projectId) {
        Long existing = allowanceLedgerMapper.selectCount(
            new LambdaQueryWrapper<AllowanceLedger>()
                .eq(AllowanceLedger::getPersonId, ledger.getPersonId())
                .eq(AllowanceLedger::getProjectId, projectId)
                .eq(AllowanceLedger::getMonth, ledger.getMonth()));
        if (existing != null && existing > 0) {
            return null; // 幂等：跳过
        }
        ledger.setProjectId(projectId);
        allowanceLedgerMapper.insert(ledger);
        return ledger;
    }

    /**
     * AC-INC-05/07/08：判定当月停发原因（兼容旧契约）
     */
    public String determineStopReason(BigDecimal comprehensiveScore, boolean noOutput60Days,
                                      boolean isMainProject) {
        if (isMainProject && noOutput60Days) {
            return null;
        }
        if (comprehensiveScore != null && comprehensiveScore.compareTo(SCORE_STOP_THRESHOLD) < 0) {
            return "SCORE_BELOW_60";
        }
        if (noOutput60Days) {
            return "NO_OUTPUT_60_DAYS";
        }
        return null;
    }

    /**
     * P3-3.2 AC-INC-05：绩效综合得分 < 60 当月停发。
     */
    public String determineLowScoreStop(BigDecimal comprehensiveScore) {
        if (comprehensiveScore == null) {
            return null;
        }
        if (comprehensiveScore.compareTo(SCORE_STOP_THRESHOLD) < 0) {
            return "STOP_SCORE_BELOW_60";
        }
        return null;
    }

    /**
     * P3-3.2 AC-INC-07/08：附加项目连续 60 天无产出判定。
     */
    public String determineNoOutput60DaysStop(Date lastActivityDate, Date asOfDate,
                                              boolean isAdditionalProject) {
        if (!isAdditionalProject) {
            return null;
        }
        if (asOfDate == null) {
            return null;
        }
        if (lastActivityDate == null) {
            return "STOP_NO_OUTPUT_60_DAYS";
        }
        long diffMs = asOfDate.getTime() - lastActivityDate.getTime();
        long days = diffMs / (24L * 60 * 60 * 1000);
        if (days >= NO_OUTPUT_DAYS_THRESHOLD) {
            return "STOP_NO_OUTPUT_60_DAYS";
        }
        return null;
    }

    /**
     * P3-3.2：判定停发原因（综合绩效分 + 无产出 + 主/附加）三层合一。
     */
    public String determineStopReasonP332(BigDecimal comprehensiveScore,
                                          Date lastActivityDate, Date asOfDate,
                                          boolean isAdditionalProject) {
        if (!isAdditionalProject && lastActivityDate == null) {
            return null;
        }
        String scoreStop = determineLowScoreStop(comprehensiveScore);
        if (scoreStop != null) {
            return scoreStop;
        }
        return determineNoOutput60DaysStop(lastActivityDate, asOfDate, isAdditionalProject);
    }

    /**
     * P3-3.3 AC-INC：津贴台账幂等检查——(personId, projectId, month) 唯一
     */
    public boolean existsByKey(Long personId, Long projectId, String month) {
        Long cnt = allowanceLedgerMapper.selectCount(
            new LambdaQueryWrapper<AllowanceLedger>()
                .eq(AllowanceLedger::getPersonId, personId)
                .eq(AllowanceLedger::getProjectId, projectId)
                .eq(AllowanceLedger::getMonth, month));
        return cnt != null && cnt > 0;
    }

    /**
     * P3-3.3 AC-INC：月中移交当月按月初人员判定。
     */
    public boolean isMemberEffectiveInMonth(Date joinDate, String month) {
        if (joinDate == null) {
            return false;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String joinStr = sdf.format(joinDate);
        String monthStart = month + "-01";
        return joinStr.compareTo(monthStart) <= 0;
    }

    /**
     * P3-3.3 AC-INC：退出次月停发。
     * <pre>
     *   exitDate == null            ⇒ active（未退出）
     *   exitDate's month >= month    ⇒ active（当月退或未退）
     *   exitDate's month &lt; month   ⇒ not active（次月起停）
     * </pre>
     */
    public boolean isMemberActiveInMonth(Date exitDate, String month) {
        if (exitDate == null) {
            return true;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String exitStr = sdf.format(exitDate);
        String monthStart = month + "-01";
        // 退出 ≥ 该月月初 ⇒ 该月仍 active
        return exitStr.compareTo(monthStart) >= 0;
    }

    /**
     * P3-3.3 AC-INC：月中移交 + 退出月份归属综合判定。
     */
    public boolean isMemberActiveForMonth(ProjectMember member, String month) {
        if (member == null) {
            return false;
        }
        if (member.getExitDate() != null) {
            return isMemberActiveInMonth(member.getExitDate(), month);
        }
        return isMemberEffectiveInMonth(member.getJoinDate(), month);
    }

    /**
     * P3-3.3 AC-INC：人员-项目-月复合主键冲突检测（并发重算护栏）
     */
    @Transactional(rollbackFor = Exception.class)
    public AllowanceLedger idempotentInsert(AllowanceLedger ledger) {
        if (ledger == null) {
            throw new IpdBusinessException("津贴草稿不能为空");
        }
        if (existsByKey(ledger.getPersonId(), ledger.getProjectId(), ledger.getMonth())) {
            return null;
        }
        allowanceLedgerMapper.insert(ledger);
        return ledger;
    }

    public List<AllowanceLedger> listByPerson(Long personId, String month) {
        return allowanceLedgerMapper.selectList(
            new LambdaQueryWrapper<AllowanceLedger>()
                .eq(AllowanceLedger::getPersonId, personId)
                .eq(AllowanceLedger::getMonth, month));
    }
}
