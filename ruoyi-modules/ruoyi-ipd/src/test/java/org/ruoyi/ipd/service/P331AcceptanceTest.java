package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P3-3.1 月度津贴基础额、锁级与 2 倍封顶验收测试
 * AC：BR-INC-02/03；卡 P3-3 描述（基础额/锁级/封顶）。
 */
@Tag("dev")
class P331AcceptanceTest {

    private final AllowanceLedgerService service = new AllowanceLedgerService();

    // ==================== 锁定评级校验 ====================

    @Test
    @DisplayName("正例: L1..L5 锁定评级合法")
    void lockedLevelsValid() {
        for (String lvl : Arrays.asList("L1", "L2", "L3", "L4", "L5")) {
            AllowanceLedgerService.validateLockedLevel(lvl);
        }
    }

    @Test
    @DisplayName("反例: 锁定评级 L0 / L6 / 中文 抛 IpdBusinessException")
    void lockedLevelsInvalid() {
        for (String lvl : Arrays.asList("L0", "L6", "初级", null, "")) {
            assertThatThrownBy(() -> AllowanceLedgerService.validateLockedLevel(lvl))
                .isInstanceOf(IpdBusinessException.class);
        }
    }

    // ==================== 基础额校验 ====================

    @Test
    @DisplayName("正例: 基础额 0 / 5000 / 10000 通过")
    void baseAmountValid() {
        for (BigDecimal v : Arrays.asList(BigDecimal.ZERO,
            new BigDecimal("5000"), new BigDecimal("10000"))) {
            AllowanceLedgerService.validateBaseAmount(v);
        }
    }

    @Test
    @DisplayName("反例: 基础额 -100 抛 IpdBusinessException")
    void baseAmountNegative() {
        assertThatThrownBy(() -> AllowanceLedgerService.validateBaseAmount(new BigDecimal("-100")))
            .isInstanceOf(IpdBusinessException.class);
    }

    @Test
    @DisplayName("反例: 基础额 null 抛 IpdBusinessException")
    void baseAmountNull() {
        assertThatThrownBy(() -> AllowanceLedgerService.validateBaseAmount(null))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== 封顶倍数校验 ====================

    @Test
    @DisplayName("正例: 封顶倍数 null 走默认 2.0；2.5 也合法")
    void capMultiplierValid() {
        AllowanceLedgerService.validateCapMultiplier(null);
        AllowanceLedgerService.validateCapMultiplier(new BigDecimal("2.0"));
        AllowanceLedgerService.validateCapMultiplier(new BigDecimal("2.5"));
    }

    @Test
    @DisplayName("反例: 封顶倍数 0.5 < 1.0 抛 IpdBusinessException")
    void capMultiplierTooLow() {
        assertThatThrownBy(() -> AllowanceLedgerService.validateCapMultiplier(new BigDecimal("0.5")))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== 多项目叠加 2 倍封顶 ====================

    @Test
    @DisplayName("单项目 baseAmount=5000，叠加 1 项 ⇒ finalAmount=5000，capApplied=0")
    void singleProject() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3").build();
        AllowanceLedger result = service.calcFinalAmount(draft,
            Collections.singletonList(new BigDecimal("5000")), null);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getCapApplied()).isEqualTo("0");
    }

    @Test
    @DisplayName("两项目 baseAmount=[5000, 5000]，叠加 10000 ≤ capLine=10000 ⇒ capApplied=0")
    void twoProjectsUnderCap() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3").build();
        AllowanceLedger result = service.calcFinalAmount(draft,
            Arrays.asList(new BigDecimal("5000"), new BigDecimal("5000")), null);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getCapApplied()).isEqualTo("0");
    }

    @Test
    @DisplayName("三项目 baseAmount=[5000, 5000, 5000]，叠加 15000 > capLine=10000 ⇒ finalAmount=10000, capApplied=1")
    void threeProjectsTriggerCap() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3").build();
        AllowanceLedger result = service.calcFinalAmount(draft,
            Arrays.asList(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("5000")), null);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getCapApplied()).isEqualTo("1");
    }

    @Test
    @DisplayName("边界: 叠加恰好 = capLine=10000 ⇒ capApplied=0（不触发封顶）")
    void twoProjectsExactCap() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3").build();
        AllowanceLedger result = service.calcFinalAmount(draft,
            Arrays.asList(new BigDecimal("5000"), new BigDecimal("5000")), null);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getCapApplied()).isEqualTo("0");
    }

    @Test
    @DisplayName("自定义 capMultiplier=3.0: 三项目 15000 ≤ capLine=15000 ⇒ capApplied=0")
    void customCapMultiplier() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L4").build();
        AllowanceLedger result = service.calcFinalAmount(draft,
            Arrays.asList(new BigDecimal("5000"), new BigDecimal("5000"), new BigDecimal("5000")),
            new BigDecimal("3.0"));
        assertThat(result.getFinalAmount()).isEqualByComparingTo("15000.00");
        assertThat(result.getCapApplied()).isEqualTo("0");
    }

    @Test
    @DisplayName("空 baseAmount 列表: finalAmount=0，capApplied=0")
    void emptyBaseAmount() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3").build();
        AllowanceLedger result = service.calcFinalAmount(draft, Collections.emptyList(), null);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getCapApplied()).isEqualTo("0");
    }

    @Test
    @DisplayName("反例: 基础额含 -100 抛 IpdBusinessException")
    void negativeBaseAmountInList() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3").build();
        assertThatThrownBy(() -> service.calcFinalAmount(draft,
            Arrays.asList(new BigDecimal("5000"), new BigDecimal("-100")), null))
            .isInstanceOf(IpdBusinessException.class);
    }

    // ==================== 草稿绑定 ====================

    @Test
    @DisplayName("草稿绑定: L3 + baseAmount=5000 ⇒ finalAmount=5000, capApplied=0")
    void draftBindingOk() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L3")
            .baseAmount(new BigDecimal("5000")).build();
        AllowanceLedger result = service.draftBinding(draft);
        assertThat(result.getFinalAmount()).isEqualByComparingTo("5000");
        assertThat(result.getCapApplied()).isEqualTo("0");
    }

    @Test
    @DisplayName("草稿绑定反例: 锁定评级非法抛 IpdBusinessException")
    void draftBindingInvalidLevel() {
        AllowanceLedger draft = AllowanceLedger.builder()
            .personId(100L).month("2026-09").lockedLevel("L9")
            .baseAmount(new BigDecimal("5000")).build();
        assertThatThrownBy(() -> service.draftBinding(draft))
            .isInstanceOf(IpdBusinessException.class);
    }
}
