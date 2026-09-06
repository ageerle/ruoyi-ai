package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;

/**
 * AC-INC-33：修改上市日期需双签 + 审计；不可单方面改 {@code projects.launch_date}。
 * Round 8 / R8X-2 P0-1：propose / secondDecision 入口加 actor.groupId == project.mainGroupId
 * 横向越权防护（除 SUPER_ADMIN 豁免外，所有双签人必须归属同一项目主组）。
 */
@Service
@RequiredArgsConstructor
public class LaunchDateChangeService {

    public static final String ACTION_PROPOSE = "LAUNCH_DATE_PROPOSE";
    public static final String ACTION_CONFIRM = "LAUNCH_DATE_CONFIRM";
    public static final String ACTION_REJECT = "LAUNCH_DATE_REJECT";

    private static final Set<String> PROPOSER_ROLES = Set.of("MARKET_PM", "RD_PM", "SUPER_ADMIN");
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final LaunchDateChangeRequestMapper requestMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    /**
     * 提议变更上市日期（第一签）。
     *
     * @param projectId      项目
     * @param proposedDate   新上市日
     * @param reason         理由
     * @param proposerId     提议人
     * @param proposerRole   角色
     * @param proposerGroupId 提议人所属产品组（横向越权防护用）
     * @return PENDING_SECOND 申请
     */
    @Transactional(rollbackFor = Exception.class)
    public LaunchDateChangeRequest propose(Long projectId, Date proposedDate, String reason,
                                           Long proposerId, String proposerRole, Long proposerGroupId) {
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
        assertSameGroup(proposerRole, proposerGroupId, project.getMainGroupId(), "提议人");
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
            .status(LaunchDateChangeRequest.ST_PENDING_SECOND)
            .tenantId("000000")
            .delFlag("0")
            .build();
        req.setCreateTime(new Date());
        requestMapper.insert(req);
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
        // R8X-2 P0-1：横向越权防护——确认人必须归属同一项目主组（SUPER_ADMIN 豁免）
        Project project = requireWritableProject(req.getProjectId());
        assertSameGroup(confirmerRole, confirmerGroupId, project.getMainGroupId(), "确认人");
        if (!SUPER_ADMIN.equals(confirmerRole) && !SUPER_ADMIN.equals(req.getProposerRole())
            && confirmerRole != null && confirmerRole.equals(req.getProposerRole())) {
            throw new ServiceException("第二签须为互补角色（市场PM↔研发PM）或超管");
        }
        req.setConfirmerId(confirmerId);
        req.setConfirmerRole(confirmerRole);
        req.setConfirmedAt(new Date());
        req.setDecision(approve ? "APPROVE" : "REJECT");
        req.setOpinion(opinion);
        if (!approve) {
            req.setStatus(LaunchDateChangeRequest.ST_REJECTED);
            requestMapper.updateById(req);
            audit(confirmerId, ACTION_REJECT, req.getId(), opinion);
            return req;
        }
        project.setLaunchDate(req.getProposedLaunchDate());
        projectMapper.updateById(project);
        req.setStatus(LaunchDateChangeRequest.ST_CONFIRMED);
        requestMapper.updateById(req);
        audit(confirmerId, ACTION_CONFIRM, req.getId(),
            "project:" + project.getId() + " launchDate:" + req.getProposedLaunchDate());
        return req;
    }

    /**
     * R8X-2 P0-1：组归属校验。SUPER_ADMIN 一律通过；其他角色必须 actor.groupId == objectGroupId。
     */
    private void assertSameGroup(String actorRole, Long actorGroupId, Long objectGroupId, String role) {
        if (SUPER_ADMIN.equals(actorRole)) {
            return;
        }
        if (actorGroupId == null || !actorGroupId.equals(objectGroupId)) {
            throw new ServiceException(role + "必须归属项目主组（横向越权防护）");
        }
    }

    /**
     * R8-AUTO-5:删除 dead code `assertNoDirectLaunchDateMutation`（永远抛异常无 caller）。
     * 后台安全审查 fail-open / control-regression 建议：要么删除（避免误用），要么改成真正守卫。
     * 本 commit 删除——项目创建 launchDate 写入由后续 P0-2 (ProjectService groupId 校验) 兜底。
     */

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
