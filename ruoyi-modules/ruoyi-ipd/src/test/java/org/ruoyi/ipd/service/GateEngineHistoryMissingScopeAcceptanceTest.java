package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.GateChecklistItem;
import org.ruoyi.ipd.dto.GateChecklistView;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1（owner 2026-09-05 指令项1d）：GateEngine 对 {@code HISTORICAL_MISSING} 的豁免范围收窄。
 *
 * <p>判据来源不是既有实现，而是 BR-PROD-03 的原文语义 + 打标记侧的同一判据
 * （{@code LegacyImportService.markPastStages} 只对 {@code isStageBefore(def.stage(), declared)}
 * 成立的动作打 MISSING）。门禁侧此前只认标志位，因此存在「标志位一旦落到不该豁免的行上就永久免检」的口子。
 *
 * <p>诚实边界：C01–C11 的 {@link #check} MISSING 分支在正常 legacy 导入链路上当前不可达
 * （打标记范围 == 豁免范围），本测试钉的是<b>收窄后的边界</b>，不是复现线上缺陷；
 * 修复前的红证来自「直接写库/数据修复把 MISSING 打在申报范围外」这一可被写入的路径。
 *
 * <p>不修改兄弟维护的 {@code GateEngineTest}，新增独立文件，避免并发写冲突。
 */
@Tag("dev")
class GateEngineHistoryMissingScopeAcceptanceTest {

    private static final String MISSING = LegacyImportService.HISTORY_MISSING;

    private StageActionMapper actionMapper;
    private SystemConfigService configService;
    private GateEngine engine;

    @BeforeEach
    void setUp() {
        actionMapper = mock(StageActionMapper.class);
        configService = mock(SystemConfigService.class);
        engine = new GateEngine(actionMapper, configService);
    }

    private StageAction act(String code, String status) {
        return StageAction.builder().id(1L).projectId(100L).stageId(10L)
            .actionCode(code).actionName(ActionCatalog.byCode(code).name()).status(status).build();
    }

    /** 把 C11 换成带 MISSING 标记的行（IN_PROGRESS：只有 MISSING 能救它，否则必红）。 */
    private StageAction missingC11() {
        StageAction c11 = act("C11", "IN_PROGRESS");
        c11.setHistoryMark(MISSING);
        return c11;
    }

    private Project project(String declaredStage) {
        return Project.builder()
            .id(100L).level("S").currentStage("CONCEPT").name("gate-history-missing")
            .declaredStage(declaredStage)
            .build();
    }

    /** CONCEPT 阶段 S 级阻断动作全 DONE，其中 code 换成传入行。 */
    @SuppressWarnings("unchecked")
    private void mockActionsReplacing(StageAction replace) {
        List<StageAction> actions = new ArrayList<>(ActionCatalog.byStage("CONCEPT").stream()
            .filter(d -> d.blocking())
            .map(d -> act(d.code(), "DONE"))
            .toList());
        actions.removeIf(a -> replace.getActionCode().equals(a.getActionCode()));
        actions.add(replace);
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(actions);
    }

    @Test
    @DisplayName("申报阶段为 PLAN 时，CONCEPT 动作的 MISSING 属申报范围内 → 放行（回归钉）")
    void inScopeMissingStillExempted() {
        mockActionsReplacing(missingC11());
        assertThatCode(() -> engine.check(project("PLAN"), "CONCEPT")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("申报阶段与动作同阶段（CONCEPT）：MISSING 不在范围内 → 必须阻断")
    void missingOutsideDeclaredScopeBlocks() {
        mockActionsReplacing(missingC11());
        assertThatThrownBy(() -> engine.check(project("CONCEPT"), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C11");
    }

    @Test
    @DisplayName("非 legacy 项目（declaredStage 为空）带 MISSING 标记 → fail-closed 阻断")
    void missingWithoutDeclaredStageBlocks() {
        mockActionsReplacing(missingC11());
        assertThatThrownBy(() -> engine.check(project(null), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C11");

        mockActionsReplacing(missingC11());
        assertThatThrownBy(() -> engine.check(project("  "), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C11");
    }

    @Test
    @DisplayName("Z 系别名行（Z03→C12，同属 CONCEPT）：豁免判定先归一，不因查不到目录而误阻断")
    void aliasCodeStillResolvesForExemption() {
        StageAction z03 = StageAction.builder().id(9L).projectId(100L).stageId(10L)
            .actionCode("Z03").actionName(ActionCatalog.byCode("C12").name())
            .status("IN_PROGRESS").historyMark(MISSING).build();
        List<StageAction> actions = new ArrayList<>(ActionCatalog.byStage("CONCEPT").stream()
            .filter(d -> d.blocking())
            .filter(d -> !"C12".equals(d.code()))
            .map(d -> act(d.code(), "DONE"))
            .toList());
        actions.add(z03);
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(actions);

        // 申报 PLAN：C12 属 CONCEPT，在范围内 → 放行
        assertThatCode(() -> engine.check(project("PLAN"), "CONCEPT")).doesNotThrowAnyException();

        // 申报 CONCEPT：不在范围内 → 阻断（证明归一后确实进了范围判定，而不是被当成未知码静默处理）
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(actions);
        assertThatThrownBy(() -> engine.check(project("CONCEPT"), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C12");
    }

    @Test
    @DisplayName("可解释清单：范围内 MISSING 说明为历史缺失；范围外说明不在豁免范围且 ok=false")
    void checklistReasonMatchesScope() {
        mockActionsReplacing(missingC11());
        GateChecklistItem inScope = find(engine.explainChecklist(project("PLAN"), "CONCEPT"), "C11");
        assertThat(inScope.ok()).isTrue();
        assertThat(inScope.reason()).contains("历史缺失（BR-PROD-03）");

        mockActionsReplacing(missingC11());
        GateChecklistItem laterDeclared = find(engine.explainChecklist(project("DEV"), "CONCEPT"), "C11");
        assertThat(laterDeclared.ok()).isTrue();
        assertThat(laterDeclared.reason()).contains("历史缺失（BR-PROD-03）");

        mockActionsReplacing(missingC11());
        GateChecklistItem sameStage = find(engine.explainChecklist(project("CONCEPT"), "CONCEPT"), "C11");
        assertThat(sameStage.ok()).isFalse();
        assertThat(sameStage.reason())
            .contains("不在豁免范围")
            .contains("按未完成处理");

        mockActionsReplacing(missingC11());
        assertThat(find(engine.explainChecklist(project(null), "CONCEPT"), "C11").ok()).isFalse();
    }

    private static GateChecklistItem find(GateChecklistView view, String code) {
        Optional<GateChecklistItem> hit = view.items().stream()
            .filter(i -> code.equals(i.code())).findFirst();
        assertThat(hit).as("清单应包含 " + code).isPresent();
        return hit.get();
    }
}
