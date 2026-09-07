package org.ruoyi.ipd.service;

import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.StateTransitionRule;

import java.util.Date;

/**
 * 跨状态机守卫层（ROOT-R3-P0-1）
 *
 * <p>两阶段守卫：
 * <ol>
 *   <li><b>preCheck</b>：写入数据库前调用，按 {@link StateTransitionRule} 判定 (entityType, fromState, toState, trigger)
 *       是否为合法迁移，非法直接抛 {@link IpdBusinessException}；拒绝跨域 = 守卫表里根本没这条规则、且
 *       {@code crossDomain=true}（防止某服务绕过规则表直接写跨域副作用）</li>
 *   <li><b>postCommit</b>：数据库事务提交后调用，触发审计 + 通知（仅当规则标记 crossDomain=true）</li>
 * </ol>
 *
 * <p>两阶段分离的目的：preCheck 必须失败-快、不写库、不发通知；postCommit 不可在事务回滚后被调用
 * （违反会污染审计链 / 重复通知）。调用方约定在 {@code @Transactional} 方法内部 preCheck，事务
 * {@code commit} 之后才 postCommit——具体编排由 Service 自己保证。
 *
 * <p>实现：{@link org.ruoyi.ipd.service.impl.DefaultStateMachineGuard}。规则元数据走内存
 * {@code ConcurrentHashMap}，由 {@code initRules()} 种子注入，运行时可调 {@link #registerRule}
 * 热加载新规则。
 */
public interface StateMachineGuard {

    /**
     * 注册一条规则（热加载；同 key 覆盖）。
     *
     * @param rule 规则实体
     */
    void registerRule(StateTransitionRule rule);

    /**
     * 按 key 移除一条规则（热卸载；用于规则下线/回归测试）。
     *
     * @param key 规则 key
     * @return true=移除成功，false=key 不存在
     */
    boolean removeRule(String key);

    /**
     * 查询迁移是否被规则表登记。
     *
     * @param entityType 实体类型
     * @param fromState  源状态
     * @param toState    目标状态
     * @param trigger    触发动作；null=查询不限定 trigger 的规则
     * @return true=登记在案（合法迁移或终态收敛）
     */
    boolean isAllowed(String entityType, String fromState, String toState, String trigger);

    /**
     * preCheck：写入前合法性校验。
     *
     * <p>判定逻辑：
     * <ol>
     *   <li>查询规则表 (entityType, fromState, toState, trigger)；找到 → 合法放行</li>
     *   <li>未找到：若 fromState=通配「*」且 toState 是该 entityType 的已知终态（由实现判定）→ 视为终态收敛（合法）</li>
     *   <li>其余：抛 {@link IpdBusinessException}，文案含 entityType/fromState/toState/trigger 便于排查</li>
     * </ol>
     *
     * @param entityType 实体类型
     * @param fromState  源状态
     * @param toState    目标状态
     * @param trigger    触发动作；null=不限定
     * @throws IpdBusinessException 非法迁移时抛出
     */
    void preCheck(String entityType, String fromState, String toState, String trigger);

    /**
     * postCommit：事务提交后副作用（审计 + 通知）。
     *
     * <p>按规则表的 crossDomain 字段分流：
     * <ul>
     *   <li>crossDomain=true：写 1 条 audit_log（action="CROSS_DOMAIN_TRANSITION"）+ 推 1 条 FYI 通知
     *       给 entityType 关联接收者（实现可下发到 NotificationService）</li>
     *   <li>crossDomain=false：仅返回（不写审计、不发通知）</li>
     * </ul>
     *
     * <p>如果规则未登记则 no-op（不抛——postCommit 在事务后，失败也只能记日志，不能把已提交的事务
     * 「反向」破坏）。
     *
     * @param entityType 实体类型
     * @param fromState  源状态
     * @param toState    目标状态
     * @param trigger    触发动作
     * @param operatorId 操作人 ID（审计与通知使用）
     * @param entityId   业务实体 ID
     * @param occurredAt 发生时间（用于审计与通知的时间戳）
     */
    void postCommit(String entityType, String fromState, String toState, String trigger,
                    Long operatorId, Long entityId, Date occurredAt);
}
