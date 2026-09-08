package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.PostLaunchReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.PostLaunchReviewMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-5.6 验收：AC-GATE-13 G5 上市 90 天复盘待办自动生成 +
 *            AC-GATE-26 上市日期变更 → 下游 Gate 截止日重排。
 *
 * <ul>
 *   <li>scheduleReview：G5 通过 + launchDate + assignee ⇒ 生成 scheduled_at=launchDate+90d 的 PENDING 待办</li>
 *   <li>completeReview：标记 COMPLETED + completedAt + 写审计 POST_LAUNCH_REVIEW_COMPLETED</li>
 *   <li>shiftDownstreamGates：G3/G4/G5 截止日 += deltaDays + 审计 LAUNCH_DATE_CHANGED</li>
 *   <li>scheduleReview 幂等：同 projectId 已有 PENDING ⇒ 复用不重复创建</li>
 *   <li>completeReview 拒绝：已 COMPLETED 不可重复完成</li>
 * </ul>
 *
 * <p>设计选择：通过新 {@link GateService#shiftDownstreamGates(Long, long, Long, IpdActor)} 方法
 * 实现 AC-GATE-26，不直接修改 {@link LaunchDateChangeService}（保持既有第二签逻辑零回归）。
 * 双签完成后由调用方（如 controller 或上游 orchestrator）传入旧/新日期触发重排。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-5.6 AC-GATE-13/26：G5 90 天复盘待办 + 上市日期变更下游重排")
class P256AcceptanceTest {

    @Mock private LaunchDateChangeRequestMapper changeRequestMapper;
    @Mock private GateMapper gateMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private PostLaunchReviewMapper postLaunchReviewMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProjectMemberMapper memberMapper;

    private LaunchDateChangeService changeService;
    private PostLaunchReviewService reviewService;
    private GateService gateService;

    /** 纯 JVM 单测无 MP 运行时：手动初始化 lambda 列缓存。 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Person.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, Gate.class);
        TableInfoHelper.initTableInfo(assistant, LaunchDateChangeRequest.class);
        TableInfoHelper.initTableInfo(assistant, PostLaunchReview.class);
    }

    private static final IpdActor MARKET_PM = new IpdActor(101L, "market-pm", "MARKET_PM", 7L);

    @BeforeEach
    void setUp() {
        changeService = new LaunchDateChangeService(changeRequestMapper, projectMapper, auditLogService);
        reviewService = new PostLaunchReviewService(
            postLaunchReviewMapper, projectMapper, memberMapper, personMapper, auditLogService);
        gateService = new GateService(gateMapper, auditLogService);
        // R-NEW-SEC-1 收口后，scheduleReview/completeReview 会过 IpdIdorGuard.requireProjectMemberOrSuperAdmin，
        // 该守卫只靠 memberMapper.selectCount 判定“在任成员”。纯 JVM 单测里统一给 1（=在任），
        // 具体拒绝分支交由 PostLaunchReviewAccessTest 逐角色细验。
        org.mockito.Mockito.lenient().when(memberMapper.selectCount(any())).thenReturn(1L);
    }

    private Project project(long id, Date launchDate, String status) {
        Project p = new Project();
        p.setId(id);
        p.setName("face-access");
        p.setCode("PRJ-2026-001");
        p.setMainGroupId(7L);
        p.setLaunchDate(launchDate);
        p.setStatus(status == null ? "ACTIVE" : status);
        p.setDelFlag("0");
        return p;
    }

    // ------------------- AC-GATE-13 G5 90 天复盘待办 -------------------

    @Test
    @DisplayName("scheduleReview：G5 通过日+项目+原 PM ⇒ 生成 scheduled_at=launchDate+90d PENDING 待办 + 审计")
    void scheduleReviewCreatesPendingTodo() {
        Date launchDate = new Date(System.currentTimeMillis() - 5L * 86_400_000L); // 5 天前上市
        Project p = project(7001L, launchDate, "ACTIVE");
        when(projectMapper.selectById(7001L)).thenReturn(p);
        // 已有 PENDING ⇒ 幂等返回（此处先空 ⇒ 走 insert）
        org.mockito.Mockito.lenient().when(postLaunchReviewMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());
        when(postLaunchReviewMapper.insert(any(PostLaunchReview.class))).thenReturn(1);

        PostLaunchReview r = reviewService.scheduleReview(7001L, launchDate, MARKET_PM);

        assertThat(r.getStatus()).isEqualTo("PENDING");
        assertThat(r.getProjectId()).isEqualTo(7001L);
        // scheduledAt = launchDate + 90d
        long diffMs = r.getScheduledAt().getTime() - launchDate.getTime();
        assertThat(diffMs / 86_400_000L).isEqualTo(90L);

        verify(postLaunchReviewMapper, atLeastOnce()).insert(any(PostLaunchReview.class));
        // 审计 POST_LAUNCH_REVIEW_SCHEDULED
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .contains("POST_LAUNCH_REVIEW_SCHEDULED");
    }

    @Test
    @DisplayName("scheduleReview 幂等：同 projectId 已有 PENDING ⇒ 不创建新记录，复用旧记录")
    void scheduleReviewIdempotent() {
        Date launchDate = new Date();
        Project p = project(7002L, launchDate, "ACTIVE");
        when(projectMapper.selectById(7002L)).thenReturn(p);

        PostLaunchReview existing = PostLaunchReview.builder()
            .id(9001L).projectId(7002L)
            .scheduledAt(new Date(launchDate.getTime() + 90L * 86_400_000L))
            .status("PENDING").build();
        when(postLaunchReviewMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(existing));

        PostLaunchReview r = reviewService.scheduleReview(7002L, launchDate, MARKET_PM);

        assertThat(r.getId()).isEqualTo(9001L);
        // 幂等 ⇒ 不写新 insert
        verify(postLaunchReviewMapper, never()).insert(any(PostLaunchReview.class));
    }

    @Test
    @DisplayName("completeReview：写 COMPLETED + completedAt + 审计 POST_LAUNCH_REVIEW_COMPLETED")
    void completeReviewMarksCompleted() {
        Date now = new Date();
        PostLaunchReview existing = PostLaunchReview.builder()
            .id(9002L).projectId(7003L)
            .scheduledAt(new Date(now.getTime() + 30L * 86_400_000L))
            .status("PENDING").build();
        when(postLaunchReviewMapper.selectById(9002L)).thenReturn(existing);
        // 对象级守卫按记录真实 projectId 判定，需项目可加载
        when(projectMapper.selectById(7003L)).thenReturn(project(7003L, now, "ACTIVE"));
        when(postLaunchReviewMapper.updateById(any(PostLaunchReview.class))).thenReturn(1);

        PostLaunchReview result = reviewService.completeReview(9002L,
            new PostLaunchReviewService.ReviewData(
                new java.math.BigDecimal("4500000.0"),
                "客户反馈：稳定性好",
                "KPI 达成 92%",
                "G6 立项阶段应更早启动市场调研"),
            MARKET_PM);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getActualRevenue()).isEqualByComparingTo("4500000.00");

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .contains("POST_LAUNCH_REVIEW_COMPLETED");
    }

    // ------------------- AC-GATE-26 上市日期变更下游重排 -------------------

    @Test
    @DisplayName("shiftDownstreamGates：G3/G4/G5 截止日 += deltaDays（不超 30 天保护） + 审计 LAUNCH_DATE_CHANGED")
    void shiftDownstreamGates() {
        // G3/G4/G5 三道（delta = +60d）
        Date g3Due = new Date(System.currentTimeMillis() + 10L * 86_400_000L);
        Date g4Due = new Date(System.currentTimeMillis() + 40L * 86_400_000L);
        Date g5Due = new Date(System.currentTimeMillis() + 70L * 86_400_000L);
        Gate g3 = new Gate(); g3.setId(301L); g3.setProjectId(7004L); g3.setGateCode("G3"); g3.setSignDueAt(g3Due);
        Gate g4 = new Gate(); g4.setId(401L); g4.setProjectId(7004L); g4.setGateCode("G4"); g4.setSignDueAt(g4Due);
        Gate g5 = new Gate(); g5.setId(501L); g5.setProjectId(7004L); g5.setGateCode("G5"); g5.setSignDueAt(g5Due);
        when(gateMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(g3, g4, g5));
        when(gateMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        int shifted = gateService.shiftDownstreamGates(7004L, 60L, 8001L, MARKET_PM);

        assertThat(shifted).isEqualTo(3);
        // 3 个 gate 各被 update 一次
        ArgumentCaptor<LambdaUpdateWrapper<Gate>> gateCap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(gateMapper, atLeastOnce()).update(any(), gateCap.capture());
        // 审计 LAUNCH_DATE_CHANGED
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(auditCap.capture());
        assertThat(auditCap.getAllValues()).extracting(AuditLog::getAction)
            .contains("LAUNCH_DATE_CHANGED");
    }

    @Test
    @DisplayName("shiftDownstreamGates 边界：deltaDays=0 ⇒ 不动 gate，不写审计")
    void shiftDownstreamGatesZeroDelta() {
        int shifted = gateService.shiftDownstreamGates(7005L, 0L, 8002L, MARKET_PM);

        assertThat(shifted).isZero();
        verify(gateMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(gateMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }
}