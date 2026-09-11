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
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.support.NoopTransactionManager;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P1-2.2：基准锁定、上市日双签、暂停/归档只读（AC-INC-33）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P122AcceptanceTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProductMapper productMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private GateEngine gateEngine;
    @Mock private ProjectBootstrapService projectBootstrapService;
    @Mock private ProjectCertService projectCertService;
    @Mock private org.ruoyi.ipd.mapper.StageActionMapper stageActionMapper;
    @Mock private org.ruoyi.ipd.mapper.KpiRecordMapper kpiRecordMapper;
    @Mock private LaunchDateChangeRequestMapper launchDateChangeRequestMapper;

    private ProjectService projectService;
    private LaunchDateChangeService launchDateChangeService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectMapper, productMapper, stageActionMapper, kpiRecordMapper, auditLogService, gateEngine, projectBootstrapService, projectCertService, NoopTransactionManager.INSTANCE, null /* P2-6.2 */);
        launchDateChangeService = new LaunchDateChangeService(
            launchDateChangeRequestMapper, projectMapper, auditLogService);
    }

    /**
     * 主组必须与下文传入的 actorGroupId(=70L) 一致：a87293f6（R8X-CONT-1 P0-1）把
     * assertSameGroup 提到了 DRAFT/状态等既有校验之前，fixture 不设 mainGroupId 会先撞
     * 「提议人必须归属项目主组」而永远到不了本卡要验的断言（2026-09-05 实测 3/3 红）。
     * 只补 fixture，不动任何断言。
     */
    private Project activeProject() {
        return Project.builder().id(70L).name("P122").status("ACTIVE").delFlag("0")
            .currentStage("VALID").mainGroupId(70L).launchDate(null).build();
    }

    @Test
    @DisplayName("AC-INC-33：单人不可双签确认；双人确认后写入 launchDate")
    void launchDateRequiresDualSign() {
        Project project = activeProject();
        when(projectMapper.selectById(70L)).thenReturn(project);
        when(launchDateChangeRequestMapper.selectCount(any())).thenReturn(0L);
        when(launchDateChangeRequestMapper.insert(any(LaunchDateChangeRequest.class))).thenAnswer(inv -> {
            LaunchDateChangeRequest r = inv.getArgument(0);
            r.setId(501L);
            return 1;
        });
        Date day = new Date(1_700_000_000_000L);
        LaunchDateChangeRequest pending = launchDateChangeService.propose(
            70L, day, "GTM 定档", 11L, "MARKET_PM", 70L,
            // R11 / A1 修复:预落 confirmer（提议人 MARKET_PM → 第二签人 RD_PM 22L）
            22L, "RD_PM", 70L);
        assertThat(pending.getStatus()).isEqualTo(LaunchDateChangeRequest.ST_PENDING_SECOND);

        when(launchDateChangeRequestMapper.selectById(501L)).thenReturn(pending);
        when(launchDateChangeRequestMapper.updateById(any(LaunchDateChangeRequest.class))).thenReturn(1);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        assertThatThrownBy(() -> launchDateChangeService.secondDecision(
            501L, 11L, "RD_PM", 70L, true, "ok"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不同人员");

        LaunchDateChangeRequest confirmed = launchDateChangeService.secondDecision(
            501L, 22L, "RD_PM", 70L, true, "同意");
        assertThat(confirmed.getStatus()).isEqualTo(LaunchDateChangeRequest.ST_CONFIRMED);
        assertThat(project.getLaunchDate()).isEqualTo(day);
    }

    @Test
    @DisplayName("四基准 DRAFT 可改；ACTIVE 锁定")
    void baselinesLockedAfterDraft() {
        Project draft = Project.builder().id(71L).status("DRAFT").delFlag("0")
            .templateType("HARDWARE").targetMarkets("[\"CN\"]").mainGroupId(1L)
            .level("A").targetSalesAmount(new BigDecimal("100"))
            .targetChannelCount(1).targetNps(50).targetSceneCount(1).name("d").build();
        when(projectMapper.selectById(71L)).thenReturn(draft);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);
        projectService.updateBaselines(71L, Project.builder()
            .targetSalesAmount(new BigDecimal("200"))
            .targetChannelCount(2).targetNps(60).targetSceneCount(2).build(), 70L, 1L, "MARKET_PM");
        assertThat(draft.getTargetSalesAmount()).isEqualByComparingTo("200");

        Project active = Project.builder().id(72L).status("ACTIVE").delFlag("0").name("a")
            .mainGroupId(1L).build();
        when(projectMapper.selectById(72L)).thenReturn(active);
        assertThatThrownBy(() -> projectService.updateBaselines(72L, Project.builder()
            .targetSalesAmount(new BigDecimal("9")).build(), 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("锁定");
    }

    @Test
    @DisplayName("暂停/归档禁止推进阶段")
    void suspendedBlocksAdvance() {
        Project suspended = Project.builder().id(73L).status("SUSPENDED").delFlag("0")
            .currentStage("CONCEPT").name("s").mainGroupId(1L).build();
        when(projectMapper.selectById(73L)).thenReturn(suspended);
        assertThatThrownBy(() -> projectService.advanceStage(73L, 1L, 1L, "MARKET_PM"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("暂停/归档");
    }
}
