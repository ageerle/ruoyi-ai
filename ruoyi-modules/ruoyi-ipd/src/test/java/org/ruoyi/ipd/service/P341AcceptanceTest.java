package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ReceiptLedger;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ReceiptLedgerMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P3-4.1 销售回款台账验收测试（AC-INC-16b/16c/16d/31/31b/32）
 *
 * <p>形态为 Mockito 单元验收；真库 HTTP 验收另见 evidence-p341-http-acceptance-*.json。
 *
 * <p>2026-09-08 缺口补齐：service 注入 ProjectMapper 后 1 参构造失效，统一迁移到 2 参构造；
 * 4 处 IllegalArgumentException / IllegalStateException 升级为 IpdBusinessException（业务拒绝统一包络）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P341AcceptanceTest {

    @Mock
    private ReceiptLedgerMapper receiptLedgerMapper;
    @Mock
    private ProjectMapper projectMapper;

    private ReceiptLedgerService receiptLedgerService;

    private ReceiptLedger sampleLedger;
    private Date windowStart;
    private Date windowEnd;

    @BeforeEach
    void setUp() {
        receiptLedgerService = new ReceiptLedgerService(receiptLedgerMapper, projectMapper);
        // AC-INC-32：上市日期 2026-01-15，6自然月窗口 → 2026-07-14
        LocalDate start = LocalDate.of(2026, 1, 15);
        LocalDate end = start.plusMonths(6).minusDays(1);
        windowStart = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
        windowEnd = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());

        sampleLedger = ReceiptLedger.builder()
            .id(3001L)
            .projectId(100L)
            .bonusPoolId(500L)
            .receiptMonth("2026-03")
            .receiptAmount(new BigDecimal("500000.00"))
            .refundAmount(BigDecimal.ZERO)
            .source("RECEIPT")
            .voucherUrl("https://oss.example.com/voucher/bank-202603.pdf")
            .voucherHash("abc123def456")
            .windowStart(windowStart)
            .windowEnd(windowEnd)
            .build();
    }

    private Project projectLaunch2026Jan15() {
        Project p = new Project();
        p.setId(100L);
        p.setDelFlag("0");
        p.setLaunchDate(windowStart);
        return p;
    }

    @Test
    @DisplayName("AC-INC-16c 录入回款：月度金额 + 凭证附件 ⇒ 成功")
    void recordReceipt_success() {
        when(projectMapper.selectById(100L)).thenReturn(projectLaunch2026Jan15());
        when(receiptLedgerMapper.insert(any(ReceiptLedger.class))).thenReturn(1);

        ReceiptLedger result = receiptLedgerService.recordReceipt(sampleLedger);

        assertThat(result.getSource()).isEqualTo("RECEIPT");
        assertThat(result.getReceiptAmount()).isEqualByComparingTo("500000.00");
        assertThat(result.getVoucherUrl()).isNotBlank();
        verify(receiptLedgerMapper).insert(any(ReceiptLedger.class));
    }

    @Test
    @DisplayName("AC-INC-16b 口径验证：source=SHIPMENT ⇒ 拒绝（必须是 RECEIPT）")
    void recordReceipt_shipmentSource_rejected() {
        sampleLedger.setSource("SHIPMENT");

        assertThatThrownBy(() -> receiptLedgerService.recordReceipt(sampleLedger))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("AC-INC-16b")
            .hasMessageContaining("RECEIPT");
    }

    @Test
    @DisplayName("AC-INC-16b 口径验证：回款金额必须为正数")
    void recordReceipt_zeroAmount_rejected() {
        sampleLedger.setReceiptAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> receiptLedgerService.recordReceipt(sampleLedger))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("正数");
    }

    @Test
    @DisplayName("AC-INC-31b 窗口内退款 ⇒ 当期冲减")
    void recordRefund_inWindow_deductsCurrentPeriod() {
        when(receiptLedgerMapper.selectOne(any())).thenReturn(sampleLedger);
        when(receiptLedgerMapper.updateById(any(ReceiptLedger.class))).thenReturn(1);

        ReceiptLedger result = receiptLedgerService.recordRefund(100L, "2026-03", new BigDecimal("50000.00"));

        assertThat(result.getRefundAmount()).isEqualByComparingTo("50000.00");
        verify(receiptLedgerMapper).updateById(any(ReceiptLedger.class));
    }

    @Test
    @DisplayName("AC-INC-31 窗口外退款 ⇒ 不做回溯扣减（拒绝）")
    void recordRefund_outOfWindow_rejected() {
        // 月份 2026-09 在窗口（2026-01-15 ~ 2026-07-14）外
        ReceiptLedger outOfWindow = ReceiptLedger.builder()
            .id(3002L).projectId(100L).receiptMonth("2026-09")
            .receiptAmount(new BigDecimal("100000.00")).refundAmount(BigDecimal.ZERO)
            .source("RECEIPT").windowStart(windowStart).windowEnd(windowEnd).build();
        when(receiptLedgerMapper.selectOne(any())).thenReturn(outOfWindow);

        assertThatThrownBy(() -> receiptLedgerService.recordRefund(100L, "2026-09", new BigDecimal("10000.00")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("AC-INC-31")
            .hasMessageContaining("窗口外退款不做回溯扣减");
    }

    @Test
    @DisplayName("AC-INC-16d 达成率计算：窗口外数据不计入")
    void calculateAchievementRate_excludesOutOfWindow() {
        ReceiptLedger inWindow = ReceiptLedger.builder()
            .projectId(100L).receiptMonth("2026-03").receiptAmount(new BigDecimal("3500000.00"))
            .refundAmount(BigDecimal.ZERO).source("RECEIPT")
            .windowStart(windowStart).windowEnd(windowEnd).build();
        ReceiptLedger outWindow = ReceiptLedger.builder()
            .projectId(100L).receiptMonth("2026-09").receiptAmount(new BigDecimal("1000000.00"))
            .refundAmount(BigDecimal.ZERO).source("RECEIPT")
            .windowStart(windowStart).windowEnd(windowEnd).build();
        when(receiptLedgerMapper.selectList(any())).thenReturn(List.of(inWindow, outWindow));

        // 目标销售额 500万，窗口内回款 350万 → 达成率 70%
        BigDecimal rate = receiptLedgerService.calculateAchievementRate(100L, new BigDecimal("5000000.00"));

        assertThat(rate).isEqualByComparingTo("70.0000");
    }

    @Test
    @DisplayName("AC-INC-32 窗口计算：上市日期起算6自然月")
    void recordReceipt_computesWindow() {
        when(projectMapper.selectById(100L)).thenReturn(projectLaunch2026Jan15());
        when(receiptLedgerMapper.insert(any(ReceiptLedger.class))).thenReturn(1);
        sampleLedger.setWindowEnd(null); // 让 service 计算

        receiptLedgerService.recordReceipt(sampleLedger);

        assertThat(sampleLedger.getWindowEnd()).isNotNull();
        // 2026-01-15 + 6个月 - 1天 = 2026-07-14
        LocalDate end = sampleLedger.getWindowEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertThat(end).isEqualTo(LocalDate.of(2026, 7, 14));
    }

    @Test
    @DisplayName("退款冲减：该月份无回款记录 ⇒ 拒绝")
    void recordRefund_noExistingRecord_rejected() {
        when(receiptLedgerMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> receiptLedgerService.recordRefund(100L, "2026-05", new BigDecimal("10000.00")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("该月份无回款记录");
    }
}
