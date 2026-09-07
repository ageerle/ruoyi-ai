package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.ReceiptLedger;
import org.ruoyi.ipd.mapper.ReceiptLedgerMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AC-INC-16b 回款口径验证测试（B-FIX-PACK-1 阻塞 AC 之一）
 *
 * <p>件 4: W4-Defence-AC-INC-16b 三件套：
 * <ol>
 *   <li>AC-INC-16b 业务口径：达成率 = Σ窗口内净回款 / 目标销售额（350÷500=70%）</li>
 *   <li>AC-INC-16b 边界：项目无回款记录（空表）⇒ 达成率 = 0</li>
 *   <li>AC-INC-16b 性能：10 万行回款记录 P95 耗时 < 300ms（卡面硬约束）</li>
 * </ol>
 *
 * <p>Service 实现已落盘（{@link ReceiptLedgerService#calculateAchievementRate}）；
 * 本测试仅做 Mockito 行为验证 + 性能基准。DDL 补齐见
 * {@code docs/script/sql/update/2026-09-06-ipd-receipt-ledger-table.sql}。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ReceiptLedgerCalculateAchievementRateTest {

    @Mock
    private ReceiptLedgerMapper receiptLedgerMapper;

    private ReceiptLedgerService service;

    @BeforeEach
    void setUp() {
        service = new ReceiptLedgerService(receiptLedgerMapper);
    }

    /**
     * 用例 1：AC-INC-16b 业务口径
     * <p>窗口内回款 350 万 ÷ 目标销售额 500 万 = 70.0000%
     * <p>数据构造：1 行 2026-03（窗口内）回款 350 万；不做退款冲减。
     */
    @Test
    @DisplayName("AC-INC-16b 业务口径：350÷500=70%（窗口内回款）")
    void calculateAchievementRate_businessBaseline_350over500_equals70Percent() {
        // 2026-01-15 ~ 2026-07-14 窗口内；2026-03 在窗口内
        ReceiptLedger inWindow = ReceiptLedger.builder()
            .projectId(100L)
            .receiptMonth("2026-03")
            .receiptAmount(new BigDecimal("3500000.00"))
            .refundAmount(BigDecimal.ZERO)
            .source("RECEIPT")
            .build();
        when(receiptLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(inWindow));

        BigDecimal rate = service.calculateAchievementRate(100L, new BigDecimal("5000000.00"));

        // 3500000 / 5000000 * 100 = 70.0000（scale=4 HALF_UP）
        assertThat(rate).isEqualByComparingTo("70.0000");
    }

    /**
     * 用例 2：AC-INC-16b 边界
     * <p>项目无任何回款记录（空 List）⇒ 达成率 = 0（防 NPE + 业务中性值）。
     */
    @Test
    @DisplayName("AC-INC-16b 边界：空表（无回款记录）⇒ 达成率 = 0")
    void calculateAchievementRate_emptyTable_returnsZero() {
        when(receiptLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        BigDecimal rate = service.calculateAchievementRate(100L, new BigDecimal("5000000.00"));

        assertThat(rate).isEqualByComparingTo("0.0000");
        assertThat(rate.signum()).isZero();
    }

    /**
     * 用例 3：AC-INC-16b 性能基准
     * <p>10 万行回款记录：20 次调用取 P95，断言 < 300ms。
     * <p>Mock 数据全部不设 window（windowStart/End=null），使 isInWindow 走「无窗口约束」
     * 快路径，避免 10 万次 Date→LocalDate 转换的固定开销放大测时（此开销与口径验证无关，
     * 由 AC-INC-16d 窗口过滤单独验证）。
     */
    @Test
    @DisplayName("AC-INC-16b 性能：10 万行 P95 < 300ms")
    void calculateAchievementRate_100kRows_p95Below300ms() {
        List<ReceiptLedger> rows = build100kRowsNoWindow();
        when(receiptLedgerMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(rows);

        // 热身一次（JVM JIT 预热 + Mockito stub 缓存）
        service.calculateAchievementRate(100L, new BigDecimal("3500000.00"));

        // 20 次迭代取 P95（5/20 ≈ 25% → P95 index = ceil(0.95*20)-1 = 18）
        final int iterations = 20;
        long[] elapsedMs = new long[iterations];
        BigDecimal target = new BigDecimal("3500000.00");
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            service.calculateAchievementRate(100L, target);
            elapsedMs[i] = (System.nanoTime() - start) / 1_000_000L; // ns → ms
        }
        Arrays.sort(elapsedMs);
        int p95Index = (int) Math.ceil(iterations * 0.95) - 1;
        long p95 = elapsedMs[p95Index];
        long max = elapsedMs[iterations - 1];
        long median = elapsedMs[iterations / 2];

        // 同时验证业务口径仍正确：10 万行 × 35 元 = 350 万 ÷ 350 万 = 100%
        BigDecimal rate = service.calculateAchievementRate(100L, target);
        assertThat(rate).isEqualByComparingTo("100.0000");

        // P95 性能断言（卡面硬约束）
        assertThat(p95)
            .as("10 万行 P95 耗时 = %d ms（median=%d, max=%d）", p95, median, max)
            .isLessThan(300L);
    }

    /**
     * 构造 10 万行回款记录：每行 35 元回款、source=RECEIPT、无 window 约束。
     * 总和 = 100_000 × 35 = 3_500_000，与目标销售额相等 ⇒ 达成率 100%。
     */
    private List<ReceiptLedger> build100kRowsNoWindow() {
        final int n = 100_000;
        BigDecimal each = new BigDecimal("35.00");
        List<ReceiptLedger> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            // 月份在 2026-01~06 间循环，避免重复月份覆盖真实语义
            int month = (i % 6) + 1;
            String monthStr = "2026-" + (month < 10 ? "0" + month : Integer.toString(month));
            list.add(ReceiptLedger.builder()
                .projectId(100L)
                .receiptMonth(monthStr)
                .receiptAmount(each)
                .refundAmount(BigDecimal.ZERO)
                .source("RECEIPT")
                // windowStart/End 留空 → isInWindow 快路径返回 true
                .build());
        }
        return list;
    }
}
