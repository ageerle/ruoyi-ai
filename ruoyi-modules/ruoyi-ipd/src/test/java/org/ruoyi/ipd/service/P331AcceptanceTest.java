package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AllowanceLedger;
import org.ruoyi.ipd.mapper.AllowanceLedgerMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P3-3.1 月度津贴基础额、锁级与 2 倍封顶验收测试
 * AC：BR-INC-02/03；卡 P3-3 描述（基础额/锁级/封顶）。
 *
 * <p>W5-E-2.3（P0 #3）：新增 list/pendingStop/autoScan 三方法 IDOR 回归测试（每方法 3 测）——
 * actor null → UNAUTHORIZED「未登录」、非 SUPER_ADMIN 调 autoScan → FORBIDDEN（service 层与
 * Controller requireAdmin 同严兜底）、正常 actor + 合法 period 走原查询/计数路径（业务逻辑零改）。
 */
@Tag("dev")
class P331AcceptanceTest {

    /**
     * W4-D：AllowanceLedgerService 新增 {@code @RequiredArgsConstructor} + Mapper 字段后，
     * 测试改为传入 mock Mapper（calcFinalAmount / draftBinding 路径不触发 Mapper 调用）。
     * W5-E-2.3：持有 mapper 引用以 stub selectList/selectCount，支撑 list/pendingStop/autoScan IDOR 测试。
     */
    private final AllowanceLedgerMapper mapper = mock(AllowanceLedgerMapper.class);
    private final AllowanceLedgerService service = new AllowanceLedgerService(mapper);

    /** W5-E-2.3：actor 模板（读路径 MARKET_PM=7L；非超管拒扫 RD_PM=8L；超管豁免 SUPER_ADMIN=999L） */
    private static final IpdActor READER_ACTOR = new IpdActor(7L, "张三", "MARKET_PM", 1L);
    private static final IpdActor NON_ADMIN_ACTOR = new IpdActor(8L, "李四", "RD_PM", 1L);
    private static final IpdActor ADMIN_ACTOR = new IpdActor(999L, "超管", "SUPER_ADMIN", null);

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

    // ==================== W5-E-2.3 P0 #3：list actor 校验（3 测） ====================

    @Test
    @DisplayName("W5-E-2.3 IDOR-L1: list actor=null → UNAUTHORIZED 未登录（防御性兜底）")
    void listNullActorUnauthorized() {
        assertThatThrownBy(() -> service.list(null, "2026-09", null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录");
    }

    @Test
    @DisplayName("W5-E-2.3 IDOR-L2: list actor.id=null → UNAUTHORIZED 未登录")
    void listNullActorIdUnauthorized() {
        assertThatThrownBy(() -> service.list(new IpdActor(null, "u", "MARKET_PM", 1L), "2026-09", null))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录");
    }

    @Test
    @DisplayName("W5-E-2.3 IDOR-L3: list actor 正常 + period 合法 → 返回 mapper 行（业务逻辑零改）")
    void listValidActorReturnsRows() {
        AllowanceLedger row = AllowanceLedger.builder()
            .personId(101L).projectId(201L).month("2026-09").lockedLevel("L3").build();
        when(mapper.selectList(any())).thenReturn(List.of(row));

        List<AllowanceLedger> rows = service.list(READER_ACTOR, "2026-09", 101L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getPersonId()).isEqualTo(101L);
    }

    // ==================== W5-E-2.3 P0 #3：pendingStop actor 校验（3 测） ====================

    @Test
    @DisplayName("W5-E-2.3 IDOR-P1: pendingStop actor=null → UNAUTHORIZED 未登录")
    void pendingStopNullActorUnauthorized() {
        assertThatThrownBy(() -> service.pendingStop(null, "2026-09"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录");
    }

    @Test
    @DisplayName("W5-E-2.3 IDOR-P2: pendingStop actor 正常 → 返回 stopReason 非空行（业务逻辑零改）")
    void pendingStopValidActorReturnsRows() {
        AllowanceLedger l = AllowanceLedger.builder()
            .personId(101L).projectId(201L).month("2026-09").lockedLevel("L3").build();
        l.setStopReason("SCORE_BELOW_60");
        when(mapper.selectList(any())).thenReturn(List.of(l));

        List<AllowanceLedger> rows = service.pendingStop(READER_ACTOR, "2026-09");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStopReason()).isEqualTo("SCORE_BELOW_60");
    }

    @Test
    @DisplayName("W5-E-2.3 IDOR-P3: pendingStop period 非法 → 抛 IpdBusinessException（YYYY-MM 校验保留）")
    void pendingStopInvalidPeriodRejected() {
        assertThatThrownBy(() -> service.pendingStop(READER_ACTOR, "2026-13"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("YYYY-MM");
    }

    // ==================== W5-E-2.3 P0 #3：autoScan actor 校验（3 测） ====================

    @Test
    @DisplayName("W5-E-2.3 IDOR-A1: autoScan actor=null → UNAUTHORIZED 未登录")
    void autoScanNullActorUnauthorized() {
        assertThatThrownBy(() -> service.autoScan(null, "2026-09"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("未登录");
    }

    @Test
    @DisplayName("W5-E-2.3 IDOR-A2: autoScan 非 SUPER_ADMIN → FORBIDDEN（service 层与 Controller requireAdmin 同严兜底）")
    void autoScanNonAdminForbidden() {
        assertThatThrownBy(() -> service.autoScan(NON_ADMIN_ACTOR, "2026-09"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅超管可执行月度扫描");
    }

    @Test
    @DisplayName("W5-E-2.3 IDOR-A3: autoScan SUPER_ADMIN + period 合法 → 返回计数（业务逻辑零改）")
    void autoScanAdminReturnsCount() {
        when(mapper.selectCount(any())).thenReturn(2L);
        assertThat(service.autoScan(ADMIN_ACTOR, "2026-09")).isEqualTo(2);
    }
}
