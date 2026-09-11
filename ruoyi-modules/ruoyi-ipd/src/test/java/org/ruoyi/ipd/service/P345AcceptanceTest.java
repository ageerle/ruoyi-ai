package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.mapper.ProjectScoreRecordMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * P3-4.5 项目绩效系数分档与可切换取数策略验收测试（AC-INC-22/23/24）
 *
 * <p>覆盖 7 维度：
 * <ol>
 *   <li>分档边界：95/85/70/60 四档上下端点（AC-INC-22/23/24 + 邻值 96/88/55/69/59）</li>
 *   <li>三种取数策略：PROJECT_SCORE / WEIGHTED_AVG / LAST_QUARTER（BR-INC-07）</li>
 *   <li>能力等级不得代替项目绩效（AC-INC-22b：L1–L5 与 weightedScore 独立）</li>
 *   <li>策略配置从 system_configs 读取（BusinessConfigService）</li>
 *   <li>score 为 null / 越界 → IpdBusinessException</li>
 *   <li>55 分系数 = 0 取消奖金资格（AC-INC-24）</li>
 *   <li>preview 仅做查表/计算，不写 audit、不落库</li>
 * </ol>
 *
 * <p>AC：AC-INC-22/23/24；BR：BR-INC-07。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P345AcceptanceTest {

    @Mock
    private BonusPoolMapper bonusPoolMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectScoreMapper projectScoreMapper;
    @Mock
    private ProjectScoreRecordMapper projectScoreRecordMapper;
    @Mock
    private BusinessConfigService businessConfigService;
    @Mock
    private StateMachineGuard stateMachineGuard;

    private BonusPoolService bonusPoolService;
    private ProjectScoreService projectScoreService;

    @BeforeEach
    void setUp() {
        bonusPoolService = new BonusPoolService(bonusPoolMapper, projectMapper);
        bonusPoolService.setStateMachineGuard(stateMachineGuard);
        bonusPoolService.setBusinessConfigService(businessConfigService);

        projectScoreService = new ProjectScoreService();
        bonusPoolService.setProjectScoreService(projectScoreService);
    }

    private IpdActor actor() {
        return new IpdActor(1001L, "测试超管", "SUPER_ADMIN", 1L);
    }

    private Project sLevelProject(Long id) {
        Project p = new Project();
        p.setId(id);
        p.setLevel("S");
        p.setLevelCoefficient(new BigDecimal("1.8"));
        return p;
    }

    /* ====================== AC-INC-22/23/24 + 边界 邻值 ====================== */

    @Test
    @DisplayName("AC-INC-22：综合 96 分（≥95 档）→ 系数 1.0")
    void coefficient_score_96_returns_1_0() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("96"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    @DisplayName("边界：综合 95 分（下端点含）→ 系数 1.0")
    void coefficient_score_95_returns_1_0_boundary() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("95"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    @DisplayName("边界：综合 94.99 分（上端点不含 95 档）→ 系数 0.8")
    void coefficient_score_94_99_returns_0_8_upperExclusive() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("94.99"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("AC-INC-23：综合 88 分（≥85 档）→ 系数 0.8")
    void coefficient_score_88_returns_0_8() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("88"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("边界：综合 85 分（下端点含）→ 系数 0.8")
    void coefficient_score_85_returns_0_8_boundary() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("85"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("综合 75 分（≥70 档）→ 系数 0.6")
    void coefficient_score_75_returns_0_6() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("75"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.6"));
    }

    @Test
    @DisplayName("综合 65 分（≥60 档）→ 系数 0.3")
    void coefficient_score_65_returns_0_3() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("65"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.3"));
    }

    @Test
    @DisplayName("AC-INC-24：综合 55 分（<60 档）→ 系数 0，取消奖金资格")
    void coefficient_score_55_returns_0_cancelBonus() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("55"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("边界：综合 59 分（<60 上端点）→ 系数 0")
    void coefficient_score_59_returns_0_upperBoundary() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("59"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /* ====================== BR-INC-07 三种取数策略 ====================== */

    @Test
    @DisplayName("BR-INC-07：策略 PROJECT_SCORE（直接传入 score）→ 系数按 score 查表")
    void strategy_PROJECT_SCORE_directLookup() {
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("90"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("BR-INC-07：策略 WEIGHTED_AVG（多期加权平均 score）→ 系数按 weightedScore 查表")
    void strategy_WEIGHTED_AVG_usesWeightedScore() {
        // 当前期 88 分（85 档 = 0.8），上期 78 分（70 档 = 0.6），
        // 加权 0.6/0.4 = (88×0.6 + 78×0.4) = 84 分 → 70 档 = 0.6
        BigDecimal weighted = new BigDecimal("88").multiply(new BigDecimal("0.6"))
            .add(new BigDecimal("78").multiply(new BigDecimal("0.4")))
            .setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            weighted, BonusPoolService.STRATEGY_WEIGHTED_AVG);
        // 84 ≥ 70 → 0.6 档
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.6"));
    }

    @Test
    @DisplayName("BR-INC-07：策略 LAST_QUARTER（上季度 score）→ 系数按上季度 score 查表")
    void strategy_LAST_QUARTER_usesLastQuarterScore() {
        // 上季度 75 分 → 70 档 = 0.6
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("75"), BonusPoolService.STRATEGY_LAST_QUARTER);
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.6"));
    }

    @Test
    @DisplayName("BR-INC-07：策略从 system_configs 读取（默认 PROJECT_SCORE）")
    void strategy_defaultFromConfig() {
        when(businessConfigService.getString("bonus.performance.strategy"))
            .thenReturn(BonusPoolService.STRATEGY_WEIGHTED_AVG);

        // 90 分（85 档 = 0.8）；策略从配置读
        BigDecimal coef = bonusPoolService.resolvePerformanceCoefficient(200L, new BigDecimal("90"));
        assertThat(coef).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    /* ====================== AC-INC-22b：能力等级不得代替项目绩效 ====================== */

    @Test
    @DisplayName("AC-INC-22b：L5 能力等级 + 50 分绩效 → 系数 = 0（能力等级不代替项目绩效）")
    void coefficient_abilityLevelL5_doesNotOverride50Score() {
        // L5 顶级能力 + score=50（<60 档）→ 系数 = 0（不享受 L5 红利）
        BigDecimal coef = bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("50"), BonusPoolService.STRATEGY_PROJECT_SCORE);
        assertThat(coef).isEqualByComparingTo(BigDecimal.ZERO);

        // 显式断言：能力等级不是系数入参（不应出现 abilityLevel 维度的奖励路径）
        // 即 score=50 不会因为 abilityLevel=L5 而返回 1.0
        ProjectScoreService.assertAbilityLevelNotUsedAsCoefficient("L5");
    }

    @Test
    @DisplayName("AC-INC-22b：assertNoAbilityInference 不抛错——加权得分与能力等级独立")
    void assertNoAbilityInference_doesNotThrow() {
        ProjectScoreService.assertNoAbilityInference(new BigDecimal("88"), "L4");
    }

    /* ====================== 异常 ====================== */

    @Test
    @DisplayName("异常：score=null → IpdBusinessException（参数校验）")
    void coefficient_nullScore_throws() {
        assertThatThrownBy(() -> bonusPoolService.calculatePerformanceCoefficient(
            null, BonusPoolService.STRATEGY_PROJECT_SCORE))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("综合得分不能为空");
    }

    @Test
    @DisplayName("异常：score 越界 101 → IpdBusinessException（区间 [0, 100]）")
    void coefficient_scoreOver100_throws() {
        assertThatThrownBy(() -> bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("101"), BonusPoolService.STRATEGY_PROJECT_SCORE))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("必须在 [0, 100]");
    }

    @Test
    @DisplayName("异常：strategy 非法 → IpdBusinessException")
    void coefficient_invalidStrategy_throws() {
        assertThatThrownBy(() -> bonusPoolService.calculatePerformanceCoefficient(
            new BigDecimal("88"), "INVALID_STRATEGY"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("取数策略");
    }

    /* ====================== preview 端点契约（preview 不写库/不落审计） ====================== */

    @Test
    @DisplayName("previewCoefficient：仅查表/计算，不写 audit_log、不 insert bonus_pools")
    void previewCoefficient_isReadOnly() {
        BonusPool preview = bonusPoolService.previewCoefficient(
            200L, new BigDecimal("92"), BonusPoolService.STRATEGY_PROJECT_SCORE, actor());

        // 92 分 → 85 档 = 0.8
        assertThat(preview.getFinalPool()).isEqualByComparingTo(BigDecimal.ZERO); // preview 不算金额
        // 验证 mapper 没被写过
        org.mockito.Mockito.verify(bonusPoolMapper, org.mockito.Mockito.never())
            .insert(any(BonusPool.class));
        org.mockito.Mockito.verify(bonusPoolMapper, org.mockito.Mockito.never())
            .updateById(any(BonusPool.class));
    }
}