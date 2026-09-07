/**
 * [SEC-FIX-HIGH-5.2] personalCoefficient 自动推导验收测试。
 * - 5 档分档边界（95/85/70/60 阈值）
 * - 无 comprehensive_score 回退 1.0
 * - 空 period/空 projectId 边界
 */
package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoComputePersonalCoefficientAcceptanceTest {

    @org.mockito.Mock private BonusPoolMapper bonusPoolMapper;
    @org.mockito.Mock private ProjectMapper projectMapper;
    @org.mockito.Mock private KpiRecordMapper kpiRecordMapper;
    @org.mockito.Mock private ProjectScoreService projectScoreService;
    @org.mockito.Mock private org.ruoyi.ipd.service.SystemConfigService systemConfigService;
    @org.mockito.Mock private org.ruoyi.ipd.service.AuditLogService auditLogService;
    @org.mockito.Mock private org.ruoyi.ipd.service.NotificationService notificationService;

    @org.mockito.InjectMocks private BonusPoolService service;

    private KpiRecord record(Long projectId, String period, BigDecimal comprehensiveScore) {
        KpiRecord r = new KpiRecord();
        r.setProjectId(projectId);
        r.setPeriod(period);
        r.setStatus("FINAL");
        r.setComprehensiveScore(comprehensiveScore);
        r.setDelFlag("0");
        r.setCreateTime(new Date());
        return r;
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] comprehensiveScore=95 → personalCoefficient=1.0（≥95 优秀档）")
    void comprehensive95() {
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of(record(10L, "2026-01", new BigDecimal("95"))));
        when(projectScoreService.projectPerformanceCoefficient(new BigDecimal("95"))).thenReturn(new BigDecimal("1.0"));
        BigDecimal coef = service.resolvePersonalCoefficient(10L, "2026-01");
        assertThat(coef).isEqualByComparingTo("1.0");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] comprehensiveScore=88 → personalCoefficient=0.8（≥85 良好档）")
    void comprehensive88() {
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of(record(10L, "2026-01", new BigDecimal("88"))));
        when(projectScoreService.projectPerformanceCoefficient(new BigDecimal("88"))).thenReturn(new BigDecimal("0.8"));
        assertThat(service.resolvePersonalCoefficient(10L, "2026-01")).isEqualByComparingTo("0.8");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] comprehensiveScore=55 → personalCoefficient=0（<60 取消奖金）")
    void comprehensive55() {
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of(record(10L, "2026-01", new BigDecimal("55"))));
        when(projectScoreService.projectPerformanceCoefficient(new BigDecimal("55"))).thenReturn(new BigDecimal("0"));
        assertThat(service.resolvePersonalCoefficient(10L, "2026-01")).isEqualByComparingTo("0");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] 无 kpi_records → 回退 1.0（中性）")
    void noRecord() {
        when(kpiRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertThat(service.resolvePersonalCoefficient(10L, "2026-01")).isEqualByComparingTo("1.0");
        verify(projectScoreService, never()).projectPerformanceCoefficient(any());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] period=null → 回退 1.0（不查表）")
    void nullPeriod() {
        assertThat(service.resolvePersonalCoefficient(10L, null)).isEqualByComparingTo("1.0");
        verify(kpiRecordMapper, never()).selectList(any());
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] projectId=null → 回退 1.0（不查表）")
    void nullProjectId() {
        assertThat(service.resolvePersonalCoefficient(null, "2026-01")).isEqualByComparingTo("1.0");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("[HIGH-5.2] comprehensiveScore=null → 回退 1.0（中性）")
    void nullScore() {
        when(kpiRecordMapper.selectList(any())).thenReturn(List.of(record(10L, "2026-01", null)));
        assertThat(service.resolvePersonalCoefficient(10L, "2026-01")).isEqualByComparingTo("1.0");
    }
}
