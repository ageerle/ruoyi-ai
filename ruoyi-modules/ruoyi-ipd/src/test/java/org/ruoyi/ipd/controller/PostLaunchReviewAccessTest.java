package org.ruoyi.ipd.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.PostLaunchReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.PostLaunchReviewMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdRolePermissionCatalog;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.PostLaunchReviewService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R-NEW-ARCH-1 / R-NEW-SEC-1 / R-NEW-SEC-5 / R-NEW-SEC-6 收口验收（G5 上市 90 天复盘）。
 *
 * <p>覆盖四件事，缺一不可：
 * <ol>
 *   <li><b>可达入口</b>：Controller 三个端点确实把请求派到对应 Service 方法，并按 IPD 契约把
 *       雪花 ID 字符串化（前端不接 19 位 JSON number）。</li>
 *   <li><b>入口守卫</b>：写入口需 MARKET_PM/超管角色门 + 该项目在职成员；读入口需在职成员。
 *       逐角色断言，含 null actor（防"内部方法不必鉴权"的旁路）。</li>
 *   <li><b>对象级防 IDOR</b>：completeReview 必须按记录真实 projectId 判定。</li>
 *   <li><b>配置/DDL 原子性</b>：新表 post_launch_reviews 的建表脚本与 tenant.excludes 登记
 *       必须同批存在（任一缺失即验收失败；本类只静态核对仓库文件，不触真库）。</li>
 * </ol>
 *
 * <p>口径说明：本类不启动 Spring，租户上下文缺失（{@code IpdIdorGuard.currentTenantId()} 返回 null）
 * 按"未启用租户隔离"放行，与 W4-Security 决策 1 一致，故守卫断言只针对角色与成员两个维度。
 * 成员判定一律用 {@code selectCount} 的 0/1 桩驱动，不去解析 MyBatis-Plus wrapper 内部参数
 * （本仓已实测该解析不稳定，见 P2-7.1 教训）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("R-NEW：G5 复盘入口可达性 + 守卫矩阵 + DDL/租户配置原子性")
class PostLaunchReviewAccessTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir"))
        .getParent().getParent();

    @Mock private PostLaunchReviewMapper reviewMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission permission;

    private PostLaunchReviewService reviewService;
    private PostLaunchReviewController controller;

    private static final Long PROJECT_A = 7101L;
    private static final Long PROJECT_B = 7102L;
    private static final Long PM_A_PERSON = 101L;
    private static final Long OUTSIDER_PERSON = 303L;

    private static final IpdActor MARKET_PM_A = new IpdActor(PM_A_PERSON, "pm-a", "MARKET_PM", 7L);
    private static final IpdActor MARKET_PM_OUTSIDER = new IpdActor(OUTSIDER_PERSON, "pm-x", "MARKET_PM", 7L);
    private static final IpdActor SUPER_ADMIN = new IpdActor(1L, "sa", "SUPER_ADMIN", 0L);
    private static final IpdActor RD_PM = new IpdActor(202L, "rd", "RD_PM", 7L);
    private static final IpdActor GROUP_LEADER = new IpdActor(301L, "leader", "GROUP_LEADER", 7L);

    @BeforeAll
    static void initTableInfo() {
        // 纯 JVM 单测无 MP 运行时：Lambda 列缓存需手动初始化
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, ProjectMember.class);
        TableInfoHelper.initTableInfo(assistant, PostLaunchReview.class);
    }

    @BeforeEach
    void setUp() {
        reviewService = new PostLaunchReviewService(
            reviewMapper, projectMapper, memberMapper, personMapper, auditLogService);
        controller = new PostLaunchReviewController(reviewService, permission);
        when(projectMapper.selectById(PROJECT_A)).thenReturn(project(PROJECT_A));
        when(projectMapper.selectById(PROJECT_B)).thenReturn(project(PROJECT_B));
    }

    private static Project project(Long id) {
        Project p = new Project();
        p.setId(id);
        p.setMainGroupId(7L);
        p.setStatus("ACTIVE");
        p.setDelFlag("0");
        return p;
    }

    /** actor 是否被判定为"该项目在职成员"——只由 selectCount 桩决定。 */
    private void actingAsMember(boolean member) {
        when(memberMapper.selectCount(any())).thenReturn(member ? 1L : 0L);
    }

    private static void assertErrorCode(Throwable e, ApiV1ErrorCode expected) {
        assertThat(e).isInstanceOf(IpdBusinessException.class);
        assertThat(((IpdBusinessException) e).getErrorCode()).isEqualTo(expected);
    }

    // ==================== 入口守卫：排期 ====================

    @Test
    @DisplayName("scheduleReview：null actor ⇒ UNAUTHORIZED（service 不信任 controller 必传）")
    void scheduleRejectsNullActor() {
        assertThatThrownBy(() -> reviewService.scheduleReview(PROJECT_A, new Date(), null))
            .satisfies(e -> assertErrorCode(e, ApiV1ErrorCode.UNAUTHORIZED));
        verify(reviewMapper, never()).insert(any(PostLaunchReview.class));
    }

    @Test
    @DisplayName("scheduleReview：RD_PM / GROUP_LEADER ⇒ FORBIDDEN，且早于任何 DB 读")
    void scheduleRejectsWrongRole() {
        for (IpdActor actor : List.of(RD_PM, GROUP_LEADER)) {
            assertThatThrownBy(() -> reviewService.scheduleReview(PROJECT_A, new Date(), actor))
                .as("%s 不应能排期复盘", actor.role())
                .satisfies(e -> assertErrorCode(e, ApiV1ErrorCode.FORBIDDEN));
        }
        verify(projectMapper, never()).selectById(any());
        verify(reviewMapper, never()).insert(any(PostLaunchReview.class));
    }

    @Test
    @DisplayName("scheduleReview：MARKET_PM 但非该项目在职成员 ⇒ FORBIDDEN")
    void scheduleRejectsNonMemberMarketPm() {
        actingAsMember(false);
        assertThatThrownBy(() -> reviewService.scheduleReview(PROJECT_A, new Date(), MARKET_PM_OUTSIDER))
            .satisfies(e -> assertErrorCode(e, ApiV1ErrorCode.FORBIDDEN));
        verify(reviewMapper, never()).insert(any(PostLaunchReview.class));
    }

    @Test
    @DisplayName("scheduleReview：MARKET_PM + 在职成员 ⇒ PENDING，tenant_id 用默认常量")
    void scheduleAllowsActiveMarketPm() {
        actingAsMember(true);
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(reviewMapper.insert(any(PostLaunchReview.class))).thenReturn(1);

        Date launchDate = Date.from(LocalDate.of(2026, 8, 1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant());
        PostLaunchReview created = reviewService.scheduleReview(PROJECT_A, launchDate, MARKET_PM_A);

        assertThat(created.getStatus()).isEqualTo("PENDING");
        assertThat(created.getTenantId()).isEqualTo("000000");
        long days = (created.getScheduledAt().getTime() - launchDate.getTime()) / 86_400_000L;
        assertThat(days).isEqualTo(90L);
    }

    @Test
    @DisplayName("scheduleReview：超管豁免成员校验（非成员也可排期）")
    void scheduleSuperAdminBypassesMembership() {
        actingAsMember(false);
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(reviewMapper.insert(any(PostLaunchReview.class))).thenReturn(1);

        assertThat(reviewService.scheduleReview(PROJECT_B, new Date(), SUPER_ADMIN)).isNotNull();
    }

    // ==================== 入口守卫：完成（防 IDOR） ====================

    @Test
    @DisplayName("completeReview：非成员不能完成他人项目复盘，且判定用的是记录里的 projectId")
    void completeReviewGuardsOnRecordProject() {
        actingAsMember(false);
        when(reviewMapper.selectById(9002L)).thenReturn(PostLaunchReview.builder()
            .id(9002L).projectId(PROJECT_B).status("PENDING").build());

        assertThatThrownBy(() -> reviewService.completeReview(9002L,
            new PostLaunchReviewService.ReviewData(null, "x", "y", "z"), MARKET_PM_A))
            .satisfies(e -> assertErrorCode(e, ApiV1ErrorCode.FORBIDDEN));

        // 守卫读取的是记录真实所属项目（B），而非任何调用方自报值
        verify(projectMapper).selectById(PROJECT_B);
        verify(reviewMapper, never()).updateById(any(PostLaunchReview.class));
    }

    @Test
    @DisplayName("completeReview：角色门先于记录加载；RD_PM 直接 FORBIDDEN 且不触达复盘表")
    void completeReviewRoleGateFirst() {
        assertThatThrownBy(() -> reviewService.completeReview(9002L, null, RD_PM))
            .satisfies(e -> assertErrorCode(e, ApiV1ErrorCode.FORBIDDEN));
        verify(reviewMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("completeReview：在职 MARKET_PM 正常置 COMPLETED")
    void completeReviewAllowsActiveMember() {
        actingAsMember(true);
        when(reviewMapper.selectById(9002L)).thenReturn(PostLaunchReview.builder()
            .id(9002L).projectId(PROJECT_A).status("PENDING").build());
        when(reviewMapper.updateById(any(PostLaunchReview.class))).thenReturn(1);

        PostLaunchReview done = reviewService.completeReview(9002L,
            new PostLaunchReviewService.ReviewData(new java.math.BigDecimal("4500000.00"),
                "稳定性好", "KPI 达成 92%", "更早启动市场调研"), MARKET_PM_A);

        assertThat(done.getStatus()).isEqualTo("COMPLETED");
        assertThat(done.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("completeReview：已 COMPLETED 的复盘不可重复完成（业务异常，非权限异常）")
    void completeReviewRejectsAlreadyCompleted() {
        actingAsMember(true);
        when(reviewMapper.selectById(9002L)).thenReturn(PostLaunchReview.builder()
            .id(9002L).projectId(PROJECT_A).status("COMPLETED").build());

        assertThatThrownBy(() -> reviewService.completeReview(9002L, null, MARKET_PM_A))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不可重复完成");
    }

    // ==================== 入口守卫：读 ====================

    @Test
    @DisplayName("findPendingByProject：非在职成员 ⇒ FORBIDDEN")
    void pendingQueryRejectsNonMember() {
        actingAsMember(false);
        assertThatThrownBy(() -> reviewService.findPendingByProject(PROJECT_A, MARKET_PM_OUTSIDER))
            .satisfies(e -> assertErrorCode(e, ApiV1ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("findPendingByProject：无 PENDING ⇒ 业务异常（前端据此区分『无待办』）")
    void pendingQueryThrowsWhenEmpty() {
        actingAsMember(true);
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertThatThrownBy(() -> reviewService.findPendingByProject(PROJECT_A, MARKET_PM_A))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无待完成复盘");
    }

    @Test
    @DisplayName("findPendingByProject：任意项目在职成员可读（读不比写严，PM 交接期也能看待办）")
    void pendingQueryAllowsAnyActiveMember() {
        actingAsMember(true);
        when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            PostLaunchReview.builder().id(9100L).projectId(PROJECT_A).status("PENDING").build()));

        IpdActor rdMember = new IpdActor(PM_A_PERSON, "rd-in-a", "RD_PM", 7L);
        assertThat(reviewService.findPendingByProject(PROJECT_A, rdMember).getId()).isEqualTo(9100L);
    }

    // ==================== Controller 可达性 ====================

    @Nested
    @DisplayName("Controller 端点")
    class Endpoints {

        @Test
        @DisplayName("GET /pending → 视图内 ID 全部字符串化，包络 code=0")
        void pendingEndpoint() {
            actingAsMember(true);
            when(permission.requireInternal()).thenReturn(MARKET_PM_A);
            when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                PostLaunchReview.builder().id(1_234_567_890_123_456_789L).projectId(PROJECT_A)
                    .assigneeId(PM_A_PERSON).status("PENDING").scheduledAt(new Date(0L)).build()));

            var resp = controller.pending(PROJECT_A);

            assertThat(resp.getCode()).isEqualTo(ApiV1ResponseCode.SUCCESS);
            assertThat(resp.getData().id()).isEqualTo("1234567890123456789");
            assertThat(resp.getData().projectId()).isEqualTo("7101");
            assertThat(resp.getData().assigneeId()).isEqualTo("101");
        }

        @Test
        @DisplayName("POST / → launchDate 按系统时区零时转 Date（R8-P0-11 口径）")
        void scheduleEndpoint() {
            when(permission.requireInternal()).thenReturn(SUPER_ADMIN);
            when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(reviewMapper.insert(any(PostLaunchReview.class))).thenReturn(1);

            var resp = controller.schedule(new PostLaunchReviewController.ScheduleRequest(
                PROJECT_A, LocalDate.of(2026, 8, 1)));

            assertThat(resp.getData().status()).isEqualTo("PENDING");
            assertThat(resp.getData().projectId()).isEqualTo("7101");
            verify(reviewMapper).insert(any(PostLaunchReview.class));
        }

        @Test
        @DisplayName("POST /{id}/complete → body 缺省按空复盘数据处理，不报 400")
        void completeEndpointWithoutBody() {
            when(permission.requireInternal()).thenReturn(SUPER_ADMIN);
            when(reviewMapper.selectById(9002L)).thenReturn(PostLaunchReview.builder()
                .id(9002L).projectId(PROJECT_A).status("PENDING").build());
            when(reviewMapper.updateById(any(PostLaunchReview.class))).thenReturn(1);

            var resp = controller.complete(9002L, null);

            assertThat(resp.getData().status()).isEqualTo("COMPLETED");
            assertThat(resp.getData().completedAt()).isNotNull();
        }

        @Test
        @DisplayName("三个权限码均已登记目录，不会重现『连超管都被注解拒』的 403")
        void permissionCodesRegistered() {
            for (String code : List.of(
                IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_QUERY,
                IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_CREATE,
                IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_COMPLETE)) {
                assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", code))
                    .as("SUPER_ADMIN 应持有 %s", code).isTrue();
            }
            assertThat(IpdRolePermissionCatalog.has("RD_PM",
                IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_QUERY)).isTrue();
            assertThat(IpdRolePermissionCatalog.has("EXTERNAL_AUDITOR",
                IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_CREATE)).isFalse();
        }
    }

    // ==================== 配置与 DDL 契约一致性 ====================

    @Test
    @DisplayName("原子变更核对：建表脚本存在且 post_launch_reviews 已登记 tenant.excludes")
    void ddlAndTenantExcludesShippedTogether() throws IOException {
        List<Path> migrations;
        try (Stream<Path> files = Files.list(REPO_ROOT.resolve("docs/script/sql/update"))) {
            migrations = files.filter(p -> p.getFileName().toString().endsWith(".sql"))
                .filter(p -> {
                    try {
                        return Files.readString(p).contains("CREATE TABLE IF NOT EXISTS `post_launch_reviews`");
                    } catch (IOException e) {
                        return false;
                    }
                }).toList();
        }
        assertThat(migrations)
            .as("post_launch_reviews 必须有可重放的建表迁移脚本")
            .hasSize(1);

        String yml = Files.readString(
            REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application.yml"));
        assertThat(yml)
            .as("新表必须同批登记 tenant.excludes，否则多租户插件静默过滤致查询为空")
            .containsPattern("(?m)^\\s*-\\s*post_launch_reviews\\s*$");

        // 脚本自身契约：软删列 + 租户列 + 回滚说明
        String ddl = Files.readString(migrations.get(0));
        assertThat(ddl).contains("`del_flag`").contains("`tenant_id`").contains("ROLLBACK");
    }

    /** 成功码常量（避免在断言里散落 0 字面量）。 */
    private static final class ApiV1ResponseCode {
        static final int SUCCESS = 0;
    }
}
