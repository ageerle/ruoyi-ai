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
        "ipd:system-config:read",
        // OPS-05：站内通知收件箱（本人；receiver 从会话推导，读写同人）
        IpdPermissionCode.OPERATION_NOTIFICATION_READ,
        // P1-10.1：AI 文档版本链读取
        IpdPermissionCode.OPERATION_AI_DOCUMENT,
        // 2026-09-06 修全员 403：SOP/AI 模型读码在 Controller 注解里存在但目录未登记，
        // 连 SUPER_ADMIN 都被 @SaCheckPermission 拒（NotPermission→30001）
        IpdPermissionCode.OPERATION_SOP_TEMPLATE,
        IpdPermissionCode.OPERATION_AI_MODEL,
        // P3-1.1/1.2/1.3：KPI 考核（内部全员可见；对象级 actor 身份在 service 二次校验）
        "ipd:kpi:query",
        // P3-6.2：贡献度评定（双 PM 自评 + 各自产品组长可读；细粒度权限在 service 二次校验）
        "ipd:contribution:query",
        // P3-8.2：负反馈查询（全员可读）
        "ipd:negative-feedback:query"
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
        IpdPermissionCode.OPERATION_COEFFICIENT_PROPOSE,
        // P1-10.1：AI 文档登记原始输出 / 人工改版 / 人工审核（BR-AI-03）
        IpdPermissionCode.OPERATION_AI_DOCUMENT_CREATE,
        IpdPermissionCode.OPERATION_AI_DOCUMENT_REVISE,
        IpdPermissionCode.OPERATION_AI_DOCUMENT_REVIEW,
        // P3-6.2：贡献度评定保存（双 PM 自评）
        "ipd:contribution:save",
        // P3-8.2：负反馈录入（MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 均可）
        "ipd:negative-feedback:create"
    );

    /** 组长初审删除申请 + 系数定值确认。 */
    private static final Set<String> DELETION_LEADER = unique(
        IpdPermissionCode.OPERATION_DELETION_REQUEST_LEADER,
        IpdPermissionCode.OPERATION_COEFFICIENT_CONFIRM,
        // P3-6.2：产品组长确认贡献度（仅 GROUP_LEADER）
        "ipd:contribution:confirm",
        // P3-8.2：负反馈认定/解除（仅 GROUP_LEADER / SUPER_ADMIN）
        "ipd:negative-feedback:decide"
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
        "ipd:audit-log:export",
        // OPS-05：outbox 消费端手动触发（运维观察；正常轮询待 OPS-04 scheduler 合入）
        IpdPermissionCode.OPERATION_NOTIFICATION_DISPATCH,
        // 2026-09-06 补齐注解用码：SOP/AI 模型写与招募超管指派（方法内 requireAdmin 已兜）
        IpdPermissionCode.OPERATION_SOP_TEMPLATE_EDIT,
        IpdPermissionCode.OPERATION_AI_MODEL_EDIT,
        IpdPermissionCode.OPERATION_BID_INVITATION_ADMIN_ASSIGN
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
