package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * P2-4.1 成员绑定时评级和津贴基准快照（BR-INC-02）。
 *
 * <p>锁定规则：lockedLevel/lockedAmount 随绑定原子写入（绑定时 person.level 快照 +
 * system_configs allowance.L1..L5 激活值），此后 HR 等级更新不追溯已绑定项目
 * （AC-HR-03 / AC-INC-02：L2 接手期间升 L3 ⇒ 该项目全程按 L2=1500 发放）。
 * <ul>
 *   <li>AC-TEAM-10：角色互斥（B7 固定不可跨）——personType 必须与绑定角色一致</li>
 *   <li>重试不多成员：同项目+同人+同角色且未退出的重复绑定拒绝，不产生第二条</li>
 *   <li>审计可追溯：MEMBER_BIND 写快照值入 afterData</li>
 * </ul>
 *
 * <p>P2-4.2 超额备案审核（AC-TEAM-11；叠加于 P2-4.1 交付之上）：
 * 该 PM 活跃绑定数达 threshold-1（缺省即第 3 个）时必须携带评级委员会审批备案编号
 * （approvalRef），否则禁止绑定；达 threshold（第 4 个）一律拒绝。
 * 首个活跃绑定=PRIMARY（主项目），其余=ADDITIONAL（附加项目）。
 * 并发不突破：计数查询带 FOR UPDATE（person_id 索引 next-key lock 串行化并发绑定）。
 */
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private static final Set<String> ROLES = Set.of("MARKET_PM", "RD_PM");

    /** P2-4.2：项目数备案阈值配置键（种子=3，A4；<=0 视为配置异常回退 3，不吞配置语义之外的 0） */
    private static final String KEY_PROJECT_COUNT_THRESHOLD = "allowance.projectCountThreshold";
    private static final int DEFAULT_PROJECT_COUNT_THRESHOLD = 3;

    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final ProjectMapper projectMapper;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;

    /** 绑定成员（P2-4.1 四参兼容入口，未携带备案编号）：等价于 approvalRef=null 的五参重载。 */
    @Transactional(rollbackFor = Exception.class)
    public ProjectMember bindMember(Long projectId, Long personId, String role, IpdActor operator) {
        return bindMember(projectId, personId, role, null, operator);
    }

    /**
     * 绑定成员（P2-4.2 完整入口）：评级快照 + 津贴基准锁定 + 超额备案审核。
     * approvalRef：绑第 threshold 个项目时必填（评级委员会审批备案编号）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectMember bindMember(Long projectId, Long personId, String role, String approvalRef, IpdActor operator) {
        if (!ROLES.contains(role)) {
            throw new ServiceException("角色非法: " + role);
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        Person person = personMapper.selectById(personId);
        if (person == null) {
            throw new ServiceException("人员不存在: " + personId);
        }
        if ("RESIGNED".equals(person.getEmploymentStatus()) || "DISABLED".equals(person.getAccountStatus())) {
            throw new ServiceException("离职/禁用人员不可入组: " + person.getName());
        }
        // AC-TEAM-10（B7 角色固定不可跨）：市场PM 不能被绑为研发PM，反之亦然
        if (!role.equals(person.getPersonType())) {
            throw new ServiceException("角色互斥：人员类型 " + person.getPersonType() + " 不可绑定为 " + role);
        }
        Long existing = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getPersonId, personId)
            .eq(ProjectMember::getRole, role)
            .isNull(ProjectMember::getExitDate));
        if (existing != null && existing > 0) {
            throw new ServiceException("该成员已绑定此角色，重试不产生重复成员");
        }
        // P2-4.2 超额备案审核（AC-TEAM-11）：FOR UPDATE 锁 person_id 索引，并发绑定在此串行化
        Long activeCount = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getPersonId, personId)
            .isNull(ProjectMember::getExitDate)
            .last("FOR UPDATE"));
        long active = activeCount == null ? 0L : activeCount;
        int threshold = systemConfigService.getIntValue(KEY_PROJECT_COUNT_THRESHOLD, DEFAULT_PROJECT_COUNT_THRESHOLD);
        if (threshold <= 0) {
            threshold = DEFAULT_PROJECT_COUNT_THRESHOLD;
        }
        if (active >= threshold) {
            throw new ServiceException("已达项目数上限 " + threshold + "，禁止再绑定（备案也不能超过上限）");
        }
        if (active == threshold - 1 && (approvalRef == null || approvalRef.isBlank())) {
            throw new ServiceException(
                "绑定第 " + threshold + " 个项目需先录入评级委员会审批备案记录（approvalRef），未录入禁止绑定");
        }
        if (active == threshold - 1 && approvalRef.length() > 64) {
            throw new ServiceException("备案编号超长（上限64，对齐 approval_ref 列宽）");
        }
        String memberType = active == 0L ? "PRIMARY" : "ADDITIONAL";
        String level = person.getLevel();
        if (level == null || level.isBlank()) {
            throw new ServiceException("该人员等级未同步（L1-L5），无法锁定津贴基准");
        }
        int amount = systemConfigService.getIntValue("allowance." + level, -1);
        if (amount <= 0) {
            throw new ServiceException("津贴参数缺失: allowance." + level);
        }
        ProjectMember member = ProjectMember.builder()
            .projectId(projectId)
            .personId(personId)
            .role(role)
            .memberType(memberType)
            .approvalRef(active == threshold - 1 ? approvalRef.trim() : null)
            .lockedLevel(level)
            .lockedAmount(BigDecimal.valueOf(amount))
            .joinDate(new Date())
            .bonusEligible("1")
            .build();
        memberMapper.insert(member);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id())
            .operatorName(operator.name())
            .action("MEMBER_BIND")
            .entityType("project_members")
            .entityId(member.getId())
            .reason("projectId=" + projectId + " role=" + role)
            .afterData(AuditEventData.json(
                "lockedLevel", level, "lockedAmount", BigDecimal.valueOf(amount),
                "memberType", memberType,
                "approvalRef", active == threshold - 1 ? approvalRef.trim() : null,
                "activeCountBefore", active))
            .createTime(new Date())
            .build());
        return member;
    }

    /**
     * P2-7.1 移交退出：原成员该绑定原子置退出（exit_reason=HANDOVER）。
     *
     * @return 影响行数；0 = 该绑定不存在或已退出（由调用方决定是否中止移交）
     */
    public int exitForHandover(Long projectId, Long personId, String role) {
        return memberMapper.update(null, new LambdaUpdateWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getPersonId, personId)
            .eq(ProjectMember::getRole, role)
            .isNull(ProjectMember::getExitDate)
            .set(ProjectMember::getExitDate, new Date())
            .set(ProjectMember::getExitReason, "HANDOVER"));
    }

    /** 在组成员列表（未退出），供验收 / 前端 / P3 津贴台账取数。 */
    public List<ProjectMember> listActiveMembers(Long projectId) {
        return memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .isNull(ProjectMember::getExitDate)
            .orderByAsc(ProjectMember::getId));
    }
}
