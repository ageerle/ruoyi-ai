package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ZK-IPD 奖金池公式一致性测试（差异矩阵 2026-09-06 P0 项）
 *
 * <p>ZK-IPD Prompt §三.2.1 权威原文："奖金池计算公式：上市后连续 6 个月
 * <b>实际回款金额</b> × 5% × <b>项目 S/A/B 差异化系数</b>"。
 *
 * <p>后端现状（差异矩阵 §6）：
 * <ul>
 *   <li>BonusPoolService.calculateBasePool 用 <b>目标销售额</b> × 5%</li>
 *   <li>BonusPoolService.calculateFinalPool 用 basePool × <b>达成率阶梯系数</b></li>
 *   <li>BonusPool 实体虽已建模 coefficient（S/A/B 等级系数）字段，但服务层零引用</li>
 * </ul>
 *
 * <p>本测试断言 ZK-IPD 公式应被正确实现：{@code finalPool = actualReceipts × 0.05 × levelCoefficient}
 *
 * <p>当前为 RED（方法不存在），实现后转 GREEN。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BonusPoolZkFormulaTest {

    @Mock
    private BonusPoolMapper bonusPoolMapper;
    @Mock
    private ProjectMapper projectMapper;

    private BonusPoolService service;

    @BeforeEach
    void setUp() {
        service = new BonusPoolService(bonusPoolMapper, projectMapper);
    }

    @Test
    @DisplayName("ZK-IPD §三.2.1：奖金池 = 实际回款 × 5% × S/A/B 系数（test path 1）")
    void zkIpdFormula_sLevel_actualReceipts_1_8() {
        // S 级项目，levelCoefficient=1.8；6 个月实际回款 1000 万
        BigDecimal actualReceipts = new BigDecimal("10000000");
        BigDecimal levelCoefficient = new BigDecimal("1.8");

        BigDecimal pool = service.calculateBonusPoolByZkFormula(actualReceipts, levelCoefficient);

        // 10000000 × 0.05 × 1.8 = 900000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("900000"));
    }

    @Test
    @DisplayName("ZK-IPD §三.2.1：奖金池 = 实际回款 × 5% × B 级系数 0.6")
    void zkIpdFormula_bLevel_actualReceipts_0_6() {
        BigDecimal actualReceipts = new BigDecimal("5000000");
        BigDecimal levelCoefficient = new BigDecimal("0.6");

        BigDecimal pool = service.calculateBonusPoolByZkFormula(actualReceipts, levelCoefficient);

        // 5000000 × 0.05 × 0.6 = 150000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("150000"));
    }

    @Test
    @DisplayName("ZK-IPD §三.2.1：A 级固定系数 1.0（v3 G1 特殊规则）")
    void zkIpdFormula_aLevel_fixedCoefficient_1_0() {
        BigDecimal actualReceipts = new BigDecimal("8000000");
        BigDecimal levelCoefficient = new BigDecimal("1.0");

        BigDecimal pool = service.calculateBonusPoolByZkFormula(actualReceipts, levelCoefficient);

        // 8000000 × 0.05 × 1.0 = 400000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("400000"));
    }

    @Test
    @DisplayName("ZK-IPD §三.2.1：实际回款为 0 ⇒ 奖金池为 0（不触发除零）")
    void zkIpdFormula_zeroReceipts_returnsZero() {
        BigDecimal pool = service.calculateBonusPoolByZkFormula(
            BigDecimal.ZERO, new BigDecimal("1.5"));

        assertThat(pool).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("ZK-IPD §三.2.1：空入参抛 ServiceException 不返回 0（fail-loud 不 fail-silent）")
    void zkIpdFormula_nullReceipts_throws() {
        try {
            service.calculateBonusPoolByZkFormula(null, new BigDecimal("1.5"));
            assertThat(false).as("期望抛异常").isTrue();
        } catch (RuntimeException expected) {
            // 预期路径
            assertThat(expected.getMessage()).contains("回款");
        }
    }

    @Test
    @DisplayName("ZK-IPD §三.2.1：负回款（应不出现于生产；防御编程）抛异常，不静默转 0")
    void zkIpdFormula_negativeReceipts_throws() {
        try {
            service.calculateBonusPoolByZkFormula(new BigDecimal("-100"), new BigDecimal("1.5"));
            assertThat(false).as("期望抛异常").isTrue();
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).contains("回款");
        }
    }

    @Test
    @DisplayName("差异矩阵 P0 项：BonusPool.coefficient 应取自 Project.levelCoefficient（非 tierCoefficient）")
    void bonusPoolCoefficientFromProjectLevelCoefficient() {
        // Arrange: 项目 S 级，levelCoefficient=1.8
        Project project = new Project();
        project.setId(100L);
        project.setLevel("S");
        project.setLevelCoefficient(new BigDecimal("1.8"));
        when(projectMapper.selectById(100L)).thenReturn(project);

        // Act: 用项目 + 实际回款构造 BonusPool，应填 coefficient=1.8
        BonusPool pool = service.buildPoolFromProject(100L,
            new BigDecimal("10000000"),
            new Date(),
            new BigDecimal("0.05"));

        // Assert: coefficient = project.levelCoefficient（不是 tierCoefficient）
        assertThat(pool.getCoefficient()).isEqualByComparingTo(new BigDecimal("1.8"));
        assertThat(pool.getProjectId()).isEqualTo(100L);
        // finalPool = 10000000 × 0.05 × 1.8 = 900000
        assertThat(pool.getFinalPool()).isEqualByComparingTo(new BigDecimal("900000"));
    }
}
