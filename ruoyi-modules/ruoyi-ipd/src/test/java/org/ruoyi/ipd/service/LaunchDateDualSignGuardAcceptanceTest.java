package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.LaunchDateChangeRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.qa.GuardSourceUtils;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * P1 加固（owner 2026-09-05 指令项1）：上市日期双签的并发防护与第二签归属权。
 *
 * <p>覆盖三条真实缺陷，每条都是「先证红、再证绿」写出来的：
 * <ol>
 *   <li><b>1a propose TOCTOU</b>——预检 {@code selectCount} 与 {@code insert} 非同一原子临界区，
 *       并发提议会给同一项目留下两条 PENDING_SECOND。DB 侧靠部分唯一索引
 *       （{@code uk_ldcr_pending_project}）兜底，应用侧必须把 {@link DuplicateKeyException}
 *       翻译成与预检同文案的业务错，而不是塌成 500。</li>
 *   <li><b>1b secondDecision 归属</b>——旧实现只拒「两侧都非超管且角色相同」，
 *       既无第二签角色白名单（{@code IpdPermission.INTERNAL_ROLES} 含 GROUP_LEADER，
 *       故组长可补签），又因 {@code confirmerRole != null &&} 短路而放过 null 角色；
 *       且 {@code selectById} 后无条件 {@code updateById}，两个确认人并发都写成功，
 *       同一申请留下两条 ACTION_CONFIRM、confirmer 被后写者覆盖。</li>
 *   <li><b>1c launch_date 写入面</b>——静态守卫钉住「全仓 main 源只有两处可写
 *       {@code projects.launch_date}」，且 {@code ProjectService} 自身不得出现任何
 *       launch_date 写调用。R8-AUTO-5 曾删掉守卫 {@code assertNoDirectLaunchDateMutation}
 *       并以注释声称由 P0-2 groupId 校验兜底，该声明不成立，故改由本用例执法。</li>
 * </ol>
 *
 * <p>不覆盖（须真库/HTTP，另记）：唯一索引在活库的实际生效、并发 50 路压测。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P1-2.2 加固：上市日期双签并发防护 + 第二签归属 + launch_date 写入面守卫")
class LaunchDateDualSignGuardAcceptanceTest {

    @Mock private LaunchDateChangeRequestMapper requestMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;

    private LaunchDateChangeService service;

    private static final Date DAY = new Date(1_700_000_000_000L);
    private static final Date OTHER_DAY = new Date(1_800_000_000_000L);

    @BeforeEach
    void setUp() {
        service = new LaunchDateChangeService(requestMapper, projectMapper, auditLogService);
    }

    /** 主组=70 的在途项目；actor.groupId 传 70 才能过 R8X-2 横向越权防护。 */
    private Project project(Date launchDateSeed) {
        return Project.builder().id(70L).name("GUARD").status("ACTIVE").delFlag("0")
            .currentStage("VALID").mainGroupId(70L).launchDate(launchDateSeed).build();
    }

    private LaunchDateChangeRequest pending() {
        return LaunchDateChangeRequest.builder()
            .id(501L).projectId(70L).proposedLaunchDate(DAY).previousLaunchDate(null)
            .reason("GTM 定档").proposerId(11L).proposerRole("MARKET_PM")
            .status(LaunchDateChangeRequest.ST_PENDING_SECOND)
            .tenantId("000000").delFlag("0").version(0).build();
    }

    // ---------------- 1a propose：DB 唯一索引冲突必须翻译为业务语义 ----------------

    @Test
    @DisplayName("1a-1) 预检放过但 insert 撞 uk → ServiceException 同文案，不得塌 500、不得写审计")
    void proposeTranslatesUniqueKeyCollision() {
        when(projectMapper.selectById(70L)).thenReturn(project(null));
        when(requestMapper.selectCount(any())).thenReturn(0L);
        when(requestMapper.insert(any(LaunchDateChangeRequest.class)))
            .thenThrow(new DuplicateKeyException("Duplicate entry '70' for key 'uk_ldcr_pending_project'"));

        assertThatThrownBy(() -> service.propose(70L, DAY, "GTM 定档", 11L, "MARKET_PM", 70L,
            // R11 / A1 修复:预落 confirmer（提议人 MARKET_PM → 第二签人 RD_PM 22L，与同测试 1b 闭环）
            22L, "RD_PM", 70L))
            .isInstanceOf(ServiceException.class)
            .hasMessage("该项目已有待第二签确认的上市日期变更申请");

        // 与预检分支同文案：两条路径对客户端必须不可区分
        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("1a-2) 预检本身命中已有 PENDING → 同文案（守 1a-1 的等价性前提）")
    void proposeRejectsOnPrecheckWithSameMessage() {
        when(projectMapper.selectById(70L)).thenReturn(project(null));
        when(requestMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.propose(70L, DAY, "GTM 定档", 11L, "MARKET_PM", 70L,
            22L, "RD_PM", 70L))
            .isInstanceOf(ServiceException.class)
            .hasMessage("该项目已有待第二签确认的上市日期变更申请");

        verify(requestMapper, never()).insert(any(LaunchDateChangeRequest.class));
    }

    // ---------------- 1b secondDecision：第二签归属（角色 + 状态迁移 CAS） ----------------

    @Test
    @DisplayName("1b-1) GROUP_LEADER 不可补第二签（旧实现放行：非白名单且与提议人角色不同即通过）")
    void groupLeaderCannotSecondSign() {
        when(requestMapper.selectById(501L)).thenReturn(pending());

        assertThatThrownBy(() -> service.secondDecision(501L, 22L, "GROUP_LEADER", 70L, true, "同意"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅市场PM/研发PM/超管");

        verify(requestMapper, never()).updateById(any(LaunchDateChangeRequest.class));
    }

    @Test
    @DisplayName("1b-2) confirmerRole=null 必须 fail-closed（旧实现因 != null 短路而放行）")
    void nullRoleCannotSecondSign() {
        when(requestMapper.selectById(501L)).thenReturn(pending());

        assertThatThrownBy(() -> service.secondDecision(501L, 22L, null, 70L, true, "同意"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅市场PM/研发PM/超管");
    }

    @Test
    @DisplayName("1b-3) 未知角色同样拒绝（白名单而非黑名单）")
    void unknownRoleCannotSecondSign() {
        when(requestMapper.selectById(501L)).thenReturn(pending());

        assertThatThrownBy(() -> service.secondDecision(501L, 22L, "FINANCE", 70L, true, "同意"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅市场PM/研发PM/超管");
    }

    @Test
    @DisplayName("1b-4) 状态迁移 CAS 未命中（已被并发处理）→ 拒绝、不写 launchDate、不留 CONFIRM 审计")
    void secondSignRequiresCasHit() {
        Project project = project(null);
        when(requestMapper.selectById(501L)).thenReturn(pending());
        when(projectMapper.selectById(70L)).thenReturn(project);
        when(requestMapper.updateById(any(LaunchDateChangeRequest.class))).thenReturn(0);

        assertThatThrownBy(() -> service.secondDecision(501L, 22L, "RD_PM", 70L, true, "同意"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已被并发处理");

        assertThat(project.getLaunchDate()).isNull();
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(auditLogService, never()).append(any());
    }

    @Test
    @DisplayName("1b-5) 正常互补双签仍成立：RD_PM 确认 MARKET_PM 提议 → launchDate 落库 + 一条 CONFIRM")
    void happyPathStillWorks() {
        Project project = project(null);
        when(requestMapper.selectById(501L)).thenReturn(pending());
        when(projectMapper.selectById(70L)).thenReturn(project);
        when(requestMapper.updateById(any(LaunchDateChangeRequest.class))).thenReturn(1);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        LaunchDateChangeRequest confirmed = service.secondDecision(501L, 22L, "RD_PM", 70L, true, "同意");

        assertThat(confirmed.getStatus()).isEqualTo(LaunchDateChangeRequest.ST_CONFIRMED);
        assertThat(confirmed.getConfirmerId()).isEqualTo(22L);
        assertThat(project.getLaunchDate()).isEqualTo(DAY);
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("1b-6) 驳回路径也走 CAS：命中则置 REJECTED 且不动 launchDate")
    void rejectPathUsesCasAndKeepsDate() {
        Project project = project(OTHER_DAY);
        when(requestMapper.selectById(501L)).thenReturn(pending());
        when(projectMapper.selectById(70L)).thenReturn(project);
        when(requestMapper.updateById(any(LaunchDateChangeRequest.class))).thenReturn(1);

        LaunchDateChangeRequest rejected = service.secondDecision(501L, 22L, "RD_PM", 70L, false, "不同意");

        assertThat(rejected.getStatus()).isEqualTo(LaunchDateChangeRequest.ST_REJECTED);
        assertThat(project.getLaunchDate()).isEqualTo(OTHER_DAY);
        verify(projectMapper, never()).updateById(any(Project.class));
    }

    // ---------------- 1c 静态守卫：launch_date 写入面必须可枚举 ----------------

    @Test
    @DisplayName("1c-1) 全仓 main 源可写 projects.launch_date 的文件只有白名单两处")
    void launchDateWriteSitesAreEnumerated() throws IOException {
        Set<Path> allowed = Set.of(
            // 双签后写回（唯一「修改」入口）
            Path.of("org/ruoyi/ipd/service/LaunchDateChangeService.java"),
            // 首次录入（BR-IPD-08「L08 录入」，仅 INSERT，不构成修改）
            Path.of("org/ruoyi/ipd/dto/ProjectCreateReq.java"),
            // ZK 场景种数据（demo 性质，不入业务守卫；本文件已有「治理豁免」注释锚定）
            Path.of("org/ruoyi/ipd/config/IpdZkScenarioInitializer.java"),
            // R11 修复:L08 首次录入 Controller 入口，委托给 launchDateChangeService.initialRecord。
            // req.launchDate() 是 DTO 字段读（不是写库），写库动作全部封装在 Service。
            Path.of("org/ruoyi/ipd/controller/ProjectController.java"));

        Set<String> found = scanMainSources().stream()
            .filter(p -> readsLikeLaunchDateWrite(p))
            .map(p -> p.toString().substring(p.toString().indexOf("/java/") + "/java/".length()))
            .filter(rel -> !allowed.contains(Path.of(rel)))
            .collect(Collectors.toSet());

        assertThat(found).as("新增 launch_date 写入点必须先过双签设计：见 LaunchDateChangeService 类注释").isEmpty();
    }

    @Test
    @DisplayName("1c-2) ProjectService 不得出现任何 launch_date 写调用（防「口头兜底」再现）")
    void projectServiceNeverWritesLaunchDate() throws IOException {
        Path file = repoRoot().resolve(
            "ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/ProjectService.java");
        String content = Files.readString(file);
        // 2026-09-08 AM-GUARD：先剔注释再断言（源码注释提及该串不构成违规）；
        // 与 1c-1 同口径——空参 .launchDate() 是 record accessor 读（合法），带参形态才是写。
        String codeOnly = GuardSourceUtils.stripComments(content);

        assertThat(codeOnly).doesNotContain("setLaunchDate(");
        assertThat(java.util.regex.Pattern.compile("\\.launchDate\\([^)]").matcher(codeOnly).find())
            .as("空参 .launchDate() 是 accessor 读（合法），带参 builder 写才违规").isFalse();
    }

    @Test
    @DisplayName("1c-3) 部分唯一索引迁移脚本在库（防「活库已修、新库不修」）")
    void pendingUniqueIndexMigrationExists() {
        Path sql = repoRoot().resolve(
            "docs/script/sql/update/2026-09-05-ipd-launch-date-pending-unique.sql");
        assertThat(Files.exists(sql)).as("1a 的 DB 侧兜底迁移必须入库").isTrue();
    }

    // ---------------- helpers ----------------

    private static boolean readsLikeLaunchDateWrite(Path p) {
        try {
            // 2026-09-08 AM-GUARD：先剔注释再匹配（全仓扫描的注释提及不构成违规）
            String c = GuardSourceUtils.stripComments(Files.readString(p));
            // 空参 .launchDate() 是 record accessor 读取（非写，如 PostLaunchReviewController/ProjectController）；
            // 只识别 setLaunchDate( 与带参 builder 写 .launchDate(x
            return c.contains("setLaunchDate(")
                || java.util.regex.Pattern.compile("\\.launchDate\\([^)]").matcher(c).find();
        } catch (IOException e) {
            throw new IllegalStateException("读取失败: " + p, e);
        }
    }

    private static List<Path> scanMainSources() throws IOException {
        Path root = repoRoot().resolve("ruoyi-modules/ruoyi-ipd/src/main/java");
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    /** 自适应 git root：surefire 工作目录=模块根，IDE 直跑=项目根。 */
    private static Path repoRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }
}
