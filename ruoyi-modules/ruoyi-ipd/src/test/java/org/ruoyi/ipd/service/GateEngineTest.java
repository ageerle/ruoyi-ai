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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-5 门禁引擎（BR-IPD-06）：S 全阻断 38 项；A 超管配置未配置从严；B 仅 Gate 关联+P10/V02+C12。
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
            .actionCode(code).actionName("t").status(status).build();
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

    @Test
    @DisplayName("S 级必做集 = 全部阻断动作 38 项（目录全集锚定）")
    void sLevelRequiredIs38() {
        assertThat(engine.requiredCodes("S")).hasSize(38);
    }

    @Test
    @DisplayName("S 级阻断动作未完成拒绝且消息带逐项清单；全 DONE/NA 放行")
    void sLevelBlockingGate() {
        mockActions(act("C11", "IN_PROGRESS"), act("C12", "DONE"));
        Project p = project("S");
        assertThatThrownBy(() -> engine.check(p, "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C11");
        mockActions(act("C11", "DONE"), act("C12", "DONE"), act("P10", "NA"));
        assertThatCode(() -> engine.check(p, "CONCEPT")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A 级：超管配置集生效（配置 C11 则必做集恰为 C11）")
    void aLevelUsesAdminConfig() {
        when(configService.getValue(GateEngine.A_LEVEL_CONFIG_KEY, "")).thenReturn("C11");
        assertThat(engine.requiredCodes("A")).containsExactly("C11");
    }

    @Test
    @DisplayName("A 级：未配置从严回落 S 全集 38 项（不造无源数据，默认最严）")
    void aLevelUnconfiguredFallsBackToS() {
        when(configService.getValue(GateEngine.A_LEVEL_CONFIG_KEY, "")).thenReturn("");
        assertThat(engine.requiredCodes("A")).hasSize(38);
    }

    @Test
    @DisplayName("B 级必做集 = Gate 关联 5 项 + P10/V02/C12（v3 规则语义）")
    void bLevelRequiredSet() {
        assertThat(engine.requiredCodes("B"))
            .containsExactlyInAnyOrder("C11", "P13", "D05", "L07", "LC02", "P10", "V02", "C12");
    }

    @Test
    @DisplayName("AC-IPD-15：B 级项目 C12 未完成跳阶拒绝（全等级阻断）")
    void bLevelC12Blocks() {
        mockActions(
            act("C11", "DONE"), act("P13", "DONE"), act("D05", "DONE"),
            act("L07", "DONE"), act("LC02", "DONE"), act("P10", "DONE"),
            act("V02", "DONE"), act("C12", "IN_PROGRESS"));
        assertThatThrownBy(() -> engine.check(project("B"), "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("C12");
    }

    @Test
    @DisplayName("B 级非必做动作（C01）未完成不拦截门禁")
    void bLevelIgnoresNonRequired() {
        mockActions(
            act("C01", "IN_PROGRESS"),
            act("C11", "DONE"), act("P13", "DONE"), act("D05", "DONE"),
            act("L07", "DONE"), act("LC02", "DONE"), act("P10", "DONE"),
            act("V02", "DONE"), act("C12", "DONE"));
        assertThatCode(() -> engine.check(project("B"), "CONCEPT")).doesNotThrowAnyException();
    }
}