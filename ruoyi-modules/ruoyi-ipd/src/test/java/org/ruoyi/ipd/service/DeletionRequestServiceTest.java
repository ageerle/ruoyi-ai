package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 删除审核引擎状态机单测（BR-DEL/F29；@Tag("dev") 必须）
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class DeletionRequestServiceTest {

    @Mock
    private DeletionRequestMapper deletionRequestMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private DeleteAuditService deleteAuditService;
    /** ROOT-R3-P0-1 修复：跨状态机守卫 mock（fail-closed 改造后必显式注入，否则 preCheckGuard 抛 IpdBusinessException） */
    @Mock
    private StateMachineGuard stateMachineGuard;

    private DeletionRequestService service;

    /** PERF-P0-1：纯 JVM 单测无 MP 运行时，手动初始化 lambda 列缓存（LambdaUpdateWrapper.set/.eq 需列名解析） */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, DeletionRequest.class);
    }

    @BeforeEach
    void setUp() {
        service = new DeletionRequestService(
            deletionRequestMapper, systemConfigService, auditLogService, deleteAuditService);
        // ROOT-R3-P0-1 修复：注入 mock 守卫（fail-closed 改造后，preCheckGuard 必显式 fail-fast）
        service.setStateMachineGuard(stateMachineGuard);
    }

    private DeletionRequest saved(Long id, String status, Date createTime, Date leaderDueAt) {
        DeletionRequest request = DeletionRequest.builder()
            .id(id).entityType("projects").entityId(100L).reason("测试删除")
            .requesterId(1L).status(status).leaderDueAt(leaderDueAt)
            .build();
        request.setCreateTime(createTime);
        return request;
    }

    @Test
    @DisplayName("提交 → LEADER_REVIEW，期限 = 2 个工作日（跳过周末）")
    void submitGoesToLeaderReviewWithWorkdayDeadline() {
        when(systemConfigService.getIntValue("deletion.leaderDeadlineDays", 2)).thenReturn(2);
        // 周五提交（2026-09-04 是周五）→ +2 工作日 = 下周二
        Date friday = date(2026, Calendar.SEPTEMBER, 4);
        DeletionRequest request = service.submit("projects", 100L, "{}", "测试删除", 1L);

        assertThat(request.getStatus()).isEqualTo(DeletionRequestService.ST_LEADER_REVIEW);
        Calendar due = Calendar.getInstance();
        due.setTime(request.getLeaderDueAt());
        // 周五 +1 工作日 = 周一(7)，+2 = 周二(8)
        assertThat(due.get(Calendar.DATE)).isEqualTo(8);
        verify(deletionRequestMapper).insert(any(DeletionRequest.class));
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("组长通过 → ADMIN_REVIEW 并设终审期限；审计写入")
    void leaderApproveMovesToAdminReview() {
        when(systemConfigService.getIntValue("deletion.adminDeadlineDays", 2)).thenReturn(2);
        DeletionRequest request = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);

        DeletionRequest after = service.leaderDecision(9L, 5L, true, "同意");

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_ADMIN_REVIEW);
        assertThat(after.getLeaderDecision()).isEqualTo("APPROVE");
        assertThat(after.getAdminDueAt()).isNotNull();
    }

    @Test
    @DisplayName("组长否决 → 终态 REJECTED，不设终审期限")
    void leaderRejectIsTerminal() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);

        DeletionRequest after = service.leaderDecision(9L, 5L, false, "不同意");

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_REJECTED);
        assertThat(after.getAdminDueAt()).isNull();
    }

    @Test
    @DisplayName("状态机不匹配：REJECTED 单不可再终审")
    void stateMachineGuardsWrongTransition() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_REJECTED, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);

        assertThatThrownBy(() -> service.adminDecision(9L, 2L, true, "x"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("状态机不匹配");
    }

    @Test
    @DisplayName("超管通过 → 委托 DeleteAuditService.approveAndExecute（P0-6.2 原子软删）")
    void adminApproveExecutes() {
        DeletionRequest request = saved(9L, DeletionRequestService.ST_ADMIN_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(request);
        DeletionRequest executed = saved(9L, DeletionRequestService.ST_DELETED, new Date(), null);
        executed.setExecutedAt(new Date());
        executed.setAdminDecision("APPROVE");
        when(deleteAuditService.approveAndExecute(9L, 2L)).thenReturn(executed);

        DeletionRequest after = service.adminDecision(9L, 2L, true, "同意删除");

        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_DELETED);
        assertThat(after.getExecutedAt()).isNotNull();
        assertThat(after.getAdminDecision()).isEqualTo("APPROVE");
        verify(deleteAuditService).approveAndExecute(9L, 2L);
    }

    @Test
    @DisplayName("PERF-P0-1 逾期升级：单 SQL 条件 UPDATE + 补逐条审计（不被逐条 updateById）")
    void escalateOverdue() {
        DeletionRequest overdue = saved(11L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), new Date(System.currentTimeMillis() - 86400_000L));
        when(deletionRequestMapper.selectList(any())).thenReturn(List.of(overdue));
        when(deletionRequestMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(systemConfigService.getIntValue("deletion.adminDeadlineDays", 2)).thenReturn(2);

        int count = service.escalateOverdueLeaderReview();

        assertThat(count).isEqualTo(1);
        // 步骤 ②：单 SQL 条件批量 UPDATE 被调一次（带 LambdaUpdateWrapper）
        verify(deletionRequestMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
        // PERF-P0-1：禁止逐条 updateById（消除 N+1 写放大）
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        // 步骤 ③：受影响行逐条补审计（G-02 语义不变）
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("PERF-P0-1 affected=0 短路：无逾期时零 SQL 额外开销（不调 UPDATE、不调 audit）")
    void escalateOverdueShortCircuitOnNoOverdue() {
        // selectList 返回空 → 直接返回 0，UPDATE 与 audit 都不被调用
        when(deletionRequestMapper.selectList(any())).thenReturn(List.of());

        int count = service.escalateOverdueLeaderReview();

        assertThat(count).isEqualTo(0);
        verify(deletionRequestMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("撤回时限：24h 内可撤回，非申请人不可撤")
    void withdrawGuard() {
        DeletionRequest recent = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, new Date(), null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(recent);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdraw(9L, 99L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("仅申请人");

        DeletionRequest after = service.withdraw(9L, 1L);
        assertThat(after.getStatus()).isEqualTo(DeletionRequestService.ST_WITHDRAWN);
    }

    @Test
    @DisplayName("撤回时限：超 24h 拒绝")
    void withdrawAfterDeadlineRejected() {
        Date old = new Date(System.currentTimeMillis() - 25 * 3600_000L);
        DeletionRequest oldRequest = saved(9L, DeletionRequestService.ST_LEADER_REVIEW, old, null);
        when(deletionRequestMapper.selectById(9L)).thenReturn(oldRequest);
        when(systemConfigService.getIntValue("deletion.withdrawHours", 24)).thenReturn(24);

        assertThatThrownBy(() -> service.withdraw(9L, 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("撤回时限");
    }

    private static Date date(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d, 10, 0, 0);
        return c.getTime();
    }
}