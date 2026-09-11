package org.ruoyi.ipd.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.StateTransitionRule;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.NotificationService;
import org.ruoyi.ipd.service.StateMachineGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认跨状态机守卫（ROOT-R3-P0-1；规则元数据走内存 ConcurrentHashMap）
 *
 * <p>设计原则：
 * <ul>
 *   <li>规则元数据全部在内存（ConcurrentHashMap），无新增表（与任务「不动 DDL」一致）</li>
 *   <li>种子规则由 {@link #initRules()} 在 Spring {@code @PostConstruct} 阶段注入，覆盖：
 *       DeletionRequestService 6 条合法迁移 + BonusPoolService 3 条合法迁移 + 4 条终态收敛通配</li>
 *   <li>preCheck 三段判定：① 精确匹配 ② 通配「*」+ 已知终态收敛 ③ 其余非法抛 IpdBusinessException</li>
 *   <li>postCommit 按 crossDomain 分流：true=写 audit + 推 FYI 通知；false=no-op</li>
 *   <li>postCommit 失败只记日志、不抛（已提交的事务不可回滚，避免污染审计链）</li>
 * </ul>
 *
 * <p>关键不变量：
 * <ul>
 *   <li>并发安全：ConcurrentHashMap + 不可变 StateTransitionRule（lombok @Data 但注册前 builder
 *       已固化；不修改既有规则）</li>
 *   <li>零依赖循环：构造函数注入只接 AuditLogService + NotificationService（两个底层组件），
 *       不反向依赖任何上层业务 Service</li>
 *   <li>测试隔离：单元测试在 {@code @BeforeEach} 调用 {@link #resetRules()} 清空后再种</li>
 * </ul>
 */
@Slf4j
@Component
public class DefaultStateMachineGuard implements StateMachineGuard {

    /** DeletionRequest 已知终态：DELETED / REJECTED / WITHDRAWN */
    private static final Set<String> DELETION_TERMINAL =
        Set.of("DELETED", "REJECTED", "WITHDRAWN");
    /** BonusPool 已知终态：CONFIRMED（freeze 后不可再变） / DISTRIBUTED */
    private static final Set<String> BONUS_POOL_TERMINAL =
        Set.of("CONFIRMED", "DISTRIBUTED");

    /** 规则表：key = "{entityType}:{fromState}->{toState}" */
    private final Map<String, StateTransitionRule> rules = new ConcurrentHashMap<>();

    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Autowired
    public DefaultStateMachineGuard(AuditLogService auditLogService,
                                    NotificationService notificationService) {
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Spring 启动后注入种子规则（覆盖 DeletionRequest / BonusPool 状态机全量合法迁移 + 终态收敛）
     */
    @PostConstruct
    public void initRules() {
        // ---- DeletionRequest 状态机：DRAFT→LEADER_REVIEW→ADMIN_REVIEW→DELETED/REJECTED；WITHDRAWN ----
        register(StateTransitionRule.builder()
            .key("deletion_request:DRAFT->LEADER_REVIEW|submit")
            .entityType("deletion_request")
            .fromState("DRAFT")
            .toState("LEADER_REVIEW")
            .trigger("submit")
            .crossDomain(false)
            .description("提交删除申请进入组长初审")
            .build());
        register(StateTransitionRule.builder()
            .key("deletion_request:LEADER_REVIEW->ADMIN_REVIEW|leaderApprove")
            .entityType("deletion_request")
            .fromState("LEADER_REVIEW")
            .toState("ADMIN_REVIEW")
            .trigger("leaderApprove")
            .crossDomain(true)        // 跨域：触发超管待办
            .description("组长通过进入超管终审（跨域→推超管 ACTION 通知）")
            .build());
        register(StateTransitionRule.builder()
            .key("deletion_request:LEADER_REVIEW->REJECTED|leaderReject")
            .entityType("deletion_request")
            .fromState("LEADER_REVIEW")
            .toState("REJECTED")
            .trigger("leaderReject")
            .crossDomain(true)        // 跨域：通知申请人
            .description("组长驳回（跨域→通知申请人）")
            .build());
        register(StateTransitionRule.builder()
            .key("deletion_request:ADMIN_REVIEW->DELETED|adminApprove")
            .entityType("deletion_request")
            .fromState("ADMIN_REVIEW")
            .toState("DELETED")
            .trigger("adminApprove")
            .crossDomain(true)        // 跨域：触发目标行软删副作用
            .description("超管通过触发原子软删（跨域→触发 DeleteAuditService）")
            .build());
        register(StateTransitionRule.builder()
            .key("deletion_request:ADMIN_REVIEW->REJECTED|adminReject")
            .entityType("deletion_request")
            .fromState("ADMIN_REVIEW")
            .toState("REJECTED")
            .trigger("adminReject")
            .crossDomain(true)        // 跨域：通知申请人
            .description("超管驳回（跨域→通知申请人）")
            .build());
        register(StateTransitionRule.builder()
            .key("deletion_request:*->WITHDRAWN|withdraw")
            .entityType("deletion_request")
            .fromState(StateTransitionRule.FROM_ANY)
            .toState("WITHDRAWN")
            .trigger("withdraw")
            .crossDomain(false)
            .description("申请人 24h 内撤回（终态收敛）")
            .build());
        register(StateTransitionRule.builder()
            .key("deletion_request:LEADER_REVIEW->ADMIN_REVIEW|escalateOverdue")
            .entityType("deletion_request")
            .fromState("LEADER_REVIEW")
            .toState("ADMIN_REVIEW")
            .trigger("escalateOverdue")
            .crossDomain(false)
            .description("组长逾期自动升级超管（PERF-P0-1 批量 UPDATE）")
            .build());

        // ---- BonusPool 状态机：DRAFT→CONFIRMED→DISTRIBUTED ----
        // P1-4 语义化：注册 INITIAL→DRAFT 创建迁移（业务语义：create new bonus pool）
        // 守卫层 preCheck 接受 fromState=Java null，isAllowed 内部映射到字面量 "INITIAL" 再与规则表 key 拼接
        // 修复前 BonusPoolService.compute 真环境 fail-closed 400「守卫层未登记该迁移」
        register(StateTransitionRule.builder()
            .key("bonus_pool:INITIAL->DRAFT|compute")
            .entityType("bonus_pool")
            .fromState("INITIAL")     // P1-4 语义化:创建迁移 = INITIAL 状态
            .toState("DRAFT")
            .trigger("compute")
            .crossDomain(false)
            .description("奖金池 compute：INITIAL→DRAFT（初始创建迁移，业务新建）")
            .build());
        register(StateTransitionRule.builder()
            .key("bonus_pool:DRAFT->CONFIRMED|freeze")
            .entityType("bonus_pool")
            .fromState("DRAFT")
            .toState("CONFIRMED")
            .trigger("freeze")
            .crossDomain(false)
            .description("奖金池 freeze：DRAFT→CONFIRMED（冻结后禁止回滚）")
            .build());
        register(StateTransitionRule.builder()
            .key("bonus_pool:CONFIRMED->DISTRIBUTED|distribute")
            .entityType("bonus_pool")
            .fromState("CONFIRMED")
            .toState("DISTRIBUTED")
            .trigger("distribute")
            .crossDomain(true)        // 跨域：触发津贴账本写入
            .description("奖金池 distribute：CONFIRMED→DISTRIBUTED（跨域→写入 AllowanceLedger）")
            .build());

        log.info("StateMachineGuard 种子规则注入完成：{} 条", rules.size());
    }

    /**
     * 测试隔离用：清空所有规则（仅测试场景调用，生产路径由 initRules 一次性种子）
     */
    public void resetRules() {
        rules.clear();
    }

    /** 注册（供外部热加载与 initRules 复用） */
    private void register(StateTransitionRule rule) {
        registerRule(rule);
    }

    @Override
    public void registerRule(StateTransitionRule rule) {
        if (rule == null || rule.getKey() == null) {
            throw new IllegalArgumentException("规则或 key 不可为空");
        }
        rules.put(rule.getKey(), rule);
    }

    @Override
    public boolean removeRule(String key) {
        return rules.remove(key) != null;
    }

    @Override
    public boolean isAllowed(String entityType, String fromState, String toState, String trigger) {
        if (entityType == null || toState == null) {
            return false;
        }
        // P1-4 语义化：Java null 表示"创建迁移"，对应规则表里的字面量 "INITIAL"
        String fromKey = fromState == null ? "INITIAL" : fromState;
        // ① 精确匹配（trigger 拼入 key 消除 (from,to) 同名多 trigger 冲突）
        String trigPart = trigger == null ? "" : "|" + trigger;
        StateTransitionRule exact = rules.get(entityType + ":" + fromKey + "->" + toState + trigPart);
        if (exact != null) {
            return true;
        }
        // ② 通配「*」+ 已知终态收敛
        StateTransitionRule wildcard = rules.get(entityType + ":*->" + toState + trigPart);
        if (wildcard != null && isTerminalState(entityType, toState)) {
            return true;
        }
        return false;
    }

    @Override
    public void preCheck(String entityType, String fromState, String toState, String trigger) {
        if (isAllowed(entityType, fromState, toState, trigger)) {
            return;
        }
        // 跨域守卫：拒绝不在规则表里的跨域联动（防止某服务绕过规则表写跨域副作用）
        throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, String.format(
            "状态机非法迁移：entityType=%s, from=%s, to=%s, trigger=%s（守卫层未登记该迁移）",
            entityType, fromState, toState, trigger));
    }

    @Override
    public void postCommit(String entityType, String fromState, String toState, String trigger,
                           Long operatorId, Long entityId, Date occurredAt) {
        if (!isAllowed(entityType, fromState, toState, trigger)) {
            // postCommit 在事务后，非法迁移不能反向破坏已提交事务；记日志 no-op
            log.warn("postCommit 收到未登记迁移，no-op：entityType={}, from={}, to={}, trigger={}",
                entityType, fromState, toState, trigger);
            return;
        }
        // 找到精确规则或通配规则（trigger 拼入 key）
        String trigPart = trigger == null ? "" : "|" + trigger;
        StateTransitionRule rule = rules.get(entityType + ":" + fromState + "->" + toState + trigPart);
        if (rule == null) {
            rule = rules.get(entityType + ":*->" + toState + trigPart);
        }
        if (rule == null || !rule.isCrossDomain()) {
            return; // 非跨域：no-op（不发审计、不发通知）
        }
        // 跨域：写 audit_log（独立事务，失败不抛）+ 推 FYI 通知
        try {
            Date ts = occurredAt == null ? new Date() : occurredAt;
            AuditLog audit = AuditLog.builder()
                .operatorId(operatorId)
                .action("CROSS_DOMAIN_TRANSITION")
                .entityType(entityType)
                .entityId(entityId)
                .reason("from=" + fromState + ",to=" + toState + ",trigger=" + trigger)
                .createTime(ts)
                .build();
            auditLogService.append(audit);
            // FYI 通知：发给操作人自己（避免空指针，跨域触发的接收者由业务方决定；本守卫仅留痕）
            if (operatorId != null) {
                String content = "from=" + fromState + ",to=" + toState + ",trigger=" + trigger
                    + ",entityId=" + entityId;
                notificationService.publish(operatorId, "CROSS_DOMAIN_TRANSITION",
                    NotificationService.KIND_FYI,
                    entityType, entityId,
                    "跨域状态机迁移：" + entityType + " " + fromState + "→" + toState,
                    content, null);
            }
        } catch (Exception ex) {
            // postCommit 失败不能反向破坏事务；只记日志
            log.error("postCommit 副作用执行失败：entityType={}, from={}, to={}, trigger={}, err={}",
                entityType, fromState, toState, trigger, ex.getMessage(), ex);
        }
    }

    /**
     * 判定 toState 是否为该 entityType 的已知终态（仅用于「*」通配收敛）
     */
    private boolean isTerminalState(String entityType, String toState) {
        if ("deletion_request".equals(entityType)) {
            return DELETION_TERMINAL.contains(toState);
        }
        if ("bonus_pool".equals(entityType)) {
            return BONUS_POOL_TERMINAL.contains(toState);
        }
        return false;
    }

    /**
     * 测试辅助：返回当前规则数量（生产代码禁止使用）
     */
    public int ruleCount() {
        return rules.size();
    }

    /**
     * 兼容 ServiceException（保留旧路径调用方的可能性）
     */
    public void preCheckOrThrowServiceException(String entityType, String fromState, String toState, String trigger) {
        if (!isAllowed(entityType, fromState, toState, trigger)) {
            throw new ServiceException("状态机非法迁移：" + entityType + " " + fromState + "→" + toState
                + " (trigger=" + trigger + ")");
        }
    }
}
