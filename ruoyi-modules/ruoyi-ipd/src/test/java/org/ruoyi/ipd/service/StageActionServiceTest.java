package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.domain.AuditLog;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-4 深轻管分离校验（BR-IPD-03/04/05 + 动作清单 v3 例外一/二）：
 * 深管=附件强制；轻管=完成日期必填+禁 DELAYED；D11 必登 FAR/FRR；V02 必登证书字段。
 */
@Tag("dev")
class StageActionServiceTest {

    private StageActionMapper actionMapper;
    private DeliverableMapper deliverableMapper;
    private AuditLogService auditLogService;
    private StageActionService service;

    @BeforeEach
    void setUp() {
        actionMapper = mock(StageActionMapper.class);
        deliverableMapper = mock(DeliverableMapper.class);
        auditLogService = mock(AuditLogService.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new StageActionService(actionMapper, deliverableMapper, auditLogService);
    }

    private StageAction seed(String code, String depth) {
        StageAction a = StageAction.builder()
            .id(1L).projectId(100L).stageId(10L).actionCode(code).actionName("t")
            .ownerRole("MARKET_PM").depth(depth).status("IN_PROGRESS")
            .isBlocking("1").isBioFeature("0")
            .build();
        when(actionMapper.selectById(1L)).thenReturn(a);
        when(actionMapper.updateById(any(StageAction.class))).thenReturn(1);
        return a;
    }

    @Test
    @DisplayName("深管无交付物标 DONE 拒绝（BR-IPD-03 强制附件）")
    void deepDoneRequiresDeliverable() {
        seed("C01", "DEEP");
        when(deliverableMapper.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.transit(1L, "DONE", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-03");
    }

    @Test
    @DisplayName("深管有交付物标 DONE 通过并写审计")
    void deepDoneWithDeliverablePasses() {
        seed("C01", "DEEP");
        when(deliverableMapper.selectCount(any())).thenReturn(1L);
        StageAction out = service.transit(1L, "DONE", "op");
        assertThat(out.getStatus()).isEqualTo("DONE");
        assertThat(out.getActualDoneAt()).isNotNull();
        Mockito.verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("轻管无完成日期标 DONE 拒绝（BR-IPD-05）")
    void lightDoneRequiresActualDate() {
        seed("C05", "LIGHT");
        assertThatThrownBy(() -> service.transit(1L, "DONE", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-05");
    }

    @Test
    @DisplayName("轻管禁 DELAYED（三字段枚举无延期）")
    void lightNoDelayed() {
        seed("C05", "LIGHT");
        assertThatThrownBy(() -> service.transit(1L, "DELAYED", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("轻管动作不支持延期");
    }

    @Test
    @DisplayName("轻管带完成日期标 DONE 通过；NA 无需校验")
    void lightDoneAndNaPass() {
        StageAction a = seed("C05", "LIGHT");
        a.setActualDoneAt(new Date());
        assertThat(service.transit(1L, "DONE", "op").getStatus()).isEqualTo("DONE");
        a.setStatus("IN_PROGRESS");
        assertThat(service.transit(1L, "NA", "op").getStatus()).isEqualTo("NA");
    }

    @Test
    @DisplayName("D11 完成必登实测 FAR/FRR（v3 例外二数值登记）")
    void d11RequiresFarFrr() {
        StageAction a = seed("D11", "LIGHT");
        a.setActualDoneAt(new Date());
        assertThatThrownBy(() -> service.transit(1L, "DONE", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("FAR/FRR");
        a.setFarValue(new java.math.BigDecimal("0.0001"));
        a.setFrrValue(new java.math.BigDecimal("0.02"));
        assertThat(service.transit(1L, "DONE", "op").getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("V02 完成必登证书编号+通过日期（v3 例外一登记不传扫描件）")
    void v02RequiresCertFields() {
        StageAction a = seed("V02", "LIGHT");
        a.setActualDoneAt(new Date());
        assertThatThrownBy(() -> service.transit(1L, "DONE", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("证书编号");
        a.setCertNo("CE-2026-001");
        a.setCertPassedAt(new Date());
        assertThat(service.transit(1L, "DONE", "op").getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("instantiate 幂等：同项目同编码已存在全跳过（CONPECT 12 项全查重）")
    void instantiateIdempotent() {
        when(actionMapper.selectCount(any())).thenReturn(1L); // 连续调用恒返回已存在
        service.instantiate(100L, 10L, "CONCEPT");
        Mockito.verify(actionMapper, Mockito.times(12)).selectCount(any()); // CONCEPT 12 项逐条查重
    }

    @Test
    @DisplayName("instantiate 全新项目：目录 12 项全量生成且阻断位/深度随目录")
    void instantiateFreshCreatesAll() {
        when(actionMapper.selectCount(any())).thenReturn(0L);
        when(actionMapper.insert(any(StageAction.class))).thenAnswer(inv -> {
            StageAction a = inv.getArgument(0);
            a.setId(99L);
            return 1;
        });
        int created = service.instantiate(100L, 10L, "CONCEPT");
        assertThat(created).isEqualTo(12); // 阶段分布锚：CONCEPT=12
    }

    @Test
    @DisplayName("交付物登记写审计并回填项目冗余")
    void addDeliverableWrites() {
        seed("C01", "DEEP");
        when(deliverableMapper.insert(any(Deliverable.class))).thenAnswer(inv -> {
            Deliverable d = inv.getArgument(0);
            d.setId(77L);
            return 1;
        });
        Deliverable d = service.addDeliverable(1L, "市场调研报告.pdf", null, "op");
        assertThat(d.getProjectId()).isEqualTo(100L);
        assertThat(d.getActionId()).isEqualTo(1L);
        Mockito.verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("C12 强制挂载联动：涉生物项目且 C12 缺失 → 自动补挂（阻断深管）")
    void c12MountWhenBio() {
        when(actionMapper.selectCount(any())).thenReturn(1L, 0L);
        when(actionMapper.insert(any(StageAction.class))).thenAnswer(inv -> {
            StageAction a = inv.getArgument(0);
            a.setId(55L);
            return 1;
        });
        assertThat(service.ensureBioComplianceMount(100L)).isEqualTo(1);
        Mockito.verify(actionMapper).insert(Mockito.argThat((StageAction a) ->
            "C12".equals(a.getActionCode()) && "1".equals(a.getIsBlocking())));
    }

    @Test
    @DisplayName("C12 联动：无涉生物动作不补挂；C12 已挂不重复补挂")
    void c12MountGuards() {
        when(actionMapper.selectCount(any())).thenReturn(0L);
        assertThat(service.ensureBioComplianceMount(100L)).isZero();
        Mockito.verify(actionMapper, Mockito.never()).insert(org.mockito.ArgumentMatchers.<StageAction>any());
        when(actionMapper.selectCount(any())).thenReturn(1L, 1L);
        assertThat(service.ensureBioComplianceMount(100L)).isZero();
    }
}