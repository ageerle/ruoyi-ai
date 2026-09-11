package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.dto.ProjectListItemView;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P1-9.2 存量 14 天场景复核 + 分段起算验收测试
 *
 * <p>关键边界：
 * <ol>
 *   <li>LEGACY + IN_PROGRESS + 7 天前有活动 ⇒ remaining=7, critical=false</li>
 *   <li>LEGACY + IN_PROGRESS + 12 天前有活动 ⇒ remaining=2, critical=true（≤3）</li>
 *   <li>LEGACY + IN_PROGRESS + 0 天前 ⇒ remaining=14, critical=false</li>
 *   <li>NEW 项目 ⇒ remaining=null, critical=null</li>
 *   <li>LEGACY + COMPLETE ⇒ remaining=null</li>
 *   <li>无活动日 + LEGACY + IN_PROGRESS ⇒ remaining=14（按默认起算）</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P192AcceptanceTest {

    @Mock private ProjectMapper projectMapper;
    @Mock private ProductMapper productMapper;
    @Mock private StageActionMapper stageActionMapper;
    @Mock private KpiRecordMapper kpiRecordMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private GateEngine gateEngine;
    @Mock private ProjectBootstrapService projectBootstrapService;
    @Mock private ProjectCertService projectCertService;
    @Mock private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @InjectMocks private ProjectService projectService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Project.class);
        TableInfoHelper.initTableInfo(assistant, StageAction.class);
        TableInfoHelper.initTableInfo(assistant, KpiRecord.class);
    }

    private Project legacyProject(long id, String catchup, Date lastActivityAt) {
        return Project.builder()
            .id(id).name("LEGACY 项目 " + id).source("LEGACY")
            .catchupStatus(catchup).status("ACTIVE").mainGroupId(70L)
            .lastActivityAt(lastActivityAt)
            .delFlag("0").build();
    }

    private Project newProject(long id) {
        return Project.builder()
            .id(id).name("NEW 项目 " + id).source("NEW")
            .catchupStatus(null).status("DRAFT").delFlag("0").build();
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(projectMapper.selectList(any())).thenAnswer(inv -> java.util.Collections.emptyList());
    }


    @Test
    @DisplayName("AC#1 LEGACY + IN_PROGRESS + 7 天前有活动 ⇒ remaining=7, critical=false")
    void midScenario_remaining7() {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000);
        Project p = legacyProject(1001L, "IN_PROGRESS", sevenDaysAgo);
        when(projectMapper.selectList(any())).thenReturn(List.of(p));
        StageAction sa = new StageAction();
        sa.setProjectId(1001L);
        sa.setUpdateTime(sevenDaysAgo);
        when(stageActionMapper.selectList(any())).thenReturn(List.of(sa));
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of());
        List<ProjectListItemView> result = projectService.listWithScenario(null);
        assertThat(result).hasSize(1);
        ProjectListItemView v = result.get(0);
        assertThat(v.scenarioDaysRemaining()).isBetween(6, 7);
        assertThat(v.critical()).isFalse();
    }

    @Test
    @DisplayName("AC#2 LEGACY + IN_PROGRESS + 12 天前有活动 ⇒ remaining=2, critical=true")
    void criticalScenario_remaining2() {
        Date twelveDaysAgo = new Date(System.currentTimeMillis() - 12L * 24 * 3600 * 1000);
        Project p = legacyProject(1002L, "IN_PROGRESS", twelveDaysAgo);
        when(projectMapper.selectList(any())).thenReturn(List.of(p));
        KpiRecord kr = new KpiRecord();
        kr.setProjectId(1002L);
        kr.setUpdateTime(twelveDaysAgo);
        when(stageActionMapper.selectList(any())).thenReturn(List.of());
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of(kr));
        List<ProjectListItemView> result = projectService.listWithScenario(null);
        ProjectListItemView v = result.get(0);
        assertThat(v.scenarioDaysRemaining()).isBetween(1, 2);
        assertThat(v.critical()).isTrue();
    }

    @Test
    @DisplayName("AC#3 NEW 项目 ⇒ scenarioDaysRemaining=null, critical=null")
    void newProject_noScenarioField() {
        Project p = newProject(1003L);
        when(projectMapper.selectList(any())).thenReturn(List.of(p));
        when(stageActionMapper.selectList(any())).thenReturn(List.of());
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of());
        List<ProjectListItemView> result = projectService.listWithScenario(null);
        ProjectListItemView v = result.get(0);
        assertThat(v.scenarioDaysRemaining()).isNull();
        assertThat(v.critical()).isNull();
    }

    @Test
    @DisplayName("AC#4 LEGACY + COMPLETE ⇒ scenarioDaysRemaining=null")
    void legacyComplete_noScenarioField() {
        Project p = legacyProject(1004L, "COMPLETE", new Date());
        when(projectMapper.selectList(any())).thenReturn(List.of(p));
        when(stageActionMapper.selectList(any())).thenReturn(List.of());
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of());
        List<ProjectListItemView> result = projectService.listWithScenario(null);
        assertThat(result.get(0).scenarioDaysRemaining()).isNull();
        assertThat(result.get(0).critical()).isNull();
    }

    @Test
    @DisplayName("AC#5 LEGACY + IN_PROGRESS + 无活动日 ⇒ remaining=14（按默认起算）")
    void legacyNoActivity_default14() {
        Project p = legacyProject(1005L, "IN_PROGRESS", null);
        when(projectMapper.selectList(any())).thenReturn(List.of(p));
        when(stageActionMapper.selectList(any())).thenReturn(List.of());
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of());
        List<ProjectListItemView> result = projectService.listWithScenario(null);
        assertThat(result.get(0).scenarioDaysRemaining()).isEqualTo(14);
        assertThat(result.get(0).critical()).isFalse();
    }

    @Test
    @DisplayName("AC#6 临界阈值常量 = 3 / 周期 = 14")
    void constants() {
        assertThat(ProjectService.LEGACY_SCENARIO_DAYS).isEqualTo(14);
        assertThat(ProjectService.LEGACY_SCENARIO_CRITICAL_DAYS).isEqualTo(3);
    }
}