package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.ReceiptLedger;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ReceiptLedgerMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AC-INC-29c/29d 算例 C 端到端链测试（B-FIX-PACK-1 阻塞 AC，卡 2e4dfcfc §1）。
 *
 * <p>算例 C 验收原文（验收清单 :308-309）：目标 500 万 / A 级 1.0 / 上市后 6 个月回款 350 万
 * ⇒ 达成率 70% → 阶梯 0.6；29d 对照：若误用出库 450 万应为 90%（出库口径），系统必须取回款 70%。
 *
 * <p><b>口径演进登记（避免下一个执行方拿旧数字当契约）</b>：验收清单旧文「奖金池 = 25 万 → 可分配 15 万」
 * 按目标销售额基数推得（500 万 × 5%）。2026-09-06 owner 已拍板 [CONSISTENCY-1]（BonusPoolService javadoc）：
 * 池基数 = <b>实际回款</b> × 5% × S/A/B 系数，targetSales 仅历史兼容保留。本测试按新口径钉数字链：
 * <pre>回款 350 万 → 达成率 70.0000%（ReceiptLedgerService，AC-INC-16b）
 *   → tierCoefficientOf = 0.6（AC-INC-17 六档，阈值 70 含端点）
 *   → finalPool = 350 万 × 5% × 1.0(A) × 0.6(tier) × 1.0(personal) = 105,000</pre>
 * 全链只装回款数据：任一层误取目标销售额/出库口径，断言即红——29d 的「未取错字段」由此全链保证。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class IncentiveCaseCReceiptChainTest {

    @Mock
    private ReceiptLedgerMapper receiptLedgerMapper;
    @Mock
    private BonusPoolMapper bonusPoolMapper;
    @Mock
    private ProjectMapper projectMapper;

    private ReceiptLedgerService receiptService;
    private BonusPoolService bonusService;

    @BeforeEach
    void setUp() {
        receiptService = new ReceiptLedgerService(receiptLedgerMapper, projectMapper);
        bonusService = new BonusPoolService(bonusPoolMapper, projectMapper);
    }

    private ReceiptLedger inWindowReceipt(String amount) {
        // 2026-01-15 ~ 2026-07-14 上市后窗口；2026-03 在窗口内（与 AC-INC-16b 基线用例同窗）
        return ReceiptLedger.builder()
            .projectId(100L)
            .receiptMonth("2026-03")
            .receiptAmount(new BigDecimal(amount))
            .refundAmount(BigDecimal.ZERO)
            .source("RECEIPT")
            .build();
    }

    /**
     * 用例 1：AC-INC-29c 主链（新口径 [CONSISTENCY-1]）
     * <p>350 万 ÷ 500 万 = 70.0000% → 阶梯 0.6 → 350 万 × 5% × 1.0 × 0.6 × 1.0 = 105,000。
     */
    @Test
    @DisplayName("AC-INC-29c 端到端：回款350万→70%→阶梯0.6→池105000（新口径）")
    void caseC_endToEnd_receipt350_tier06_pool105000() {
        when(receiptLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(inWindowReceipt("3500000.00")));

        BigDecimal rate = receiptService.calculateAchievementRate(100L, new BigDecimal("5000000.00"));
        BigDecimal tier = bonusService.tierCoefficientOf(rate);
        BigDecimal pool = bonusService.calculateBonusPoolByZkFormulaWithModifiers(
            new BigDecimal("3500000.00"), new BigDecimal("1.0"), tier, new BigDecimal("1.0"));

        assertThat(rate).isEqualByComparingTo("70.0000");
        assertThat(tier).isEqualByComparingTo("0.6");
        assertThat(pool).isEqualByComparingTo("105000");
    }

    /**
     * 用例 2：AC-INC-29d 对照——出库口径数据在录入口即被拒，无法污染链条。
     * <p>若误用出库 450 万：达成率 90% → 阶梯 0.8 → 450 万 × 5% × 0.8 = 180,000 ≠ 105,000；
     * 但 source 非 RECEIPT 的记录在 recordReceipt 直接抛出，链路内不存在出库字段可取。
     */
    @Test
    @DisplayName("AC-INC-29d 对照：出库口径录入即拒（source!=RECEIPT 抛出），链路无出库字段")
    void caseD_shipmentSourceRejectedAtIngest() {
        ReceiptLedger shipment = ReceiptLedger.builder()
            .projectId(100L)
            .receiptMonth("2026-03")
            .receiptAmount(new BigDecimal("4500000.00"))
            .refundAmount(BigDecimal.ZERO)
            .source("SHIPMENT")
            .build();

        assertThatThrownBy(() -> receiptService.recordReceipt(shipment))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("RECEIPT");
    }

    /**
     * 用例 3：阶梯端点（AC-INC-17：达成率 ≥ 阈值 从高到低匹配，下端点含）。
     * <p>70.0000% 恰含于 0.6 档；69.9999% 落下一档 0.3——钉住算例 C「70% → 0.6」不是浮点巧合。
     */
    @Test
    @DisplayName("AC-INC-17 端点：70.0000%→0.6（含端点），69.9999%→0.3（落档）")
    void tierBoundary_70Inclusive_hits06_and_699999Hits03() {
        assertThat(bonusService.tierCoefficientOf(new BigDecimal("70.0000")))
            .isEqualByComparingTo("0.6");
        assertThat(bonusService.tierCoefficientOf(new BigDecimal("69.9999")))
            .isEqualByComparingTo("0.3");
    }

    /**
     * 用例 4：池基数 = 回款 × 5%（中性修饰，不乘阶梯）——对照已作废的目标额口径
     * （旧文 500 万 × 5% = 250,000；[CONSISTENCY-1] 后基数 350 万 × 5% = 175,000）。
     */
    @Test
    @DisplayName("口径对照：池基数=回款350万×5%=175000（中性），非目标额口径 250000")
    void poolBase_is_receipt_notTargetSales() {
        BigDecimal neutral = bonusService.calculateBonusPoolByZkFormulaWithModifiers(
            new BigDecimal("3500000.00"), new BigDecimal("1.0"), null, null);

        assertThat(neutral).isEqualByComparingTo("175000");
    }
}
