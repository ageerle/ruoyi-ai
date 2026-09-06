package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-5.1 门禁：当前阶段过滤、缺失拒绝、轻管不阻断、未来阶段不阻塞。
 */
@Tag("dev")
class GateEngineTest {

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

    private Project project(String level) {
        Project p = new Project();
        p.setId(100L);
        p.setLevel(level);
        p.setCurrentStage("CONCEPT");
        return p;
    }

    @SuppressWarnings("unchecked")
    private void mockActions(StageAction... actions) {
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(actions));
    }

    /** CONCEPT 阶段 S 级全部阻断动作标 DONE（供放行场景）。 */
    private StageAction[] conceptBlockingDone() {
        return ActionCatalog.byStage("CONCEPT").stream()
            .filter(d -> d.blocking())
            .map(d -> act(d.code(), "DONE"))
            .toArray(StageAction[]::new);
    }

    @Test
    @DisplayName("S 级必做集 = 全部阻断动作 38 项")
    void sLevelRequiredIs38() {
        assertThat(engine.requiredCodes("S")).hasSize(38);
    }

    @Test
    @DisplayName("CONCEPT 阶段 S 级应做集 = 10 项阻断")
    void conceptStageRequiredIs10() {
        assertThat(engine.requiredCodesForStage("S", "CONCEPT")).hasSize(10);
    }

    @Test
    @DisplayName("S 级当前阶段阻断未完成拒绝且消息带清单；全 DONE 放行")
    void sLevelBlockingGate() {
        List<StageAction> partial = new ArrayList<>(List.of(conceptBlockingDone()));
        partial.removeIf(a -> "C11".equals(a.getActionCode()));
        partial.add(act("C11", "IN_PROGRESS"));
        mockActions(partial.toArray(StageAction[]::new));
        assertThatThrownBy(() -> engine.check(project("S"), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C11");
        mockActions(conceptBlockingDone());
        assertThatCode(() -> engine.check(project("S"), "CONCEPT")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("AC-IPD-07：仅轻管未完成不阻断 CONCEPT 跳阶")
    void lightIncompleteDoesNotBlock() {
        List<StageAction> mixed = new ArrayList<>(List.of(conceptBlockingDone()));
        mixed.add(act("C05", "NOT_STARTED"));
        mockActions(mixed.toArray(StageAction[]::new));
        assertThatCode(() -> engine.check(project("S"), "CONCEPT")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("未来阶段阻断未完成不阻塞当前 CONCEPT 跳阶")
    void futureStageDoesNotBlockCurrent() {
        List<StageAction> mixed = new ArrayList<>(List.of(conceptBlockingDone()));
        mixed.add(act("P12", "NOT_STARTED"));
        mixed.add(act("D05", "NOT_STARTED"));
        mixed.add(act("V02", "NOT_STARTED"));
        mockActions(mixed.toArray(StageAction[]::new));
        assertThatCode(() -> engine.check(project("S"), "CONCEPT")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("缺失/未实例化必做动作拒绝")
    void missingRequiredRejects() {
        mockActions(act("C11", "DONE"), act("C12", "DONE"));
        assertThatThrownBy(() -> engine.check(project("S"), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未实例化");
    }

    @Test
    @DisplayName("A 级：超管配置集生效")
    void aLevelUsesAdminConfig() {
        when(configService.getValue(GateEngine.A_LEVEL_CONFIG_KEY, "")).thenReturn("C11");
        assertThat(engine.requiredCodes("A")).containsExactly("C11");
        mockActions(act("C11", "DONE"));
        assertThatCode(() -> engine.check(project("A"), "CONCEPT")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A 级：未配置从严回落 S 全集 38 项")
    void aLevelUnconfiguredFallsBackToS() {
        when(configService.getValue(GateEngine.A_LEVEL_CONFIG_KEY, "")).thenReturn("");
        assertThat(engine.requiredCodes("A")).hasSize(38);
    }

    @Test
    @DisplayName("B 级必做集 = ActionCatalog 权威 10 项")
    void bLevelRequiredSet() {
        assertThat(engine.requiredCodes("B"))
            .containsExactlyElementsOf(ActionCatalog.B_LEVEL_BLOCKING_CODES)
            .hasSize(10);
    }

    @Test
    @DisplayName("AC-IPD-15：B 级 CONCEPT 下 C12 未完成跳阶拒绝")
    void bLevelC12Blocks() {
        mockActions(act("C11", "DONE"), act("C12", "IN_PROGRESS"));
        assertThatThrownBy(() -> engine.check(project("B"), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C12");
    }

    @Test
    @DisplayName("AC-IPD-09：VALID 阶段 V02 未完成拒绝（轻管但阻断）")
    void v02BlocksOnValid() {
        List<StageAction> validRequired = ActionCatalog.byStage("VALID").stream()
            .filter(d -> engine.requiredCodes("S").contains(d.code()))
            .map(d -> act(d.code(), "V02".equals(d.code()) ? "NOT_STARTED" : "DONE"))
            .collect(Collectors.toList());
        mockActions(validRequired.toArray(StageAction[]::new));
        Project p = project("S");
        p.setCurrentStage("VALID");
        assertThatThrownBy(() -> engine.check(p, "VALID"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("V02");
    }

    @Test
    @DisplayName("B 级非必做动作未完成不拦截")
    void bLevelIgnoresNonRequired() {
        mockActions(act("C01", "IN_PROGRESS"), act("C11", "DONE"), act("C12", "DONE"));
        assertThatCode(() -> engine.check(project("B"), "CONCEPT")).doesNotThrowAnyException();
    }
}
