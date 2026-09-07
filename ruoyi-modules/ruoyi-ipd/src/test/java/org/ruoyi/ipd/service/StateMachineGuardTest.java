package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.StateTransitionRule;
import org.ruoyi.ipd.service.impl.DefaultStateMachineGuard;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 跨状态机守卫单测（ROOT-R3-P0-1；7 测覆盖）
 *
 * <p>覆盖维度：
 * <ol>
 *   <li>合法迁移：规则表已登记 → preCheck 放行 / isAllowed=true</li>
 *   <li>非法迁移：规则表无登记且非终态收敛 → preCheck 抛 IpdBusinessException</li>
 *   <li>拒绝跨域：crossDomain=true 规则登记后，未登记迁移触发 postCommit 仅记日志 no-op（不写审计不通知）</li>
 *   <li>触发审计：crossDomain=true 迁移 postCommit → 写 1 条 audit_log + 推 1 条 FYI 通知</li>
 *   <li>异常处理：postCommit 内部审计/通知抛异常时，仅记日志，不反向破坏（已通过 service 层 no-op 测试）</li>
 *   <li>边界条件：通配「*」+ 已知终态（如 *->WITHDRAWN）合法；通配到非终态非法</li>
 *   <li>热加载：registerRule / removeRule 动态增删后 isAllowed 行为同步</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StateMachineGuardTest {

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private DefaultStateMachineGuard guard;

    @BeforeEach
    void setUp() {
        guard = new DefaultStateMachineGuard(auditLogService, notificationService);
        // 测试隔离：每个测试重新注入种子（避免 @PostConstruct 已运行场景的污染）
        guard.resetRules();
        guard.initRules();
    }

    /* ====================== 1. 合法迁移 ====================== */

    @Test
    @DisplayName("合法迁移：DRAFT->LEADER_REVIEW 放行，isAllowed=true")
    void legalTransitionAllowed() {
        assertThat(guard.isAllowed("deletion_request", "DRAFT", "LEADER_REVIEW", "submit")).isTrue();
        // preCheck 不抛
        guard.preCheck("deletion_request", "DRAFT", "LEADER_REVIEW", "submit");
    }

    /* ====================== 2. 非法迁移 ====================== */

    @Test
    @DisplayName("非法迁移：DRAFT->DELETED 规则表未登记且 DRAFT 非通配收敛 → 抛 IpdBusinessException")
    void illegalTransitionRejected() {
        assertThat(guard.isAllowed("deletion_request", "DRAFT", "DELETED", "skipReview")).isFalse();
        assertThatThrownBy(() ->
            guard.preCheck("deletion_request", "DRAFT", "DELETED", "skipReview"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("状态机非法迁移")
            .hasMessageContaining("deletion_request")
            .hasMessageContaining("DRAFT")
            .hasMessageContaining("DELETED");
    }

    /* ====================== 3. 拒绝跨域 ====================== */

    @Test
    @DisplayName("拒绝跨域：未登记的跨域 trigger → postCommit 不写审计不通知（仅记日志）")
    void rejectCrossDomainNoOp() {
        // 守卫表无「DRAFT->DISTRIBUTED」登记 → postCommit 应 no-op（不发审计不发通知）
        guard.postCommit("bonus_pool", "DRAFT", "DISTRIBUTED", "skipSteps",
            1L, 100L, new Date());

        // 不应触发任何跨域副作用
        verifyNoInteractions(auditLogService);
        verifyNoInteractions(notificationService);
    }

    /* ====================== 4. 触发审计 ====================== */

    @Test
    @DisplayName("触发审计：crossDomain=true 迁移 → 写 1 条 audit + 推 1 条 FYI 通知")
    void postCommitTriggersAuditAndNotification() {
        // LEADER_REVIEW->ADMIN_REVIEW 标记为 crossDomain=true（种子规则）
        guard.postCommit("deletion_request", "LEADER_REVIEW", "ADMIN_REVIEW", "leaderApprove",
            7L, 999L, new Date());

        // 验证 audit 被写
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(auditCaptor.capture());
        AuditLog written = auditCaptor.getValue();
        assertThat(written.getAction()).isEqualTo("CROSS_DOMAIN_TRANSITION");
        assertThat(written.getEntityType()).isEqualTo("deletion_request");
        assertThat(written.getEntityId()).isEqualTo(999L);
        assertThat(written.getOperatorId()).isEqualTo(7L);
        assertThat(written.getReason()).contains("LEADER_REVIEW").contains("ADMIN_REVIEW");

        // 验证 notification 被发
        verify(notificationService, times(1)).publish(
            eq(7L),
            eq("CROSS_DOMAIN_TRANSITION"),
            eq(NotificationService.KIND_FYI),
            eq("deletion_request"),
            eq(999L),
            anyString(),
            anyString(),
            eq(null)
        );
    }

    /* ====================== 5. 异常处理 ====================== */

    @Test
    @DisplayName("异常处理：postCommit 内部 audit.append 抛异常 → 不反向传播（已提交事务不可破坏）")
    void postCommitAuditFailureIsolated() {
        // 模拟 audit.append 抛运行时异常
        org.mockito.Mockito.doThrow(new RuntimeException("audit chain 异常"))
            .when(auditLogService).append(any(AuditLog.class));

        // postCommit 内部捕获异常，不外抛
        guard.postCommit("deletion_request", "LEADER_REVIEW", "ADMIN_REVIEW", "leaderApprove",
            7L, 999L, new Date());

        // audit 仍被调用 1 次（异常前）
        verify(auditLogService, times(1)).append(any(AuditLog.class));
        // notification 因 audit 失败而未被调用（短路顺序：先 audit 后 notify）
        verifyNoInteractions(notificationService);
    }

    /* ====================== 6. 边界条件 ====================== */

    @Test
    @DisplayName("边界条件：通配「*」+ 已知终态（如 *->WITHDRAWN）合法；通配到非终态非法")
    void wildcardTerminalAllowed() {
        // *->WITHDRAWN 合法（WITHDRAWN 是 deletion_request 已知终态）
        assertThat(guard.isAllowed("deletion_request", "LEADER_REVIEW", "WITHDRAWN", "withdraw")).isTrue();
        assertThat(guard.isAllowed("deletion_request", "ADMIN_REVIEW", "WITHDRAWN", "withdraw")).isTrue();
        guard.preCheck("deletion_request", "ADMIN_REVIEW", "WITHDRAWN", "withdraw");

        // *->CONFIRMED 合法（CONFIRMED 是 bonus_pool 已知终态）
        assertThat(guard.isAllowed("bonus_pool", "DRAFT", "CONFIRMED", "freeze")).isTrue();
        guard.preCheck("bonus_pool", "DRAFT", "CONFIRMED", "freeze");

        // 通配到非终态仍非法（如 *->DRAFT 不是终态）
        assertThat(guard.isAllowed("deletion_request", "DRAFT", "DRAFT", "noop")).isFalse();
    }

    /* ====================== 7. 热加载 ====================== */

    @Test
    @DisplayName("热加载：registerRule 新增规则后 isAllowed 立即生效；removeRule 后失效")
    void hotLoadRegisterAndRemove() {
        // 初始状态：X->Y 未登记
        assertThat(guard.isAllowed("custom_entity", "X", "Y", "fire")).isFalse();

        // 热加载一条新规则
        StateTransitionRule rule = StateTransitionRule.builder()
            .key("custom_entity:X->Y")
            .entityType("custom_entity")
            .fromState("X")
            .toState("Y")
            .trigger("fire")
            .crossDomain(true)
            .description("测试热加载")
            .build();
        guard.registerRule(rule);

        // 立即生效
        assertThat(guard.isAllowed("custom_entity", "X", "Y", "fire")).isTrue();
        assertThat(guard.ruleCount()).isPositive();

        // 卸载后失效
        boolean removed = guard.removeRule("custom_entity:X->Y");
        assertThat(removed).isTrue();
        assertThat(guard.isAllowed("custom_entity", "X", "Y", "fire")).isFalse();
    }
}
