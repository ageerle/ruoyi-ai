package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
 *   <li>幂等：(personId, projectId, month) 唯一，重复执行不重复台账</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AllowanceService {

    private final AllowanceLedgerMapper allowanceLedgerMapper;
    private final ProjectMemberMapper projectMemberMapper;

    /** AC-INC-03/04：默认 2 倍封顶 */
    private static final BigDecimal DEFAULT_CAP_MULTIPLIER = new BigDecimal("2");

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
     * AC-INC-05/07/08：判定当月停发原因
     * 60 边界；绩效 < 60 停发；附加项目连续 60 天无产出可触发停发
     */
    public String determineStopReason(BigDecimal comprehensiveScore, boolean noOutput60Days,
                                      boolean isMainProject) {
        // AC-INC-08：主项目无产出不触发停发
        if (isMainProject && noOutput60Days) {
            return null;
        }
        if (comprehensiveScore != null && comprehensiveScore.compareTo(new BigDecimal("60")) < 0) {
            return "SCORE_BELOW_60";
        }
        if (noOutput60Days) {
            return "NO_OUTPUT_60_DAYS";
        }
        return null;
    }

    public List<AllowanceLedger> listByPerson(Long personId, String month) {
        return allowanceLedgerMapper.selectList(
            new LambdaQueryWrapper<AllowanceLedger>()
                .eq(AllowanceLedger::getPersonId, personId)
                .eq(AllowanceLedger::getMonth, month));
    }
}
