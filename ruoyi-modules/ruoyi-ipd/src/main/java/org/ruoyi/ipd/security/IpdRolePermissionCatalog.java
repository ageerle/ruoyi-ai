package org.ruoyi.ipd.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SEC-API-02：personType → Sa-Token 权限码目录。
 * <p>
 * 对齐 {@link IpdPermissionCode} 与 Controller 字面量；粒度与 {@link IpdPermission#require*} 一致：
 * 注解层做粗粒度门禁，对象级/专业锁定仍由 require* 叠加。
 */
public final class IpdRolePermissionCatalog {

    /** 全体内部角色可读的查询类权限。 */
    private static final Set<String> READ_SET = unique(
        IpdPermissionCode.OPERATION_MODULE_PROJECT,
        IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY,
        IpdPermissionCode.OPERATION_PRODUCT_GROUP,
        "ipd:product:query",
        IpdPermissionCode.OPERATION_STAGE_ACTION,
        IpdPermissionCode.OPERATION_CERT_TEMPLATE,
        IpdPermissionCode.OPERATION_GATE_ELEMENT,
        IpdPermissionCode.OPERATION_GATE_REVIEW,
        "ipd:system-config:read"
    );

    /** 内部角色可写的业务操作（不含超管专属配置/归档）。 */
    private static final Set<String> BUSINESS_WRITE = unique(
        IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE,
        IpdPermissionCode.OPERATION_PRODUCT_GROUP_CREATE,
        IpdPermissionCode.OPERATION_PRODUCT_GROUP_BIND_PROJECT,
        IpdPermissionCode.OPERATION_STAGE_ACTION_EXECUTE,
        IpdPermissionCode.OPERATION_STAGE_ACTION_DELIVERABLE,
        IpdPermissionCode.OPERATION_STAGE_ACTION_INSTANTIATE,
        IpdPermissionCode.OPERATION_GATE_REVIEW_INITIATE,
        IpdPermissionCode.OPERATION_GATE_REVIEW_APPROVE,
        IpdPermissionCode.OPERATION_DELETION_REQUEST_SUBMIT,
        IpdPermissionCode.OPERATION_COEFFICIENT_PROPOSE
    );

    /** 组长初审删除申请 + 系数定值确认。 */
    private static final Set<String> DELETION_LEADER = unique(
        IpdPermissionCode.OPERATION_DELETION_REQUEST_LEADER,
        IpdPermissionCode.OPERATION_COEFFICIENT_CONFIRM
    );

    /** 仅市场侧可建项（对齐 requireProjectCreator）。 */
    private static final Set<String> PROJECT_CREATE = unique(
        IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE
    );

    /** 仅 SUPER_ADMIN：Gate/证书模板写 + 系统参数 + 全量审计 + 归档 purge + 删除终审。 */
    private static final Set<String> ADMIN_WRITE = unique(
        IpdPermissionCode.OPERATION_GATE_ELEMENT_CREATE,
        IpdPermissionCode.OPERATION_GATE_ELEMENT_UPDATE,
        IpdPermissionCode.OPERATION_GATE_ELEMENT_DISABLE,
        IpdPermissionCode.OPERATION_CERT_TEMPLATE_CREATE,
        IpdPermissionCode.OPERATION_CERT_TEMPLATE_DELETE,
        IpdPermissionCode.OPERATION_DELETION_REQUEST_ARCHIVE,
        IpdPermissionCode.OPERATION_DELETION_REQUEST_PURGE,
        IpdPermissionCode.OPERATION_DELETION_REQUEST_ADMIN,
        "ipd:system-config:list",
        "ipd:system-config:update",
        // SEC-02 缺陷 A-audit：AuditLogController 旧三端点 @SaCheckPermission 要求下列码；
        // 未登记时全员（含超管）NotPermission→AC-AUD-02 不可能过。组长/成员走无注解的
        // /scope、/export/scope（requireInternal + service 层角色范围过滤，P0-5.4）。
        "ipd:audit-log:list",
        "ipd:audit-log:verify",
        "ipd:audit-log:export"
    );

    private static final Map<String, Set<String>> BY_ROLE = Map.of(
        "SUPER_ADMIN", merge(READ_SET, BUSINESS_WRITE, PROJECT_CREATE, DELETION_LEADER, ADMIN_WRITE),
        "GROUP_LEADER", merge(READ_SET, BUSINESS_WRITE, PROJECT_CREATE, DELETION_LEADER),
        "MARKET_PM", merge(READ_SET, BUSINESS_WRITE, PROJECT_CREATE),
        "RD_PM", merge(READ_SET, BUSINESS_WRITE)
    );

    private IpdRolePermissionCatalog() {
    }

    /**
     * 按人员类型解析权限码列表；未知类型返回空列表（拒绝一切注解权限）。
     *
     * @param personType Person.personType，如 MARKET_PM
     * @return 不可变权限码列表
     */
    public static List<String> permissionsOf(String personType) {
        if (personType == null || personType.isBlank()) {
            return List.of();
        }
        Set<String> set = BY_ROLE.get(personType);
        if (set == null) {
            return List.of();
        }
        return List.copyOf(set);
    }

    /**
     * 角色列表：直接回传 personType，便于 @SaCheckRole 扩展。
     *
     * @param personType 人员类型
     * @return 单元素角色列表或空
     */
    public static List<String> rolesOf(String personType) {
        if (personType == null || personType.isBlank() || !BY_ROLE.containsKey(personType)) {
            return List.of();
        }
        return List.of(personType);
    }

    /**
     * 判断角色是否具备指定权限码（单测与白盒用）。
     *
     * @param personType 人员类型
     * @param permission 权限码
     * @return 是否具备
     */
    public static boolean has(String personType, String permission) {
        return permissionsOf(personType).contains(permission);
    }

    @SafeVarargs
    private static Set<String> merge(Set<String>... parts) {
        Set<String> out = new LinkedHashSet<>();
        for (Set<String> part : parts) {
            out.addAll(part);
        }
        return Collections.unmodifiableSet(out);
    }

    /**
     * 构建去重权限集合（IpdPermissionCode 中多个常量可映射同一字面量）。
     *
     * @param codes 权限码
     * @return 不可变集合
     */
    private static Set<String> unique(String... codes) {
        Set<String> out = new LinkedHashSet<>();
        Collections.addAll(out, codes);
        return Collections.unmodifiableSet(out);
    }
}
