package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-3.2 模板适用性裁剪验收（AC-PROD-02/03、AC-IPD-21/22/25）。
 * <p>形态为内存 Fixture 单测；真库 HTTP 另见证据包，不得仅凭本类标 done。
 */
@Tag("dev")
class P132AcceptanceTest {

    private Fixture f;

    @BeforeEach
    void setup() {
        f = new Fixture();
    }

    @Test
    @DisplayName("AC-PROD-02：硬件模板挂载 69 动作且硬件适用集非 NA")
    void hardwareMountsApplicableActions() {
        f.putProject(21L, "HARDWARE", "[\"国内\"]");
        assertThat(f.bootstrap.bootstrap(21L, 7L)).isEqualTo(6);
        assertThat(f.actions).hasSize(69);
        assertThat(statusOf("P04")).isEqualTo("NOT_STARTED");
        assertThat(statusOf("D03")).isEqualTo("NOT_STARTED");
        assertThat(statusOf("V11")).isEqualTo("NOT_STARTED");
        assertThat(depthOf("V11")).isEqualTo("LIGHT");
        assertThat(statusOf("P05")).isEqualTo("NA");
        assertThat(statusOf("P06")).isEqualTo("NA");
        assertThat(statusOf("C04")).isEqualTo("NA");
    }

    @Test
    @DisplayName("AC-PROD-03：软件模板将 D03/D07/V05 标 NA")
    void softwareMarksHardwareExclusiveAsNa() {
        f.putProject(22L, "SOFTWARE", "[\"CN\"]");
        f.bootstrap.bootstrap(22L, 7L);
        assertThat(statusOf("D03")).isEqualTo("NA");
        assertThat(statusOf("D07")).isEqualTo("NA");
        assertThat(statusOf("V05")).isEqualTo("NA");
        assertThat(statusOf("P05")).isEqualTo("NOT_STARTED");
        assertThat(statusOf("V11")).isEqualTo("NOT_STARTED");
        assertThat(depthOf("V11")).isEqualTo("LIGHT");
    }

    @Test
    @DisplayName("AC-IPD-22：方案模板 V11 升深管；AC-IPD-25 C05 仍轻管；C10 深管非阻断")
    void solutionUpgradesV11AndKeepsC05Light() {
        f.putProject(23L, "SOLUTION", "[\"SA\"]");
        f.bootstrap.bootstrap(23L, 7L);
        assertThat(depthOf("V11")).isEqualTo("DEEP");
        assertThat(statusOf("V11")).isEqualTo("NOT_STARTED");
        assertThat(depthOf("C05")).isEqualTo("LIGHT");
        assertThat(depthOf("C10")).isEqualTo("DEEP");
        assertThat(blockingOf("C10")).isEqualTo("0");
        assertThat(statusOf("P06")).isEqualTo("NOT_STARTED");
        assertThat(statusOf("C04")).isEqualTo("NOT_STARTED");
        assertThat(f.actions).filteredOn(a -> "DEEP".equals(a.getDepth())).hasSize(43);
    }

    private String statusOf(String code) {
        return f.actions.stream().filter(a -> code.equals(a.getActionCode())).findFirst()
            .orElseThrow().getStatus();
    }

    private String depthOf(String code) {
        return f.actions.stream().filter(a -> code.equals(a.getActionCode())).findFirst()
            .orElseThrow().getDepth();
    }

    private String blockingOf(String code) {
        return f.actions.stream().filter(a -> code.equals(a.getActionCode())).findFirst()
            .orElseThrow().getIsBlocking();
    }

    /** 最小内存 Fixture：模拟 Mapper 插入后可读回。 */
    private static final class Fixture {
        final Map<Long, Project> projects = new LinkedHashMap<>();
        final List<ProjectStage> stages = new ArrayList<>();
        final List<StageAction> actions = new ArrayList<>();
        final ProjectStageMapper stageMapper = mock(ProjectStageMapper.class);
        final StageActionMapper actionMapper = mock(StageActionMapper.class);
        final ProjectBootstrapService bootstrap;
        long nextStageId = 1000L;
        long nextActionId = 2000L;

        Fixture() {
            when(stageMapper.selectProjectForBootstrap(anyLong())).thenAnswer(inv ->
                projects.get(inv.getArgument(0, Long.class)));
            when(stageMapper.selectAllStageIdsForBootstrap(anyLong())).thenAnswer(inv ->
                stages.stream().filter(s -> s.getProjectId().equals(inv.getArgument(0)))
                    .map(ProjectStage::getId).toList());
            when(stageMapper.selectAllActionIdsForBootstrap(anyLong())).thenAnswer(inv ->
                actions.stream().filter(a -> a.getProjectId().equals(inv.getArgument(0)))
                    .map(StageAction::getId).toList());
            when(stageMapper.selectLiveByProject(anyLong())).thenAnswer(inv ->
                stages.stream().filter(s -> s.getProjectId().equals(inv.getArgument(0))).toList());
            when(stageMapper.selectActionsForBootstrap(anyLong())).thenAnswer(inv ->
                actions.stream().filter(a -> a.getProjectId().equals(inv.getArgument(0))).toList());
            when(stageMapper.insert(any(ProjectStage.class))).thenAnswer(inv -> {
                ProjectStage stage = inv.getArgument(0);
                stage.setId(++nextStageId);
                stages.add(stage);
                return 1;
            });
            when(actionMapper.insert(any(StageAction.class))).thenAnswer(inv -> {
                StageAction action = inv.getArgument(0);
                action.setId(++nextActionId);
                actions.add(action);
                return 1;
            });
            bootstrap = new ProjectBootstrapService(stageMapper, actionMapper);
        }

        /**
         * 登记待 bootstrap 的项目模板与市场。
         *
         * @param id       项目 ID
         * @param template HARDWARE|SOFTWARE|SOLUTION
         * @param markets  目标市场 JSON
         */
        void putProject(long id, String template, String markets) {
            projects.put(id, Project.builder().id(id).templateType(template)
                .targetMarkets(markets).tenantId("000000").delFlag("0").build());
            stages.clear();
            actions.clear();
        }
    }
}
