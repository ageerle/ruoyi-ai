package org.ruoyi.ipd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 跨状态机迁移规则（ROOT-R3-P0-1 StateMachineGuard）
 *
 * <p>用于描述「某个实体类型（deletion_request / bonus_pool / project ...）在某源状态（fromState）
 * 经某触发动作（trigger）可向目标状态（toState）迁移」的合法性。守卫层
 * {@link org.ruoyi.ipd.service.StateMachineGuard} 在写入前/提交后据此判定。
 *
 * <p><b>为何独立于具体状态机实现：</b>DeletionRequestService / BonusPoolService 各自内嵌状态机
 * （DRAFT→LEADER_REVIEW→ADMIN_REVIEW→DELETED/REJECTED 与 DRAFT→CONFIRMED→DISTRIBUTED），
 * 跨服务协同时存在「删一条 DeletionRequest 即冻结其关联 BonusPool」等联动场景。
 * 守卫层把这些跨域联动收敛到一张规则表，避免各服务互相感知具体状态枚举。
 *
 * <p><b>不可落库</b>：本类是守卫层的内存规则载体（{@code ConcurrentHashMap} 种子由
 * {@link org.ruoyi.ipd.service.impl.DefaultStateMachineGuard#initRules} 注入），
 * 严禁走 MyBatis-Plus 持久化（避免引入与状态机主表争抢的额外 DDL 漂移面）。
 *
 * <p>字段：
 * <ul>
 *   <li>{@code key} 唯一键："{entityType}:{fromState}->{toState}"（如 "deletion_request:LEADER_REVIEW->ADMIN_REVIEW"）</li>
 *   <li>{@code entityType} 实体类型（deletion_request / bonus_pool ...）</li>
 *   <li>{@code fromState} 源状态；"*" 表示通配（任何状态都可向 toState 迁移，仅限终态收敛）</li>
 *   <li>{@code toState} 目标状态</li>
 *   <li>{@code trigger} 触发动作（leaderDecision / adminDecision / freeze / distribute ...），null 表示不限定</li>
 *   <li>{@code crossDomain} 是否跨域迁移（true=触发外部副作用审计+通知；false=仅做合法性校验）</li>
 *   <li>{@code guardClass} 自定义守卫类全限定名（可选；null=走默认合法性校验，无业务强约束）</li>
 *   <li>{@code description} 规则语义说明（仅用于调试与错误消息）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateTransitionRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通配源状态：表示「任何状态可向 toState 迁移」——通常用于终态（如 DELETED、REJECTED） */
    public static final String FROM_ANY = "*";

    private String key;
    private String entityType;
    private String fromState;
    private String toState;
    /** 触发动作（leaderDecision/adminDecision/freeze/distribute/...）；null=不限定 */
    private String trigger;
    /** 是否跨域迁移（true=触发外部副作用审计+通知；false=仅做合法性校验） */
    private boolean crossDomain;
    /** 自定义守卫类全限定名（可选） */
    private String guardClass;
    /** 规则语义说明（用于调试与错误消息） */
    private String description;
}
