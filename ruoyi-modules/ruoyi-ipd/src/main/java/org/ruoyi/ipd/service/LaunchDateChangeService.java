package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdIdorGuard;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;

/**
 * AC-INC-33：修改上市日期需双签 + 审计；不可单方面改 {@code projects.launch_date}。
 * Round 8 / R8X-2 P0-1：propose / secondDecision 入口加 actor.groupId == project.mainGroupId
 * 横向越权防护（除 SUPER_ADMIN 豁免外，所有双签人必须归属同一项目主组）。
 *
 * <p>PERF-P1-6 框架（2026-09-07）：所有写方法统一 {@code @Transactional(rollbackFor = Exception.class)}
 * 类级默认值；{@link #propose}/{@link #secondDecision}/{@link #initialRecord} 在同一事务内完成业务写入，
 * {@link AuditLogService#append} 走 {@code REQUIRES_NEW} 与业务回滚解耦——审计完整性 vs 性能 trade-off。
 * 当前 {@link LaunchDateChangeRequest} 已带 {@code @Version} 乐观锁（{@code secondDecision} 用
 * {@code updateById==0} 判定并发失败、阻止重复审计写入，幂等性已闭合）。4 SQL → 1 批插入目标需
 * 引入 {@code AppendAuditBatchUtil}（见 docs/ipd-系统说明/治理/ 待办），当前提交只做事务边界同质化收敛。
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LaunchDateChangeService {

    public static final String ACTION_PROPOSE = "LAUNCH_DATE_PROPOSE";
    public static final String ACTION_CONFIRM = "LAUNCH_DATE_CONFIRM";
    public static final String ACTION_REJECT = "LAUNCH_DATE_REJECT";

    private static final Set<String> PROPOSER_ROLES = Set.of("MARKET_PM", "RD_PM", "SUPER_ADMIN");
    /**
     * 第二签角色白名单。不等于 {@link #PROPOSER_ROLES} 的隐含前提：
     * {@code IpdPermission.INTERNAL_ROLES} 还含 GROUP_LEADER（IpdPermission:75），
     * 不加白名单则组长可在同组内替他方补第二签。
     */
    private static final Set<String> CONFIRMER_ROLES = Set.of("MARKET_PM", "RD_PM", "SUPER_ADMIN");
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final LaunchDateChangeRequestMapper requestMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    /**
     * 提议变更上市日期（第一签）。
     * <p>R11 / A1 修复（WB-17-1 收口后死路）:提议时预落 confirmer_id/confirmer_role/confirmer_group_id，
     * 使 StrategicChangeAggregator 能正常投递 LD-卡（真活恒空死路）。与 Gate 仲裁方案 1 同构——
     * propose 时刻已知第二签人（提议人选择/系统互补角色推导），即落库而非 confirm 时回填。
     *
     * @param projectId         项目
     * @param proposedDate      新上市日
     * @param reason            理由
     * @param proposerId        提议人
     * @param proposerRole      角色
     * @param proposerGroupId   提议人所属产品组（横向越权防护用）
     * @param confirmerId       第二签确认人（提议时由前端选定，必传）
     * @param confirmerRole     第二签角色
     * @param confirmerGroupId  第二签人所属产品组（必传，与项目主组同）
     * @return PENDING_SECOND 申请（confirmer 字段已落库）
     */
    @Transactional(rollbackFor = Exception.class)
    public LaunchDateChangeRequest propose(Long projectId, Date proposedDate, String reason,
                                           Long proposerId, String proposerRole, Long proposerGroupId,
                                           Long confirmerId, String confirmerRole, Long confirmerGroupId) {
        if (projectId == null || proposedDate == null || proposerId == null) {
            throw new ServiceException("项目、上市日期与提议人不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("修改上市日期必须填写理由（AC-INC-33）");
        }
        if (proposerRole == null || !PROPOSER_ROLES.contains(proposerRole)) {
            throw new ServiceException("仅市场PM/研发PM/超管可提议上市日期变更");
        }
        Project project = requireWritableProject(projectId);
        // R8X-2 P0-1：横向越权防护——提议人必须归属同一项目主组（SUPER_ADMIN 豁免）
        IpdIdorGuard.assertSameGroupIpd(new IpdActor(proposerId, null, proposerRole, proposerGroupId),
            project.getMainGroupId());
        // R11 / A1 修复:提议时即选第二签人，fail-closed（不能后补、不能默认）
        if (confirmerId == null || confirmerRole == null || confirmerGroupId == null) {
            throw new ServiceException("提议上市日期变更必须同时指定第二签确认人（防工作台 LD-卡恒空）");
        }
        if (confirmerId.equals(proposerId)) {
            throw new ServiceException("双签须由不同人员完成（AC-INC-33）");
        }
        if (!CONFIRMER_ROLES.contains(confirmerRole)) {
            throw new ServiceException("仅市场PM/研发PM/超管可完成上市日期变更第二签");
        }
        IpdIdorGuard.assertSameGroupIpd(new IpdActor(confirmerId, null, confirmerRole, confirmerGroupId),
            project.getMainGroupId());
        if (!SUPER_ADMIN.equals(confirmerRole) && !SUPER_ADMIN.equals(proposerRole)
            && confirmerRole.equals(proposerRole)) {
            throw new ServiceException("第二签须为互补角色（市场PM↔研发PM）或超管");
        }
        Long pending = requestMapper.selectCount(new LambdaQueryWrapper<LaunchDateChangeRequest>()
            .eq(LaunchDateChangeRequest::getProjectId, projectId)
            .eq(LaunchDateChangeRequest::getStatus, LaunchDateChangeRequest.ST_PENDING_SECOND));
        if (pending != null && pending > 0) {
            throw new ServiceException("该项目已有待第二签确认的上市日期变更申请");
        }
        LaunchDateChangeRequest req = LaunchDateChangeRequest.builder()
            .projectId(projectId)
            .proposedLaunchDate(proposedDate)
            .previousLaunchDate(project.getLaunchDate())
            .reason(reason.trim())
            .proposerId(proposerId)
            .proposerRole(proposerRole)
            .confirmerId(confirmerId)
            .confirmerRole(confirmerRole)
            .status(LaunchDateChangeRequest.ST_PENDING_SECOND)
            .tenantId("000000")
            .delFlag("0")
            .build();
        req.setCreateTime(new Date());
        try {
            requestMapper.insert(req);
        } catch (DuplicateKeyException ex) {
            // P1（owner 2026-09-05 项1a）：上面的 selectCount 预检与 insert 不在同一原子临界区，
            // 并发提议会双双通过预检 → 同一项目出现两条 PENDING_SECOND。
            // DB 侧兜底：uk_ldcr_pending_project（“部分唯一索引”的 MySQL 生成列等价实现，见
            // docs/script/sql/update/2026-09-05-ipd-launch-date-pending-unique.sql）。
            // 预检保留：为了让正常串行路径仍返回可读的业务错而非依赖异常；两者必须同文案。
            throw new ServiceException("该项目已有待第二签确认的上市日期变更申请");
        }
        audit(proposerId, ACTION_PROPOSE, req.getId(),
            "project:" + projectId + " date:" + proposedDate + " " + reason.trim());
        return req;
    }

    /**
     * 第二签确认或驳回；确认后写回项目上市日期。
     *
     * @param requestId       申请
     * @param confirmerId     确认人（须异于提议人）
     * @param confirmerRole   确认人角色
     * @param confirmerGroupId 确认人所属产品组（横向越权防护用）
     * @param approve         是否通过
     * @param opinion         意见
     * @return 终态申请
     */
    @Transactional(rollbackFor = Exception.class)
    public LaunchDateChangeRequest secondDecision(Long requestId, Long confirmerId, String confirmerRole,
                                                  Long confirmerGroupId, boolean approve, String opinion) {
        if (confirmerId == null) {
            throw new ServiceException("第二签确认人不能为空");
        }
        LaunchDateChangeRequest req = requestMapper.selectById(requestId);
        if (req == null) {
            throw new ServiceException("上市日期变更申请不存在: " + requestId);
        }
        if (!LaunchDateChangeRequest.ST_PENDING_SECOND.equals(req.getStatus())) {
            throw new ServiceException("状态机不匹配：期望 PENDING_SECOND，实际 " + req.getStatus());
        }
        if (confirmerId.equals(req.getProposerId())) {
            throw new ServiceException("双签须由不同人员完成（AC-INC-33）");
        }
        // R11 / A1 修复:必须匹配 propose 时刻预落的 confirmer_id（防提议人绕过预落自己确认）
        if (req.getConfirmerId() != null && !confirmerId.equals(req.getConfirmerId())) {
            throw new ServiceException("第二签人必须为提议时指定的确认人（confirmerId 预落校验）");
        }
        // P1（owner 2026-09-05 项1b）：第二签角色白名单，fail-closed。
        // 旧实现只在「两侧都非超管且角色相同」时拒绝，且因 `confirmerRole != null &&` 短路，
        // confirmerRole==null 时整条判定不成立而直接放行；GROUP_LEADER 这类不在声明集内的
        // 内部角色也能补签——抛错文案写的「市场PM↔研发PM 或超管」与实际执法不一致。
        if (confirmerRole == null || !CONFIRMER_ROLES.contains(confirmerRole)) {
            throw new ServiceException("仅市场PM/研发PM/超管可完成上市日期变更第二签");
        }
        // R8X-2 P0-1：横向越权防护——确认人必须归属同一项目主组（SUPER_ADMIN 豁免）
        Project project = requireWritableProject(req.getProjectId());
        IpdIdorGuard.assertSameGroupIpd(new IpdActor(confirmerId, null, confirmerRole, confirmerGroupId),
            project.getMainGroupId());
        if (!SUPER_ADMIN.equals(confirmerRole) && !SUPER_ADMIN.equals(req.getProposerRole())
            && confirmerRole.equals(req.getProposerRole())) {
            throw new ServiceException("第二签须为互补角色（市场PM↔研发PM）或超管");
        }
        String finalStatus = approve ? LaunchDateChangeRequest.ST_CONFIRMED : LaunchDateChangeRequest.ST_REJECTED;
        req.setStatus(finalStatus);
        req.setConfirmerId(confirmerId);
        req.setConfirmerRole(confirmerRole);
        req.setConfirmedAt(new Date());
        req.setDecision(approve ? "APPROVE" : "REJECT");
        req.setOpinion(opinion);
        // P1（owner 2026-09-05 项1b）：状态迁移的归属权由 @Version 乐观锁担保（同 P1-4.3 / R8-P0-9 惯例）。
        // 旧实现丢弃 updateById 返回值：两个确认人并发时都读到 PENDING_SECOND，双方都写成功
        // → 同一申请留下两条 ACTION_CONFIRM 审计且 confirmer 字段被后写者覆盖，第二签到底是谁做的已不可追溯。
        // 硬前置：未 apply docs/script/sql/update/2026-09-05-ipd-launch-date-pending-unique.sql 的 version 列前，
        // selectById 读不到 version → 拦截器跳过版本号条件，本判定退化为恒不命中（仅保留旧行为，不会误拒）。
        if (requestMapper.updateById(req) == 0) {
            throw new ServiceException("该上市日期变更申请已被并发处理，本次第二签未生效");
        }
        if (!approve) {
            audit(confirmerId, ACTION_REJECT, req.getId(), opinion);
            return req;
        }
        project.setLaunchDate(req.getProposedLaunchDate());
        projectMapper.updateById(project);
        audit(confirmerId, ACTION_CONFIRM, req.getId(),
            "project:" + project.getId() + " launchDate:" + req.getProposedLaunchDate());
        return req;
    }

    /**
     * R8-AUTO-5:删除 dead code `assertNoDirectLaunchDateMutation`（永远抛异常无 caller）。
     * 注意：当时注释声称「项目创建 launchDate 写入由后续 P0-2 (ProjectService groupId 校验) 兜底」
     * ——该声明不成立：groupId 校验只管横向越权，不覆盖 launch_date 写入语义。真实守住此不变量的
     * 只有两处：①本类 secondDecision（双签后写回）；②ProjectCreateReq.toEntity → ProjectService.create
     * （仅 INSERT，属 BR-IPD-08「L08 录入」路径，不算修改）。已改由 LaunchDateDualSignGuardAcceptanceTest
     * 的静态守卫用例钉住，不得再靠注释口头兜底（owner 2026-09-05 项1c）。
     */

    /**
     * P1 / §5.1：L08 上市日期初次录入（独立端点）—— 状态仅 DRAFT|CONFIRMED 可调；
     * 写 INITIAL_LAUNCH_DATE 审计。launch_date 已有值（曾录入过）则拒绝（走双签修改流程）。
     *
     * @param projectId 项目
     * @param date      录入的上市日期
     * @param reason    理由
     * @param operatorId 操作人
     */
    @Transactional(rollbackFor = Exception.class)
    public Project initialRecord(Long projectId, Date date, String reason, Long operatorId) {
        if (projectId == null || date == null) {
            throw new ServiceException("项目与上市日期不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("初次录入上市日期必须填写理由");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        if (!"DRAFT".equals(project.getStatus()) && !"CONFIRMED".equals(project.getStatus())
            && !"TEAMING".equals(project.getStatus()) && !"ACTIVE".equals(project.getStatus())) {
            throw new ServiceException("DRAFT/CONFIRMED/TEAMING/ACTIVE 状态可初次录入上市日期");
        }
        if (project.getLaunchDate() != null) {
            throw new ServiceException("上市日期已存在；修改请走双签流程（POST /launch-date-change-requests）");
        }
        project.setLaunchDate(date);
        projectMapper.updateById(project);
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId)
            .action("INITIAL_LAUNCH_DATE")
            .entityType("projects")
            .entityId(projectId)
            .reason(reason.trim())
            .afterData("{\"launchDate\":\"" + date + "\"}")
            .createTime(new Date())
            .build());
        return project;
    }

    private Project requireWritableProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        if ("SUSPENDED".equals(project.getStatus()) || "ARCHIVED".equals(project.getStatus())) {
            throw new ServiceException("暂停/归档项目禁止修改上市日期");
        }
        return project;
    }

    private void audit(Long operatorId, String action, Long entityId, String reason) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action(action).entityType("launch_date_change_requests")
            .entityId(entityId).reason(reason).createTime(new Date()).build());
    }
}
