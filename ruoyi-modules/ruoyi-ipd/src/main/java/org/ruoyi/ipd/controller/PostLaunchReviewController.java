package org.ruoyi.ipd.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.PostLaunchReview;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdRolePermissionCatalog;
import org.ruoyi.ipd.service.PostLaunchReviewService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * G5 上市 90 天复盘（/api/v1/post-launch-reviews；页 47）。
 *
 * <p><b>R-NEW-ARCH-1 收口</b>：P2-5.6 只落了 {@link PostLaunchReviewService} 与 Mockito 验收，
 * 全仓除测试外无任何调用方，属"Service 已实现、API 层不可达"的半闭环；同时真库从未建表
 * （见 {@code docs/script/sql/update/2026-09-07-ipd-rnew-post-launch-reviews.sql}）。
 * 本 Controller 是该域第一个可达入口，因此权限码与守卫与建表脚本、{@code tenant.excludes} 同批落地。
 *
 * <p><b>鉴权模式（R-NEW-ARCH-2 声明）</b>：使用 {@code @SaCheckPermission} 注解 +
 * {@link IpdPermission#requireInternal()} 会话捕获的双层模式——
 * 注解按 {@link IpdRolePermissionCatalog} 登记的权限码把住"哪类内部角色可调"，
 * 对象级判定（该项目在职 MARKET_PM / 在职成员）由 {@code PostLaunchReviewService} 内
 * {@link org.ruoyi.ipd.security.IpdIdorGuard} 完成。<b>"注解粒度粗"不等于"未认证"</b>，
 * 新增端点时两级都不可省略。
 *
 * <p>路径口径：设计稿（卡片 P2-5.6）曾写 {@code POST /api/v1/gate/post-launch-review/{projectId}}；
 * 该路径从未上线、前端全仓无引用，这里改用与本仓其余 46 个 Controller 一致的
 * {@code /api/v1/<资源复数>} 风格，避免 Gate 前缀与 gate 域混淆。
 */
@RestController
@RequestMapping("/api/v1/post-launch-reviews")
@RequiredArgsConstructor
public class PostLaunchReviewController {

    private final PostLaunchReviewService postLaunchReviewService;
    private final IpdPermission permission;

    /**
     * 排期入参。
     *
     * <p>R8-P0-11 口径：日期用 {@link LocalDate} + {@code @JsonFormat("yyyy-MM-dd")}，
     * 在 Controller 层转 {@link Date} 传给 Service，避免 java.util.Date 反序列化时区漂移。
     */
    public record ScheduleRequest(
        @NotNull Long projectId,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate launchDate) { }

    /** 完成复盘入参（四项均为复盘正文，允许缺省；金额由前端按千分位解析后传数字）。 */
    public record CompleteRequest(BigDecimal actualRevenue, String customerFeedback,
                                  String kpiAchievement, String lessons) { }

    /** 复盘视图：ID 一律字符串化（IPD /api/v1 契约：前端不接 JSON number 的 19 位雪花 ID）。 */
    public record PostLaunchReviewView(String id, String projectId, String scheduledAt, String status,
                                       String assigneeId, BigDecimal actualRevenue, String customerFeedback,
                                       String kpiAchievement, String lessons, String completedAt) {
        public static PostLaunchReviewView from(PostLaunchReview r) {
            return new PostLaunchReviewView(
                r.getId() == null ? null : String.valueOf(r.getId()),
                r.getProjectId() == null ? null : String.valueOf(r.getProjectId()),
                r.getScheduledAt() == null ? null : r.getScheduledAt().toString(),
                r.getStatus(),
                r.getAssigneeId() == null ? null : String.valueOf(r.getAssigneeId()),
                r.getActualRevenue(), r.getCustomerFeedback(), r.getKpiAchievement(), r.getLessons(),
                r.getCompletedAt() == null ? null : r.getCompletedAt().toString());
        }
    }

    /** 页 47 入口：查项目当前 PENDING 复盘；无待办时由 service 抛业务异常（前端据此区分"无待办"）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_QUERY,
        type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/pending")
    public ApiV1Response<PostLaunchReviewView> pending(@RequestParam Long projectId) {
        IpdActor actor = permission.requireInternal();
        return ApiV1Response.ok(PostLaunchReviewView.from(
            postLaunchReviewService.findPendingByProject(projectId, actor)));
    }

    /** 生成/复用 90 天复盘待办（幂等：同项目已有 PENDING 直接返回旧记录）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_CREATE,
        type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<PostLaunchReviewView> schedule(@Valid @RequestBody ScheduleRequest request) {
        IpdActor actor = permission.requireInternal();
        Date launchDate = Date.from(
            request.launchDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        return ApiV1Response.ok(PostLaunchReviewView.from(postLaunchReviewService
            .scheduleReview(request.projectId(), launchDate, actor)));
    }

    /** 完成复盘（COMPLETED 终态，不可重复完成；对象级归属由 service 按记录真实 projectId 判定）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_COMPLETE,
        type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/complete")
    public ApiV1Response<PostLaunchReviewView> complete(@PathVariable Long id,
                                                        @RequestBody(required = false) CompleteRequest request) {
        IpdActor actor = permission.requireInternal();
        PostLaunchReviewService.ReviewData data = request == null ? null
            : new PostLaunchReviewService.ReviewData(request.actualRevenue(), request.customerFeedback(),
                request.kpiAchievement(), request.lessons());
        return ApiV1Response.ok(PostLaunchReviewView.from(
            postLaunchReviewService.completeReview(id, data, actor)));
    }
}
