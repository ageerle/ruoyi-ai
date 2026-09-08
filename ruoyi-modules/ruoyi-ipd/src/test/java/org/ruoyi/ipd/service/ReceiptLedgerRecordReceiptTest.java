package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ReceiptLedger;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ReceiptLedgerMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 2026-09-08 缺口补齐回归：receipt_ledger 写入路径接线（controller 新增）前，
 * service 层录入校验与 AC-INC-32 窗口起算兜底测试。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ReceiptLedgerRecordReceiptTest {

    @Mock
    private ReceiptLedgerMapper receiptLedgerMapper;
    @Mock
    private ProjectMapper projectMapper;

    private ReceiptLedgerService service;

    @BeforeEach
    void setUp() {
        service = new ReceiptLedgerService(receiptLedgerMapper, projectMapper);
    }

    private Project projectWithLaunch(Date launchDate) {
        Project project = new Project();
        project.setId(1L);
        project.setDelFlag("0");
        project.setLaunchDate(launchDate);
        return project;
    }

    private Date date(int y, int m, int d) {
        return Date.from(LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("正例：source=RECEIPT + 项目有上市日期 → 窗口按 AC-INC-32 起算 6 自然月")
    void recordReceipt_withLaunchDate_computesWindow() {
        when(projectMapper.selectById(1L)).thenReturn(projectWithLaunch(date(2026, 1, 15)));
        ReceiptLedger ledger = new ReceiptLedger();
        ledger.setProjectId(1L);
        ledger.setSource("RECEIPT");
        ledger.setReceiptMonth("2026-08");
        ledger.setReceiptAmount(new BigDecimal("1000.00"));

        ReceiptLedger saved = service.recordReceipt(ledger);

        assertThat(saved.getWindowStart()).isEqualTo(date(2026, 1, 15));
        assertThat(saved.getWindowEnd()).isEqualTo(date(2026, 7, 14));
        assertThat(saved.getRefundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(receiptLedgerMapper).insert(ledger);
    }

    @Test
    @DisplayName("项目无上市日期且未传窗口 → windowStart 保持空（isInWindow 默认计入）")
    void recordReceipt_noLaunchDate_noWindow() {
        when(projectMapper.selectById(1L)).thenReturn(projectWithLaunch(null));
        ReceiptLedger ledger = new ReceiptLedger();
        ledger.setProjectId(1L);
        ledger.setSource("RECEIPT");
        ledger.setReceiptMonth("2026-08");
        ledger.setReceiptAmount(new BigDecimal("500.00"));

        ReceiptLedger saved = service.recordReceipt(ledger);

        assertThat(saved.getWindowStart()).isNull();
        assertThat(saved.getWindowEnd()).isNull();
        verify(receiptLedgerMapper).insert(ledger);
    }

    @Test
    @DisplayName("月份缺失或非法格式 → 拒绝且不落库（receipt_month NOT NULL 防 500）")
    void recordReceipt_invalidMonth_rejected() {
        ReceiptLedger bad1 = new ReceiptLedger();
        bad1.setProjectId(1L);
        bad1.setSource("RECEIPT");
        bad1.setReceiptAmount(BigDecimal.ONE);
        // receiptMonth 为 null

        ReceiptLedger bad2 = new ReceiptLedger();
        bad2.setProjectId(1L);
        bad2.setSource("RECEIPT");
        bad2.setReceiptMonth("2026-13");
        bad2.setReceiptAmount(BigDecimal.ONE);

        assertThatThrownBy(() -> service.recordReceipt(bad1))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("YYYY-MM");
        assertThatThrownBy(() -> service.recordReceipt(bad2))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("YYYY-MM");
        verify(receiptLedgerMapper, never()).insert(any(ReceiptLedger.class));
    }

    @Test
    @DisplayName("项目不存在 → 拒绝（防脏 projectId 落库）")
    void recordReceipt_projectMissing_rejected() {
        when(projectMapper.selectById(9L)).thenReturn(null);
        ReceiptLedger ledger = new ReceiptLedger();
        ledger.setProjectId(9L);
        ledger.setSource("RECEIPT");
        ledger.setReceiptMonth("2026-08");
        ledger.setReceiptAmount(BigDecimal.ONE);

        assertThatThrownBy(() -> service.recordReceipt(ledger))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("项目不存在");
        verify(receiptLedgerMapper, never()).insert(any(ReceiptLedger.class));
    }

    @Test
    @DisplayName("金额缺失或非正 → 拒绝")
    void recordReceipt_nonPositiveAmount_rejected() {
        // 金额校验先于项目查询，无需 stub projectMapper
        ReceiptLedger bad = new ReceiptLedger();
        bad.setProjectId(1L);
        bad.setSource("RECEIPT");
        bad.setReceiptMonth("2026-08");
        bad.setReceiptAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.recordReceipt(bad))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("正数");
        verify(receiptLedgerMapper, never()).insert(any(ReceiptLedger.class));
    }

    @Test
    @DisplayName("source 非 RECEIPT → AC-INC-16b 口径拒绝")
    void recordReceipt_wrongSource_rejected() {
        ReceiptLedger ledger = new ReceiptLedger();
        ledger.setProjectId(1L);
        ledger.setSource("INVOICE");
        ledger.setReceiptMonth("2026-08");
        ledger.setReceiptAmount(BigDecimal.ONE);

        assertThatThrownBy(() -> service.recordReceipt(ledger))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("AC-INC-16b");
        verify(receiptLedgerMapper, never()).insert(any(ReceiptLedger.class));
    }
}
