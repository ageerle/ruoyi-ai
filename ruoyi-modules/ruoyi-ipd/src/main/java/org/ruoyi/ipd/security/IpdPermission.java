package org.ruoyi.ipd.security;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.service.IpdAuthService;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;

/**
 * SEC-01接口角色边界。复用IPD会话及状态机，不调用基线sys_user权限。
 * 本人/同组/协同组的数据范围由SEC-02继续接入，角色允许不等于对象允许。
 */
@Component
@RequiredArgsConstructor
public class IpdPermission {
    /** 项目列表 */
    public static final String PROJECT_LIST = "ipd:project:list";
    /** 项目详情 */
    public static final String PROJECT_QUERY = "ipd:project:query";
    /** 新建项目 */
    public static final String PROJECT_ADD = "ipd:project:add";
    /** 变更项目状态 */
    public static final String PROJECT_STATUS = "ipd:project:status";
    /** 推进项目阶段 */
    public static final String PROJECT_ADVANCE = "ipd:project:advance";

    /** 产品列表 */
    public static final String PRODUCT_LIST = "ipd:product:list";
    /** 产品详情 */
    public static final String PRODUCT_QUERY = "ipd:product:query";
    /** 新建产品 */
    public static final String PRODUCT_ADD = "ipd:product:add";
    /** 产品绑定项目 */
    public static final String PRODUCT_BIND = "ipd:product:bind";
    /** 变更产品状态 */
    public static final String PRODUCT_STATUS = "ipd:product:status";

    /** 阶段动作列表 */
    public static final String STAGE_ACTION_LIST = "ipd:stage-action:list";
    /** 阶段动作状态流转 */
    public static final String STAGE_ACTION_TRANSIT = "ipd:stage-action:transit";
    /** 登记交付物 */
    public static final String STAGE_ACTION_DELIVERABLE = "ipd:stage-action:deliverable";
    /** 实例化阶段动作 */
    public static final String STAGE_ACTION_INSTANTIATE = "ipd:stage-action:instantiate";

    /** 认证模板查询 */
    public static final String CERT_TEMPLATE_LIST = "ipd:cert-template:list";
    /** 新建认证模板 */
    public static final String CERT_TEMPLATE_ADD = "ipd:cert-template:add";
    /** 移除认证模板 */
    public static final String CERT_TEMPLATE_REMOVE = "ipd:cert-template:remove";

    /** Gate 要素列表 */
    public static final String GATE_ELEMENT_LIST = "ipd:gate-element:list";
    /** 新建 Gate 要素 */
    public static final String GATE_ELEMENT_ADD = "ipd:gate-element:add";
    /** 更新 Gate 要素 */
    public static final String GATE_ELEMENT_EDIT = "ipd:gate-element:edit";
    /** 停用 Gate 要素 */
    public static final String GATE_ELEMENT_DISABLE = "ipd:gate-element:disable";

    /** 归档区列表 */
    public static final String DELETION_ARCHIVE = "ipd:deletion:archive";
    /** 超管二次确认清除 */
    public static final String DELETION_PURGE = "ipd:deletion:purge";

    /** KPI 考核查询（P3-1.1/1.2/1.3 通用权限码，MARKET_PM/RD_PM/GROUP_LEADER/SUPER_ADMIN 可见） */
    public static final String KPI_QUERY = "ipd:kpi:query";

    /** P3-8.2 负反馈查询（MARKET_PM/RD_PM/GROUP_LEADER/SUPER_ADMIN 可见；细粒度由 service 二次校验） */
    public static final String NEGATIVE_FEEDBACK_QUERY = "ipd:negative-feedback:query";
    /** P3-8.2 负反馈录入（DRAFT 创建；MARKET_PM/RD_PM/GROUP_LEADER/SUPER_ADMIN 均可） */
    public static final String NEGATIVE_FEEDBACK_CREATE = "ipd:negative-feedback:create";
    /** P3-8.2 负反馈认定/解除（仅 GROUP_LEADER / SUPER_ADMIN；走 requireLeaderOrAdmin） */
    public static final String NEGATIVE_FEEDBACK_DECIDE = "ipd:negative-feedback:decide";

    private static final Set<String> INTERNAL_ROLES = Set.of("MARKET_PM", "RD_PM", "GROUP_LEADER", "SUPER_ADMIN");
    private final IpdAuthSession session;
    private final IpdAuthService authService;

    public IpdActor requireInternal() {
        Person person;
        try {
            person = session.currentPerson();
        } catch (NotLoginException e) {
            throw new IpdPermissionException(401, ApiV1ErrorCode.UNAUTHORIZED);
        }
        IpdAuthService.Scope scope = authService.scopeOf(person);
        if (scope == IpdAuthService.Scope.NONE || person.getId() == null) {
            throw new IpdPermissionException(401, ApiV1ErrorCode.UNAUTHORIZED);
        }
        if (scope != IpdAuthService.Scope.FULL) {
            throw new IpdPermissionException(403,
                scope == IpdAuthService.Scope.HANDOVER_ONLY ? ApiV1ErrorCode.ACCOUNT_FROZEN_PENDING_HANDOVER
                : scope == IpdAuthService.Scope.PASSWORD_CHANGE_REQUIRED ? ApiV1ErrorCode.ACCOUNT_PASSWORD_CHANGE_REQUIRED
                : ApiV1ErrorCode.FORBIDDEN);
        }
        if (!INTERNAL_ROLES.contains(person.getPersonType())) throw denied();
        return new IpdActor(person.getId(), person.getName(), person.getPersonType(), person.getGroupId());
    }

    public IpdActor requireAdmin() {
        return requireRoles("SUPER_ADMIN");
    }

    /** 页08：研发PM不能以修改请求角色的方式成为市场侧项目发起人。 */
    public IpdActor requireProjectCreator() {
        return requireRoles("MARKET_PM", "GROUP_LEADER", "SUPER_ADMIN");
    }

    /** BR-PROD-01：超管导入在售型号；内部普通新增不接受游客占位来源。 */
    public IpdActor requireProductCreator(String source) {
        IpdActor actor = requireInternal();
        if (Product.SRC_ADMIN_IMPORT.equals(source) && !"SUPER_ADMIN".equals(actor.role())) throw denied();
        if (Product.SRC_GUEST_OTHER.equals(source)) throw denied();
        return actor;
    }

    /** 在售型号由超管维护；本人/组范围还须由SEC-02叠加。 */
    public IpdActor requireProductWriter(Supplier<Product> productLoader) {
        IpdActor actor = requireInternal();
        Product product = productLoader.get();
        if (product == null || Product.SRC_GUEST_OTHER.equals(product.getSource())) throw denied();
        if (Product.SRC_ADMIN_IMPORT.equals(product.getSource()) && !"SUPER_ADMIN".equals(actor.role())) throw denied();
        return actor;
    }

    /** BR-USER-08：固定专业不可跨；组长只有GROUP_LEADER动作，不隐式代理双侧PM。 */
    public IpdActor requireActionWriter(Supplier<StageAction> actionLoader) {
        IpdActor actor = requireInternal();
        StageAction action = actionLoader.get();
        if (action == null) throw denied();
        if ("SUPER_ADMIN".equals(actor.role())) return actor;
        String owner = action.getOwnerRole();
        if (actor.role().equals(owner)) return actor;
        if ("BOTH".equals(owner) && Set.of("MARKET_PM", "RD_PM").contains(actor.role())) return actor;
        throw new IpdPermissionException(409, ApiV1ErrorCode.ROLE_LOCKED);
    }

    /** 禁止Entity请求中的审计身份透传；完整业务字段白名单另由API-02收口。 */
    public void bindCreateAudit(BaseEntity entity, IpdActor actor) {
        entity.setCreateBy(actor.id());
        entity.setUpdateBy(actor.id());
        entity.setCreateDept(null);
        entity.setCreateTime(null);
        entity.setUpdateTime(null);
    }

    /** SEC-02：当前操作人是否被允许访问 actorGroupId 所属组的对象。SUPER_ADMIN 一律通过；本组/空组/跨组的判定规则由该方法唯一承担。 */
    public boolean canAccessGroup(Long actorGroupId, Long objectGroupId) {
        if (actorGroupId == null) return false;
        return actorGroupId.equals(objectGroupId);
    }

    /** SEC-02：super_admin 例外放行；其他角色按 actorGroupId == objectGroupId 判定。null objectGroupId 一律拒绝（无归属）。 */
    public boolean canReadProject(String role, Long actorGroupId, Long projectProductGroupId) {
        if ("SUPER_ADMIN".equals(role)) return true;
        return canAccessGroup(actorGroupId, projectProductGroupId);
    }

    private IpdActor requireRoles(String... roles) {
        IpdActor actor = requireInternal();
        if (!Set.of(roles).contains(actor.role())) throw denied();
        return actor;
    }

    /**
     * 组长或超管（删除初审等）。
     *
     * @return 当前操作人
     */
    public IpdActor requireLeaderOrAdmin() {
        return requireRoles("GROUP_LEADER", "SUPER_ADMIN");
    }

    private static IpdPermissionException denied() {
        return new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN);
    }
}
