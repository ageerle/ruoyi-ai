package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ZK-IPD §三.2 完整公式一致性测试（BonusPool 反向验证 2026-09-06）
 *
 * <p>ZK-IPD Prompt §三.2 项目奖金池规则：
 * <pre>
 *   1. 奖金池计算公式：上市后连续 6 个月实际回款金额 × 5% × 项目 S/A/B 差异化系数。
 *   2. 统计口径：以企业回款金额为唯一核算基数，上市 6 个月周期结束后产生的退款，不回溯扣减奖金。
 *   3. 项目等级：创建时选定 S/A/B 等级，项目中期支持修改，修改操作全程记录审计日志。
 *   4. 分配比例：市场 PM 占比 40%-65%、研发 PM 占比 35%-60%，上市 90 天复盘后由双 PM + 上级三方最终评定。
 *   5. <b>修正因子：奖金可叠加销售达成率阶梯系数、个人绩效系数。</b>
 *   6. 项目移交适配：无论单 PM 或双 PM 同时移交，完整保留所有台账...
 * </pre>
 *
 * <p>§三.2.5 关键洞察："可叠加" 意为 {@code finalPool = actualReceipts × 5% × levelCoefficient
 * × tierCoefficient × personalCoefficient}。原 fb4a86d3 实现只覆盖 §三.2.1 主公式，
 * 未实现 §三.2.5 修正因子叠加，本测试验证完整公式。
 *
 * <p>差异矩阵 2026-09-06 P0 项（反向验证）：BonusPool.tierCoefficient 字段已建模（域层），
 * 但 §三.2.5 修正因子叠加未在服务层实现——"绿但对应错误实现" 的典型样例。
 *
 * @see <a href="https://ZK-IPD 设计稿仓">IPD产品经理管理系统·最终完整版AI开发Prompt（全规则闭环无遗留疑问）.md §三.2.5</a>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BonusPoolZkFormulaFullTest {

    @Mock
    private BonusPoolMapper bonusPoolMapper;
    @Mock
    private ProjectMapper projectMapper;

    private BonusPoolService service;

    @BeforeEach
    void setUp() {
        service = new BonusPoolService(bonusPoolMapper, projectMapper);
    }

    /* ----------------- §三.2.1 主公式 + §三.2.5 修正因子（完整 4 因子叠加） ----------------- */

    @Test
    @DisplayName("§三.2.5 完整公式：实际回款 × 5% × S/A/B × 销售达成率阶梯 × 个人绩效 = 4 因子全叠加")
    void fullFormula_sLevel_tierCoefficient_personalCoefficient_allStacked() {
        // S 级 1.8 + 销售达成率 100%（命中 1.0 档）+ 个人绩效 1.2
        BigDecimal actualReceipts = new BigDecimal("10000000");
        BigDecimal levelCoefficient = new BigDecimal("1.8");
        BigDecimal tierCoefficient = new BigDecimal("1.0"); // 达成率 100% → tier=1.0
        BigDecimal personalCoefficient = new BigDecimal("1.2");

        BigDecimal pool = service.calculateBonusPoolByZkFormulaWithModifiers(
            actualReceipts, levelCoefficient, tierCoefficient, personalCoefficient);

        // 10000000 × 0.05 × 1.8 × 1.0 × 1.2 = 1080000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("1080000"));
    }

    @Test
    @DisplayName("§三.2.5 B 级 0.6 × 达成率 80%（命中 0.8 档） × 个人绩效 0.9")
    void fullFormula_bLevel_80Percent_tierCoefficient_personalCoefficient() {
        BigDecimal actualReceipts = new BigDecimal("5000000");
        BigDecimal levelCoefficient = new BigDecimal("0.6");   // B 级
        BigDecimal tierCoefficient = new BigDecimal("0.8");    // 达成率 80%→0.8
        BigDecimal personalCoefficient = new BigDecimal("0.9"); // 个人绩效 0.9

        BigDecimal pool = service.calculateBonusPoolByZkFormulaWithModifiers(
            actualReceipts, levelCoefficient, tierCoefficient, personalCoefficient);

        // 5000000 × 0.05 × 0.6 × 0.8 × 0.9 = 108000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("108000"));
    }

    @Test
    @DisplayName("§三.2.5 A 级 1.0 × 达成率 60%（命中 0.3 档） × 个人绩效 1.0")
    void fullFormula_aLevel_60Percent_tierCoefficient_personalCoefficient() {
        BigDecimal actualReceipts = new BigDecimal("8000000");
        BigDecimal levelCoefficient = new BigDecimal("1.0");
        BigDecimal tierCoefficient = new BigDecimal("0.3"); // 达成率 60%→0.3
        BigDecimal personalCoefficient = new BigDecimal("1.0");

        BigDecimal pool = service.calculateBonusPoolByZkFormulaWithModifiers(
            actualReceipts, levelCoefficient, tierCoefficient, personalCoefficient);

        // 8000000 × 0.05 × 1.0 × 0.3 × 1.0 = 120000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("120000"));
    }

    @Test
    @DisplayName("§三.2.5 零回款 ⇒ 奖金池为 0（不触发除零，不报错）")
    void fullFormula_zeroReceipts_returnsZero() {
        BigDecimal pool = service.calculateBonusPoolByZkFormulaWithModifiers(
            BigDecimal.ZERO, new BigDecimal("1.8"), new BigDecimal("1.0"), new BigDecimal("1.2"));

        assertThat(pool).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("§三.2.5 tierCoefficient=0（达成率<50%） ⇒ 奖金池为 0，不发放")
    void fullFormula_tierCoefficientZero_noPayment() {
        // §三.2.1 + AC-INC-21：达成率 45% 命中 0 档，不发放
        BigDecimal actualReceipts = new BigDecimal("10000000");
        BigDecimal levelCoefficient = new BigDecimal("1.8");
        BigDecimal tierCoefficient = BigDecimal.ZERO; // 达成率<50% → 0
        BigDecimal personalCoefficient = new BigDecimal("1.2");

        BigDecimal pool = service.calculateBonusPoolByZkFormulaWithModifiers(
            actualReceipts, levelCoefficient, tierCoefficient, personalCoefficient);

        // 10000000 × 0.05 × 1.8 × 0 × 1.2 = 0
        assertThat(pool).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("§三.2.5 空入参抛 ServiceException（fail-loud 不 fail-silent）")
    void fullFormula_nullReceipts_throws() {
        assertThatThrownBy(() -> service.calculateBonusPoolByZkFormulaWithModifiers(
                null, new BigDecimal("1.8"), new BigDecimal("1.0"), new BigDecimal("1.2")))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("回款");
    }

    @Test
    @DisplayName("§三.2.5 负回款抛 ServiceException")
    void fullFormula_negativeReceipts_throws() {
        assertThatThrownBy(() -> service.calculateBonusPoolByZkFormulaWithModifiers(
                new BigDecimal("-100"), new BigDecimal("1.8"), new BigDecimal("1.0"), new BigDecimal("1.2")))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("回款");
    }

    /* ----------------- §三.2.5 + §三.2.3 联动：buildPoolFromProject 应同时回填 tierCoefficient ----------------- */

    @Test
    @DisplayName("§三.2.5+§三.2.3 联动：buildPoolFromProject 同时回填 levelCoefficient + tierCoefficient(由 achievementRate 推出)")
    void buildPoolFromProject_stacksTierCoefficientFromAchievementRate() {
        // Arrange: 项目 S 级 1.8 + 销售达成率 100% → tierCoefficient = 1.0
        Project project = new Project();
        project.setId(200L);
        project.setLevel("S");
        project.setLevelCoefficient(new BigDecimal("1.8"));
        when(projectMapper.selectById(200L)).thenReturn(project);

        // Act: 用项目 + 实际回款 + 达成率构造 BonusPool
        BonusPool pool = service.buildPoolFromProjectWithAchievement(
            200L,
            new BigDecimal("10000000"),
            new BigDecimal("100"),  // 达成率 100%
            new BigDecimal("1.2"),  // 个人绩效 1.2
            new Date(),
            new BigDecimal("0.05"));

        // Assert: coefficient = levelCoefficient, tierCoefficient 由达成率 100% 推出 1.0
        assertThat(pool.getCoefficient()).isEqualByComparingTo(new BigDecimal("1.8"));
        assertThat(pool.getTierCoefficient()).isEqualByComparingTo(new BigDecimal("1.0"));
        // finalPool = 10000000 × 0.05 × 1.8 × 1.0 × 1.2 = 1080000
        assertThat(pool.getFinalPool()).isEqualByComparingTo(new BigDecimal("1080000"));
    }

    @Test
    @DisplayName("§三.2.5+§三.2.3 联动：达成率 45%（命中 0 档） ⇒ finalPool = 0")
    void buildPoolFromProject_tierCoefficientZero_noPayment() {
        Project project = new Project();
        project.setId(300L);
        project.setLevel("S");
        project.setLevelCoefficient(new BigDecimal("1.8"));
        when(projectMapper.selectById(300L)).thenReturn(project);

        BonusPool pool = service.buildPoolFromProjectWithAchievement(
            300L,
            new BigDecimal("10000000"),
            new BigDecimal("45"),  // 达成率 45% → tierCoefficient = 0
            new BigDecimal("1.2"),
            new Date(),
            new BigDecimal("0.05"));

        assertThat(pool.getTierCoefficient()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(pool.getFinalPool()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /* ----------------- §三.2.5 个人绩效系数缺省为 1.0（中性默认） ----------------- */

    @Test
    @DisplayName("§三.2.5 默认中性：个人绩效系数缺省 1.0 ⇒ 与原 §三.2.1 主公式等价")
    void fullFormula_personalCoefficientDefaultsToOne_matchesMainFormula() {
        BigDecimal actualReceipts = new BigDecimal("10000000");
        BigDecimal levelCoefficient = new BigDecimal("1.8");
        BigDecimal tierCoefficient = new BigDecimal("1.0");

        // tierCoefficient=1.0, personalCoefficient 缺省=1.0
        BigDecimal pool = service.calculateBonusPoolByZkFormulaWithModifiers(
            actualReceipts, levelCoefficient, tierCoefficient, null);

        // 10000000 × 0.05 × 1.8 × 1.0 × 1.0 = 900000
        assertThat(pool).isEqualByComparingTo(new BigDecimal("900000"));
    }
}
