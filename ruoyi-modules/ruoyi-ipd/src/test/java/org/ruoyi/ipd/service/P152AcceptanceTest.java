package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.GateChecklistView;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-5.2：S/A/B 必做集配置校验与可解释清单（AC-IPD-10/11）。
 */
@Tag("dev")
class P152AcceptanceTest {

    private StageActionMapper actionMapper;
    private SystemConfigService configService;
    private GateEngine engine;

    @BeforeEach
    void setUp() {
        actionMapper = mock(StageActionMapper.class);
        configService = mock(SystemConfigService.class);
        engine = new GateEngine(actionMapper, configService);
    }

    private Project project(String level, String stage) {
        Project p = new Project();
        p.setId(200L);
        p.setLevel(level);
        p.setCurrentStage(stage);
        return p;
    }

    @Test
    @DisplayName("S 级必做 38；B 级权威 10 不硬凑 14")
    void sAndBSizes() {
        assertThat(engine.requiredCodes("S")).hasSize(38);
        assertThat(engine.requiredCodes("B"))
            .containsExactlyElementsOf(ActionCatalog.B_LEVEL_BLOCKING_CODES)
            .hasSize(10);
    }

    @Test
    @DisplayName("A 级配置 trim/去重/别名；未知码拒绝")
    void aLevelNormalizeAndRejectUnknown() {
        when(configService.getValue(GateEngine.A_LEVEL_CONFIG_KEY, ""))
            .thenReturn(" C11 , C11 , Z03 ");
        assertThat(engine.requiredCodes("A")).containsExactly("C11", "C12");

        assertThatThrownBy(() -> GateEngine.normalizeALevelConfig("C11,NOPE"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未知");

        assertThat(GateEngine.validateAndNormalizeALevelConfigValue(" Z03 , C11 "))
            .isEqualTo("C12,C11");
    }

    @Test
    @DisplayName("可解释清单含逐项 reason + configVersion")
    void explainChecklist() {
        when(configService.getValue(GateEngine.A_LEVEL_CONFIG_KEY, "")).thenReturn("C11");
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
            java.util.List.of(StageAction.builder().actionCode("C11").actionName("立项决策评审")
                .status("IN_PROGRESS").build()));

        GateChecklistView view = engine.explainChecklist(project("A", "CONCEPT"), "CONCEPT");
        assertThat(view.configVersion()).isEqualTo("A:C11");
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).code()).isEqualTo("C11");
        assertThat(view.items().get(0).ok()).isFalse();
        assertThat(view.items().get(0).reason()).contains("未完成").contains("超管配置");
    }

    @Test
    @DisplayName("S 级 CONCEPT 清单 10 项且含来源说明")
    void sConceptExplain() {
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(java.util.List.of());
        GateChecklistView view = engine.explainChecklist(project("S", "CONCEPT"), null);
        assertThat(view.configVersion()).isEqualTo("S:blocking-38");
        assertThat(view.items()).hasSize(10);
        assertThat(view.items()).allSatisfy(i -> {
            assertThat(i.ok()).isFalse();
            assertThat(i.reason()).contains("未实例化");
        });
    }
}
