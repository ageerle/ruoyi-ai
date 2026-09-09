package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.CoefficientChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.CoefficientChangeRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

/**
 * AC-INC-15c：S/B 级系数定值 = 双PM 联合提议 → 产品组长确认 → 写入项目档案。
 * <p>A 级固定 1.0，禁止走本流程；区间校验复用 {@link ProjectService} 规则文案。
 */
@Service
@RequiredArgsConstructor
public class CoefficientChangeService {

    public static final String ACTION_PROPOSE = "COEFFICIENT_PROPOSE";
    public static final String ACTION_CONFIRM = "COEFFICIENT_CONFIRM";
    public static final String ACTION_REJECT = "COEFFICIENT_REJECT";

    private final CoefficientChangeRequestMapper requestMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    /**
     * 双PM 联合提议（一次提交同时登记双方 ID）。
     * <p>R11 / A2 修复（WB-17-1 收口后死路）:提议时预落 leader_id，使 StrategicChangeAggregator
     * 能正常投递 CC-卡。与 Gate 仲裁方案 1 同构——双 PM 提议时已知组长人选，即落库。
     *
     * @param projectId   项目
     * @param coefficient 提议系数
     * @param reason      定值理由（必填）
     * @param marketPmId  市场PM
     * @param rdPmId      研发PM
     * @param proposerId  提交人
     * @param leaderId    产品组长（提议时由前端选定，必传）
     * @return 新建申请（PENDING_LEADER，leader_id 已落库）
     */
    @Transactional(rollbackFor = Exception.class)
    public CoefficientChangeRequest propose(Long projectId, BigDecimal coefficient, String reason,
                                            Long marketPmId, Long rdPmId, Long proposerId, Long leaderId) {
        if (projectId == null || coefficient == null || marketPmId == null || rdPmId == null || proposerId == null) {
            throw new ServiceException("项目、系数、双PM 与提交人不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("S/B 级系数定值理由必填（写审计）");
        }
        if (marketPmId.equals(rdPmId)) {
            throw new ServiceException("联合提议须由市场PM与研发PM两位不同人员");
        }
        // R11 / A2 修复:提议时即选组长，fail-closed
        if (leaderId == null) {
            throw new ServiceException("S/B 级系数定值必须指定产品组长（防工作台 CC-卡恒空）");
        }
        if (leaderId.equals(proposerId) || leaderId.equals(marketPmId) || leaderId.equals(rdPmId)) {
            throw new ServiceException("组长不可与提议人/双PM 同人（职责隔离）");
        }
        Project project = requireProject(projectId);
        String level = project.getLevel();
        if ("A".equals(level)) {
            throw new ServiceException("A 级为固定 1.0 不可改");
        }
        if (!"S".equals(level) && !"B".equals(level)) {
            throw new ServiceException("仅 S/B 级可走系数定值流程");
        }
        ProjectService.validateCoefficientRange(level, coefficient);
        Long pending = requestMapper.selectCount(new LambdaQueryWrapper<CoefficientChangeRequest>()
            .eq(CoefficientChangeRequest::getProjectId, projectId)
            .eq(CoefficientChangeRequest::getStatus, CoefficientChangeRequest.ST_PENDING_LEADER));
        if (pending != null && pending > 0) {
            throw new ServiceException("该项目已有待组长确认的系数定值申请");
        }
        CoefficientChangeRequest req = CoefficientChangeRequest.builder()
            .projectId(projectId)
            .proposedCoefficient(coefficient)
            .reason(reason.trim())
            .marketPmId(marketPmId)
            .rdPmId(rdPmId)
            .proposerId(proposerId)
            .leaderId(leaderId)
            .status(CoefficientChangeRequest.ST_PENDING_LEADER)
            .build();
        req.setCreateTime(new Date());
        requestMapper.insert(req);
        audit(proposerId, ACTION_PROPOSE, req.getId(),
            "project:" + projectId + " coef:" + coefficient + " " + reason.trim());
        return req;
    }

    /**
     * 产品组长确认或驳回；确认后写回项目档案。
     *
     * @param requestId 申请 ID
     * @param leaderId  组长
     * @param approve   true=确认写入；false=驳回
     * @param opinion   意见
     * @return 终态申请
     */
    @Transactional(rollbackFor = Exception.class)
    public CoefficientChangeRequest leaderDecision(Long requestId, Long leaderId, boolean approve, String opinion) {
        if (leaderId == null) {
            throw new ServiceException("组长不能为空");
        }
        CoefficientChangeRequest req = requestMapper.selectById(requestId);
        if (req == null) {
            throw new ServiceException("系数定值申请不存在: " + requestId);
        }
        if (!CoefficientChangeRequest.ST_PENDING_LEADER.equals(req.getStatus())) {
            throw new ServiceException("状态机不匹配：期望 PENDING_LEADER，实际 " + req.getStatus());
        }
        // R11 / A2 修复:必须匹配 propose 时刻预落的 leader_id（防提议人绕过预落自己确认）
        if (req.getLeaderId() != null && !leaderId.equals(req.getLeaderId())) {
            throw new ServiceException("组长人必须为提议时指定的组长（leaderId 预落校验）");
        }
        req.setLeaderId(leaderId);
        req.setLeaderDecision(approve ? "APPROVE" : "REJECT");
        req.setLeaderDecidedAt(new Date());
        req.setLeaderOpinion(opinion);
        if (!approve) {
            req.setStatus(CoefficientChangeRequest.ST_REJECTED);
            requestMapper.updateById(req);
            audit(leaderId, ACTION_REJECT, req.getId(), opinion);
            return req;
        }
        Project project = requireProject(req.getProjectId());
        ProjectService.validateCoefficientRange(project.getLevel(), req.getProposedCoefficient());
        project.setLevelCoefficient(req.getProposedCoefficient());
        project.setLevelCoefficientReason(req.getReason());
        projectMapper.updateById(project);
        req.setStatus(CoefficientChangeRequest.ST_CONFIRMED);
        requestMapper.updateById(req);
        audit(leaderId, ACTION_CONFIRM, req.getId(),
            "project:" + project.getId() + " coef:" + req.getProposedCoefficient());
        return req;
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        return project;
    }

    private void audit(Long operatorId, String action, Long entityId, String reason) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId)
            .action(action)
            .entityType("coefficient_change_requests")
            .entityId(entityId)
            .reason(reason)
            .createTime(new Date())
            .build());
    }
}
