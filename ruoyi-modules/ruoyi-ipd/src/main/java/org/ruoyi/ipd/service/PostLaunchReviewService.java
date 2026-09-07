package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.PostLaunchReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.PostLaunchReviewMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * G5 上市 90 天复盘待办服务（AC-GATE-13 / AC-GATE-28；P2-5.6）。
 *
 * <p>口径：
 * <ul>
 *   <li>scheduleReview：launchDate 入参 ⇒ 生成 scheduled_at = launchDate + 90d 的 PENDING；
 *       assigneeId 取项目当前主 MARKET_PM（移交后跟随 ProjectMember）；同 projectId 已有 PENDING ⇒ 复用不创建</li>
 *   <li>completeReview：填入复盘数据 + status=COMPLETED + completedAt=now；写审计 POST_LAUNCH_REVIEW_COMPLETED</li>
 *   <li>复用项目成员：assigneeId 由 ProjectMember 在任 MARKET_PM 反查（移交后接续到新 PM）</li>
 *   <li>幂等：同 projectId 已有 PENDING ⇒ 直接返回旧记录（多触发源兼容）</li>
 *   <li>删除走 DeletionRequestService 软删除（暂未启用）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PostLaunchReviewService {

    /** 90 天复盘窗口。 */
    private static final long REVIEW_WINDOW_DAYS = 90L;

    private static final String ST_PENDING = "PENDING";
    private static final String ST_COMPLETED = "COMPLETED";

    private final PostLaunchReviewMapper postLaunchReviewMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final AuditLogService auditLogService;

    /** 复盘数据 record（与 PostLaunchReview 字段对齐，避免 DTO 爆炸）。 */
    public record ReviewData(BigDecimal actualRevenue, String customerFeedback,
                             String kpiAchievement, String lessons) { }

    /**
     * AC-GATE-13：G5 通过后生成 90 天复盘待办。
     *
     * @param projectId  项目
     * @param launchDate G5 通过日期（也是基准 +90d）
     * @param operator   操作人（仅审计）
     * @return 待办记录（PENDING 或复用旧 PENDING）
     */
    @Transactional(rollbackFor = Exception.class)
    public PostLaunchReview scheduleReview(Long projectId, Date launchDate, IpdActor operator) {
        if (projectId == null || launchDate == null) {
            throw new ServiceException("项目与上市日期不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        // 幂等：同 projectId 已有 PENDING ⇒ 复用
        List<PostLaunchReview> existing = postLaunchReviewMapper.selectList(
            new LambdaQueryWrapper<PostLaunchReview>()
                .eq(PostLaunchReview::getProjectId, projectId)
                .eq(PostLaunchReview::getStatus, ST_PENDING));
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }
        // assigneeId：项目主组当前在任 MARKET_PM；移交后由 ProjectMember 取新 PM
        Long assigneeId = pickMarketPm(projectId);
        if (assigneeId == null) {
            // 项目无 MARKET_PM 在任绑定：fallback 用项目 createBy；仍 null 则保留为 null（PM 接手后系统重派）
            assigneeId = project.getCreateBy();
        }
        Date scheduledAt = new Date(launchDate.getTime() + REVIEW_WINDOW_DAYS * 86_400_000L);
        PostLaunchReview r = PostLaunchReview.builder()
            .projectId(projectId)
            .scheduledAt(scheduledAt)
            .status(ST_PENDING)
            .assigneeId(assigneeId)
            .build();
        r.setTenantId("000000");
        r.setCreateTime(new Date());
        postLaunchReviewMapper.insert(r);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("POST_LAUNCH_REVIEW_SCHEDULED").entityType("post_launch_reviews").entityId(r.getId())
            .reason("projectId=" + projectId + " scheduledAt=" + scheduledAt)
            .afterData("{\"scheduledAt\":\"" + scheduledAt + "\",\"assigneeId\":\"" + assigneeId + "\"}")
            .createTime(new Date())
            .build());
        return r;
    }

    /**
     * AC-GATE-28：复盘完成 + 写 KPI / 奖金池联动占位（实际联动走 KpiRecordService / BonusPoolService，
     * 本卡只完成 status=COMPLETED + 审计 + 完成时间；联动逻辑 KpiSharedConfirm 卡已合，本服务只落审计入口）。
     */
    @Transactional(rollbackFor = Exception.class)
    public PostLaunchReview completeReview(Long reviewId, ReviewData data, IpdActor operator) {
        if (reviewId == null) {
            throw new ServiceException("复盘记录 ID 不能为空");
        }
        PostLaunchReview r = postLaunchReviewMapper.selectById(reviewId);
        if (r == null || "1".equals(r.getDelFlag())) {
            throw new ServiceException("复盘记录不存在: " + reviewId);
        }
        if (ST_COMPLETED.equals(r.getStatus())) {
            throw new ServiceException("复盘已完成（COMPLETED），不可重复完成");
        }
        r.setActualRevenue(data == null ? null : data.actualRevenue());
        r.setCustomerFeedback(data == null ? null : data.customerFeedback());
        r.setKpiAchievement(data == null ? null : data.kpiAchievement());
        r.setLessons(data == null ? null : data.lessons());
        r.setStatus(ST_COMPLETED);
        r.setCompletedAt(new Date());
        postLaunchReviewMapper.updateById(r);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id()).operatorName(operator.name()).operatorRole(operator.role())
            .action("POST_LAUNCH_REVIEW_COMPLETED").entityType("post_launch_reviews").entityId(r.getId())
            .reason("projectId=" + r.getProjectId())
            .afterData("{\"actualRevenue\":\"" + (data == null ? "" : data.actualRevenue())
                + "\",\"customerFeedbackLen\":\"" + (data == null || data.customerFeedback() == null ? 0 : data.customerFeedback().length())
                + "\"}")
            .createTime(new Date())
            .build());
        return r;
    }

    /**
     * 取项目当前在任 MARKET_PM（移交后接续新 PM）；不存在返回 null。
     */
    private Long pickMarketPm(Long projectId) {
        ProjectMember m = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getRole, "MARKET_PM")
            .isNull(ProjectMember::getExitDate)
            .orderByAsc(ProjectMember::getId)
            .last("LIMIT 1"));
        if (m != null) {
            return m.getPersonId();
        }
        return null;
    }
}