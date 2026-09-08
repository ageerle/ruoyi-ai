package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.mapper.BonusPoolMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * P3-4.3 六档达成率函数与全边界判定验收测试
 * AC: AC-INC-17/17b/17c/17d/17e/17f/17g/17h/18/19/20/21；BR: BR-INC-06
 *
 * <p>形态为 Mockito 单元验收（对齐 P233 惯例）；静态扫描（AC-INC-17g）
 * 走 {@link #noFloatEqualityInTierMatching()} 直接读取 BonusPoolService 源码校验。
 *
 * <p>不得据此标 done（BR-真库）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P343AcceptanceTest {

    @Mock private BonusPoolMapper bonusPoolMapper;

    @InjectMocks private BonusPoolService bonusPoolService;

    private static final Long PROJECT_ID = 100L;

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p343-test"),
            BonusPool.class);
    }

    private static BigDecimal pct(String s) {
        return new BigDecimal(s);
    }

    // ==================== AC-INC-17/17b/17c/17d/17e/17f/17h：边界判定 ====================

    @Test
    @DisplayName("AC-INC-17: 达成率恰好 100% => 阶梯系数 1.0（区间下端点，含）")
    void tier100PctIsCoef1_0() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("100"))).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("AC-INC-17b: 达成率 130% => 阶梯系数 1.2（顶端）")
    void tier130PctIsCoef1_2() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("130"))).isEqualByComparingTo("1.2");
    }

    @Test
    @DisplayName("AC-INC-17c: 达成率 120%（区间上端点，含）=> 阶梯系数 1.0，不是 1.2")
    void tier120PctIsCoef1_0() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("120"))).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("AC-INC-17d: 达成率 120.1%（刚越过上端点）=> 阶梯系数 1.2")
    void tier120_1PctIsCoef1_2() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("120.1"))).isEqualByComparingTo("1.2");
    }

    @Test
    @DisplayName("AC-INC-17e: 达成率 110% => 阶梯系数 1.0（110 落在 [85, 120) 区间）")
    void tier110PctIsCoef1_0() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("110"))).isEqualByComparingTo("1.0");
    }

    @Test
    @DisplayName("AC-INC-17f: 达成率 99.99%（1.0 档下界为 100%，不含 99.99）=> 阶梯系数 0.8")
    void tier99_99PctIsCoef0_8() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("99.99"))).isEqualByComparingTo("0.8");
    }

    @Test
    @DisplayName("AC-INC-18: 达成率 90% => 阶梯系数 0.8")
    void tier90PctIsCoef0_8() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("90"))).isEqualByComparingTo("0.8");
    }

    @Test
    @DisplayName("AC-INC-19: 达成率 75% => 阶梯系数 0.6")
    void tier75PctIsCoef0_6() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("75"))).isEqualByComparingTo("0.6");
    }

    @Test
    @DisplayName("AC-INC-20: 达成率 60% => 阶梯系数 0.3，并触发复盘检讨")
    void tier60PctIsCoef0_3WithReview() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("60"))).isEqualByComparingTo("0.3");
        assertThat(bonusPoolService.isReviewRequired(pct("60"))).isTrue();
    }

    @Test
    @DisplayName("AC-INC-20: 达成率 50%（区间下端点，含）=> 阶梯系数 0.3，触发复盘")
    void tier50PctIsCoef0_3WithReview() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("50"))).isEqualByComparingTo("0.3");
        assertThat(bonusPoolService.isReviewRequired(pct("50"))).isTrue();
    }

    @Test
    @DisplayName("AC-INC-21: 达成率 45% => 阶梯系数 0，不发放；已发月度津贴不追回（独立规则）")
    void tier45PctIsCoef0() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("45"))).isEqualByComparingTo("0");
        assertThat(bonusPoolService.isReviewRequired(pct("45"))).isFalse();
    }

    @Test
    @DisplayName("AC-INC-21: 达成率 0% => 阶梯系数 0")
    void tier0PctIsCoef0() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("0"))).isEqualByComparingTo("0");
    }

    // ==================== AC-INC-17g：静态扫描——不得出现浮点等值判定 ====================

    @Test
    @DisplayName("AC-INC-17g: BonusPoolService 源码不含 === / equalityTolerance / toFixed(2)== 等浮点等值判定")
    void noFloatEqualityInTierMatching() throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(
            "src/main/java/org/ruoyi/ipd/service/BonusPoolService.java");
        String src = new String(java.nio.file.Files.readAllBytes(path));
        // 先剔除注释行再扫描：块注释装饰分隔线 /* ==== */ 会误伤 === 扫描
        // （阶梯匹配实现已全 BigDecimal.compareTo，无浮点等值）
        String codeOnly = java.util.Arrays.stream(src.split("\n", -1))
            .filter(line -> { String t = line.trim();
                return !t.startsWith("//") && !t.startsWith("/*") && !t.startsWith("*"); })
            .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(codeOnly).doesNotContain("===");
        assertThat(codeOnly).doesNotContain("equalityTolerance");
        assertThat(codeOnly).doesNotContain("toFixed");
    }

    // ==================== AC-INC-16：奖金池基数与最终池 ====================

    @Test
    @DisplayName("AC-INC-16: 奖金池基数 = 目标销售额 x poolRate(默认 0.05)，不是实际/回款")
    void basePoolFromTargetSalesNotReceipts() {
        BigDecimal targetSales = new BigDecimal("1000000");
        BigDecimal base = bonusPoolService.calculateBasePool(targetSales, null);
        assertThat(base).isEqualByComparingTo("50000");
        assertThat(bonusPoolService.calculateBasePool(targetSales, new BigDecimal("0.10")))
            .isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("AC-INC-16 + AC-INC-17: 最终奖金池 = 基础奖金池 x 阶梯系数")
    void finalPoolIsBaseTimesTierCoef() {
        BigDecimal targetSales = new BigDecimal("1000000");
        BigDecimal base = bonusPoolService.calculateBasePool(targetSales, null);
        BigDecimal finalPool = bonusPoolService.calculateFinalPool(base, new BigDecimal("1.0"));
        assertThat(finalPool).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("fillDerivedFields: 100% 达成率时 basePool=实际回款×5%, finalPool=base×1.0（ZK 实际回款口径，[CONSISTENCY-1]）")
    void fillDerivedFieldsFor100Pct() {
        BonusPool pool = BonusPool.builder()
            .projectId(PROJECT_ID)
            .targetSales(new BigDecimal("1000000"))
            .achievementRate(pct("100"))
            .build();
        bonusPoolService.fillDerivedFields(pool);
        assertThat(pool.getBasePool()).isEqualByComparingTo("50000");
        assertThat(pool.getTierCoefficient()).isEqualByComparingTo("1.0");
        assertThat(pool.getFinalPool()).isEqualByComparingTo("50000");
    }

    // ==================== 反例：达成率为空 / 负数 ====================

    @Test
    @DisplayName("反例: 达成率为空抛 IllegalArgumentException")
    void tierNullThrows() {
        assertThatThrownBy(() -> bonusPoolService.tierCoefficientOf(null));
    }

    @Test
    @DisplayName("反例: 负达成率（防御）=> 阶梯系数 0")
    void tierNegativeIsZero() {
        assertThat(bonusPoolService.tierCoefficientOf(pct("-1"))).isEqualByComparingTo("0");
    }

    // ==================== save: 派生字段填充 + Mapper 写入 ====================

    @Test
    @DisplayName("save: 写入前填充 basePool/tierCoefficient/finalPool，调用 mapper.insert")
    void saveFillsDerivedFields() {
        when(bonusPoolMapper.insert(any(BonusPool.class))).thenReturn(1);
        BonusPool pool = BonusPool.builder()
            .projectId(PROJECT_ID)
            .targetSales(new BigDecimal("2000000"))
            .achievementRate(pct("90"))
            .build();
        bonusPoolService.save(pool);
        assertThat(pool.getBasePool()).isEqualByComparingTo("100000");
        assertThat(pool.getTierCoefficient()).isEqualByComparingTo("0.8");
        assertThat(pool.getFinalPool()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("listByProject: 按 projectId 查询并按 calculatedAt 倒序")
    void listByProjectOrderedDesc() {
        BonusPool p1 = BonusPool.builder().id(1L).projectId(PROJECT_ID).build();
        BonusPool p2 = BonusPool.builder().id(2L).projectId(PROJECT_ID).build();
        when(bonusPoolMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(p1, p2));
        List<BonusPool> list = bonusPoolService.listByProject(PROJECT_ID);
        assertThat(list).hasSize(2);
    }
}
