package org.ruoyi.ipd.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.controller.AuditLogController;
import org.ruoyi.ipd.controller.SystemConfigController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-02 缺陷 A-audit 契约锁：IpdRolePermissionCatalog 与 AuditLog/SystemConfig 控制器注解字面量对齐。
 * <p>
 * 缺陷史（2026-09-05）：AuditLogController 三端点 @SaCheckPermission 要求 ipd:audit-log:*，
 * Catalog 未登记 → 全员（含超管）NotPermission，AC-AUD-02 不可能通过。
 * 本测试通过反射比对控制器注解字面量与 Catalog 授予集，防止后续端点增改再脱节。
 * <p>
 * 注意：/scope、/export/scope 为无注解端点（requireInternal + service 层角色范围过滤，P0-5.4），
 * 不在注解矩阵内，属预期设计而非缺口。
 */
@Tag("dev")
@DisplayName("SEC02 缺陷A-audit：Catalog 审计/系统参数权限码契约")
class Sec02AuditCatalogAcceptanceTest {

    private static final List<String> INTERNAL_ROLES =
        List.of("SUPER_ADMIN", "GROUP_LEADER", "MARKET_PM", "RD_PM");

    @Test
    @DisplayName("AuditLogController 每个 @SaCheckPermission 字面量均被 SUPER_ADMIN 持有")
    void auditLogAnnotationsGrantedToSuperAdmin() {
        List<String> required = annotatedPermissions(AuditLogController.class);
        assertThat(required)
            .as("AuditLogController 应存在注解权限端点（list/verify/export）")
            .isNotEmpty()
            .contains("ipd:audit-log:list", "ipd:audit-log:verify", "ipd:audit-log:export");
        for (String code : required) {
            assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", code))
                .as("SUPER_ADMIN 应持有 %s", code)
                .isTrue();
        }
    }

    @Test
    @DisplayName("SystemConfigController 每个 @SaCheckPermission 字面量均被 SUPER_ADMIN 持有")
    void systemConfigAnnotationsGrantedToSuperAdmin() {
        List<String> required = annotatedPermissions(SystemConfigController.class);
        assertThat(required)
            .contains("ipd:system-config:list", "ipd:system-config:read", "ipd:system-config:update");
        for (String code : required) {
            assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", code))
                .as("SUPER_ADMIN 应持有 %s", code)
                .isTrue();
        }
    }

    @Test
    @DisplayName("审计三码超管专属：组长/市场PM/研发PM 均不持有")
    void auditCodesAreAdminOnly() {
        List<String> adminOnly = List.of("ipd:audit-log:list", "ipd:audit-log:verify", "ipd:audit-log:export");
        for (String role : INTERNAL_ROLES) {
            if ("SUPER_ADMIN".equals(role)) {
                continue;
            }
            for (String code : adminOnly) {
                assertThat(IpdRolePermissionCatalog.has(role, code))
                    .as("%s 不应持有 %s（旧三端点超管专属，范围查询走 /scope）", role, code)
                    .isFalse();
            }
        }
    }

    @Test
    @DisplayName("system-config：read 全体内部角色可读，list/update 超管专属")
    void systemConfigSplit() {
        for (String role : INTERNAL_ROLES) {
            assertThat(IpdRolePermissionCatalog.has(role, "ipd:system-config:read"))
                .as("%s 应可读单参数（业务链路依赖，如 Gate 周期天数）", role)
                .isTrue();
        }
        for (String role : INTERNAL_ROLES) {
            if ("SUPER_ADMIN".equals(role)) {
                continue;
            }
            assertThat(IpdRolePermissionCatalog.has(role, "ipd:system-config:list")).isFalse();
            assertThat(IpdRolePermissionCatalog.has(role, "ipd:system-config:update")).isFalse();
        }
    }

    @Test
    @DisplayName("未知角色/空角色返回空权限集（拒绝一切注解权限）")
    void unknownRoleDenied() {
        assertThat(IpdRolePermissionCatalog.permissionsOf("EXTERNAL_AUDITOR")).isEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf(null)).isEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf(" ")).isEmpty();
        assertThat(IpdRolePermissionCatalog.has("EXTERNAL_AUDITOR", "ipd:audit-log:list")).isFalse();
    }

    /** 反射提取控制器所有方法上的 @SaCheckPermission value 字面量。 */
    private static List<String> annotatedPermissions(Class<?> controller) {
        List<String> codes = new ArrayList<>();
        for (Method m : controller.getDeclaredMethods()) {
            SaCheckPermission ann = m.getAnnotation(SaCheckPermission.class);
            if (ann != null) {
                codes.addAll(List.of(ann.value()));
            }
        }
        return codes;
    }
}
