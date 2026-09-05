package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-4.3 状态机 + 幂等 + NA 必 reason + 乐观锁 + 审计。
 * - 深管 NOT_STARTED → IN_PROGRESS → DONE/NA/DELAYED
 * - 轻管禁 DELAYED
 * - NA 必 reason
 * - 幂等：同 id 同 target 不写库、不写审计
 * - 乐观锁：updateById 返回 0 视为冲突
 * - 每次成功迁移写审计 action=TRANSIT
 */
@Tag("dev")
class P143AcceptanceTest {

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

    private StageAction seedDeep(String code, String status) {
        StageAction a = StageAction.builder()
            .id(1L).projectId(100L).stageId(10L).actionCode(code).actionName("t")
            .ownerRole("MARKET_PM").depth("DEEP").status(status)
            .isBlocking("1").isBioFeature("0").version(0)
            .build();
        when(actionMapper.selectById(1L)).thenReturn(a);
        when(actionMapper.updateById(any(StageAction.class))).thenReturn(1);
        return a;
    }

    private StageAction seedLight(String code, String status) {
        StageAction a = StageAction.builder()
            .id(1L).projectId(100L).stageId(10L).actionCode(code).actionName("t")
            .ownerRole("MARKET_PM").depth("LIGHT").status(status)
            .isBlocking("0").isBioFeature("0").version(0)
            .build();
        when(actionMapper.selectById(1L)).thenReturn(a);
        when(actionMapper.updateById(any(StageAction.class))).thenReturn(1);
        return a;
    }

    @Test
    @DisplayName("深管 NOT_STARTED→IN_PROGRESS 通过；非法 target 拒绝")
    void deepStateMachineHappyPath() {
        seedDeep("C01", "NOT_STARTED");
        StageAction out = service.transit(1L, "IN_PROGRESS", null, "op");
        assertThat(out.getStatus()).isEqualTo("IN_PROGRESS");
        assertThatThrownBy(() -> service.transit(1L, "DONE", "ok", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-03");
    }

    @Test
    @DisplayName("深管 IN_PROGRESS→DONE 在交付物登记后通过并写 TRANSIT 审计")
    void deepDoneWritesTransitAudit() {
        seedDeep("C01", "IN_PROGRESS");
        when(deliverableMapper.selectCount(any())).thenReturn(1L);
        StageAction out = service.transit(1L, "DONE", "完成", "op");
        assertThat(out.getStatus()).isEqualTo("DONE");
        assertThat(out.getActualDoneAt()).isNotNull();
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getAction()).isEqualTo("TRANSIT");
        assertThat(log.getEntityType()).isEqualTo("STAGE_ACTION");
        assertThat(log.getEntityId()).isEqualTo(1L);
        assertThat(log.getReason()).isEqualTo("完成");
        assertThat(log.getBeforeData()).contains("\"status\":\"IN_PROGRESS\"");
        assertThat(log.getAfterData()).contains("\"status\":\"DONE\"");
    }

    @Test
    @DisplayName("幂等：同 id 同 target 重复 /transit 不写库、不写审计、返回当前态")
    void idempotentSameTarget() {
        seedDeep("C01", "IN_PROGRESS");
        when(deliverableMapper.selectCount(any())).thenReturn(1L);
        StageAction first = service.transit(1L, "DONE", "完成", "op");
        assertThat(first.getStatus()).isEqualTo("DONE");
        StageAction second = service.transit(1L, "DONE", "重复", "op");
        assertThat(second.getStatus()).isEqualTo("DONE");
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("NA 必须填 reason；缺 reason 拒绝（防绕过）")
    void naRequiresReason() {
        seedDeep("C01", "IN_PROGRESS");
        assertThatThrownBy(() -> service.transit(1L, "NA", null, "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("标记 NA 必须填写原因");
        assertThatThrownBy(() -> service.transit(1L, "NA", "   ", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("标记 NA 必须填写原因");
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("NA 带 reason 通过；深管允许 NA")
    void naWithReason() {
        seedDeep("C01", "IN_PROGRESS");
        StageAction out = service.transit(1L, "NA", "本项目无海外市场", "op");
        assertThat(out.getStatus()).isEqualTo("NA");
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("轻管 NOT_STARTED→DONE 直跳允许（无 IN_PROGRESS 强制）")
    void lightCanSkipInProgressToDone() {
        StageAction a = seedLight("C05", "NOT_STARTED");
        a.setActualDoneAt(new Date());
        StageAction out = service.transit(1L, "DONE", "完成", "op");
        assertThat(out.getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("乐观锁：并发同 id 重复 transit 仅 1 成功（MP 模拟 version 冲突）")
    void optimisticLockConcurrent() {
        seedDeep("C01", "IN_PROGRESS");
        when(deliverableMapper.selectCount(any())).thenReturn(1L);
        // 必须放在 seedDeep 之后；前次 thenReturn(1) 已被覆盖
        when(actionMapper.updateById(any(StageAction.class)))
            .thenReturn(1)   // 第一次成功
            .thenReturn(0);  // 第二次冲突

        StageAction out1 = service.transit(1L, "DONE", "first", "op");
        assertThat(out1.getStatus()).isEqualTo("DONE");

        // 第二次：模拟状态机依旧在 IN_PROGRESS（实际已被事务改），强制再发起
        StageAction stale = StageAction.builder()
            .id(1L).projectId(100L).stageId(10L).actionCode("C01").actionName("t")
            .ownerRole("MARKET_PM").depth("DEEP").status("IN_PROGRESS")
            .isBlocking("1").isBioFeature("0").version(0)
            .build();
        when(actionMapper.selectById(1L)).thenReturn(stale);
        assertThatThrownBy(() -> service.transit(1L, "DONE", "second", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("乐观锁");
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("非法 target 拒绝（深管不应接受超出 DEEP_ALLOWED 集合）")
    void illegalTarget() {
        seedDeep("C01", "IN_PROGRESS");
        assertThatThrownBy(() -> service.transit(1L, "GARBAGE", "x", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("非法目标状态");
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("深管 DONE 已删附件（del_flag!=0）不计入")
    void deepDoneIgnoresDeletedDeliverables() {
        seedDeep("C01", "IN_PROGRESS");
        when(deliverableMapper.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.transit(1L, "DONE", "ok", "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-03");
    }

    @Test
    @DisplayName("轻管 PENDING→NA 带 reason 通过；NA 同样审计")
    void lightNaPasses() {
        seedLight("C05", "NOT_STARTED");
        StageAction out = service.transit(1L, "NA", "不适用", "op");
        assertThat(out.getStatus()).isEqualTo("NA");
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("TRANSIT");
    }
}
