package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-4.1：轻管日期 / FAR·FRR / 证书录入（不改 status；再 /transit DONE）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P141AcceptanceTest {

    @Mock private StageActionMapper actionMapper;
    @Mock private DeliverableMapper deliverableMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectStageMapper projectStageMapper;
    @Mock private ProjectMapper projectMapper;

    private StageActionService service;

    @BeforeEach
    void setUp() {
        lenient().when(projectMapper.selectById(any())).thenReturn(
            Project.builder().id(100L).status("ACTIVE").delFlag("0").build());
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new StageActionService(actionMapper, deliverableMapper, auditLogService,
            projectStageMapper, projectMapper);
    }

    private StageAction seed(String code, String depth) {
        StageAction a = StageAction.builder()
            .id(1L).projectId(100L).stageId(10L).actionCode(code).actionName("t")
            .ownerRole("MARKET_PM").depth(depth).status("IN_PROGRESS")
            .isBlocking("1").isBioFeature("0").version(0)
            .build();
        when(actionMapper.selectById(1L)).thenReturn(a);
        lenient().when(actionMapper.updateById(any(StageAction.class))).thenReturn(1);
        return a;
    }

    @Test
    @DisplayName("轻管先录 actualDoneAt 再 DONE；无日期仍拒绝")
    void lightDateThenDone() {
        StageAction a = seed("C05", "LIGHT");
        assertThatThrownBy(() -> service.transit(1L, "DONE", "x", "9"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-05");

        Date day = new Date(1_700_000_000_000L);
        StageAction recorded = service.recordFields(1L, day, null, null, null, null, null, "9");
        assertThat(recorded.getActualDoneAt()).isNotNull();
        assertThat(recorded.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(service.transit(1L, "DONE", "done", "9").getStatus()).isEqualTo("DONE");
        verify(auditLogService, atLeastOnce()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("D11：FAR/FRR 成对录入后可 DONE；越界拒绝")
    void d11FarFrrRecord() {
        StageAction a = seed("D11", "LIGHT");
        a.setActualDoneAt(new Date());
        assertThatThrownBy(() -> service.recordFields(1L, null,
            new BigDecimal("1.5"), new BigDecimal("0.1"), null, null, null, "9"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("FAR");

        service.recordFields(1L, null, new BigDecimal("0.0001"), new BigDecimal("0.02"), null, null, null, "9");
        assertThat(a.getFarValue()).isEqualByComparingTo("0.0001");
        assertThat(service.transit(1L, "DONE", "ok", "9").getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("V02：证书号+通过日录入后可 DONE；C05 拒证书字段")
    void v02CertRecord() {
        StageAction c05 = seed("C05", "LIGHT");
        assertThatThrownBy(() -> service.recordFields(1L, null, null, null, "X", new Date(), null, "9"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不支持证书");

        StageAction v02 = seed("V02", "LIGHT");
        v02.setActualDoneAt(new Date());
        Date passed = new Date(1_700_100_000_000L);
        service.recordFields(1L, null, null, null, "CE-2026-001", passed, null, "9");
        assertThat(v02.getCertNo()).isEqualTo("CE-2026-001");
        assertThat(service.transit(1L, "DONE", "ok", "9").getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("暂停项目禁止录入字段")
    void suspendedReadonly() {
        seed("C05", "LIGHT");
        when(projectMapper.selectById(100L)).thenReturn(
            Project.builder().id(100L).status("SUSPENDED").delFlag("0").build());
        assertThatThrownBy(() -> service.recordFields(1L, new Date(), null, null, null, null, null, "9"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("暂停");
    }
}
