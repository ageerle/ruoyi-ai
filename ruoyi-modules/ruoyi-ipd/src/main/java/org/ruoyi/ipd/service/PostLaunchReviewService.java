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
import org.ruoyi.ipd.security.IpdIdorGuard;
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
 *
 * <p><b>入口守卫（R-NEW-SEC-1 收口，2026-09-07）</b>：本服务三个公开写/读入口此前接受
 * {@link IpdActor} 却不做任何校验，任何 internal 角色拿到 projectId 即可排期/完成别人的复盘。
 * 现统一走 {@link IpdIdorGuard}，且角色校验先于任何 DB 读（fail-closed）：
 * <ul>
 *   <li>写入口（schedule/complete）：{@code MARKET_PM} 或 {@code SUPER_ADMIN} 角色门 + 该项目在职成员</li>
 *   <li>读入口（pending 查询）：该项目在职成员或 {@code SUPER_ADMIN}</li>
 * </ul>
 * 因此内部调度方（如未来 G5 通过后的自动排期）必须以项目主 MARKET_PM 或超管身份传入 actor，
 * 不能再传 GROUP_LEADER/RD_PM 视角的 actor——这是有意的权限收紧，不是回归。
 */
@Service
@RequiredArgsConstructor
public class PostLaunchReviewService {

    /** 90 天复盘窗口。 */
    private static final long REVIEW_WINDOW_DAYS = 90L;

    private static final String ST_PENDING = "PENDING";
    private static final String ST_COMPLETED = "COMPLETED";

    /**
     * 复盘写操作要求的业务角色（超管另由 {@link IpdIdorGuard} 豁免）。
     * 与 P2-5.6 卡片口径一致：复盘待办只归项目主 MARKET_PM 与其超管。
     */
    private static final String ROLE_MARKET_PM = "MARKET_PM";

    /**
     * 单企业私有部署（R8）下 tenant_id 的固定值。
     *
     * <p>此前该字面量直接写在 {@code scheduleReview} 里（R-NEW-ARCH-3 指出的散落魔法值）。
     * 提常量同时明确其语义：本表已登记 {@code tenant.excludes}，不参与多租户过滤，
     * 该列仅作数据归属占位，改动它不影响查询行为，但会让新建行与既有行取值不一致。
     */
    private static final String DEFAULT_TENANT_ID = "000000";

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
     * @param operator   操作人（审计 + 守卫主体，须为该项目在职 MARKET_PM 或超管）
     * @return 待办记录（PENDING 或复用旧 PENDING）
     */
    @Transactional(rollbackFor = Exception.class)
    public PostLaunchReview scheduleReview(Long projectId, Date launchDate, IpdActor operator) {
        // 守卫先于参数校验与任何 DB 读：角色不符的冒充者不应得知 projectId/launchDate 是否合法
        IpdIdorGuard.requireRoleOrSuperAdmin(operator, ROLE_MARKET_PM);
        if (projectId == null || launchDate == null) {
            throw new ServiceException("项目与上市日期不能为空");
        }
        IpdIdorGuard.requireProjectMemberOrSuperAdmin(operator, projectId, memberMapper, projectMapper);
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
        r.setTenantId(DEFAULT_TENANT_ID);
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
        // 守卫先于参数校验（与 scheduleReview 同口径，fail-closed）
        IpdIdorGuard.requireRoleOrSuperAdmin(operator, ROLE_MARKET_PM);
        if (reviewId == null) {
            throw new ServiceException("复盘记录 ID 不能为空");
        }
        PostLaunchReview r = postLaunchReviewMapper.selectById(reviewId);
        if (r == null || "1".equals(r.getDelFlag())) {
            throw new ServiceException("复盘记录不存在: " + reviewId);
        }
        // 对象级守卫：按记录真正所属项目判定，不接受调用方自报 projectId（防 IDOR）
        IpdIdorGuard.requireProjectMemberOrSuperAdmin(operator, r.getProjectId(), memberMapper, projectMapper);
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
     * 页 47 入口：取项目当前 PENDING 复盘（R-NEW-ARCH-1 随 Controller 一同补齐）。
     *
     * <p>读入口只要求“项目在职成员或超管”，不比写入口严（PM 交接期间接手人也要能看到待办）。
     *
     * @param projectId 项目
     * @param operator  服务端会话身份
     * @return PENDING 记录（按 id 升序取最早一条）
     * @throws ServiceException 项目无 PENDING 复盘
     */
    public PostLaunchReview findPendingByProject(Long projectId, IpdActor operator) {
        IpdIdorGuard.requireAuthenticated(operator);
        if (projectId == null) {
            throw new ServiceException("项目 ID 不能为空");
        }
        IpdIdorGuard.requireProjectMemberOrSuperAdmin(operator, projectId, memberMapper, projectMapper);
        List<PostLaunchReview> rows = postLaunchReviewMapper.selectList(
            new LambdaQueryWrapper<PostLaunchReview>()
                .eq(PostLaunchReview::getProjectId, projectId)
                .eq(PostLaunchReview::getStatus, ST_PENDING)
                .orderByAsc(PostLaunchReview::getId)
                .last("LIMIT 1"));
        if (rows == null || rows.isEmpty()) {
            throw new ServiceException("项目无待完成复盘: " + projectId);
        }
        return rows.get(0);
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