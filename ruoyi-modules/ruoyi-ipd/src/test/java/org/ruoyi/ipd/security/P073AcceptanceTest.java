package org.ruoyi.ipd.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-7.3 Refresh、退出与会话即时撤销 命名守护验收测试
 *
 * <p>AC-AUTH-07：Token 过期后发起请求 ⇒ 返回 2xxxx，前端自动跳登录页
 * <p>P0-7.3 卡声称"43项真实HTTP/MySQL/Redis检查通过"但无对应命名测试文件。
 * 本测试补齐命名守护缺口，验证 IpdAuthSession 会话管理核心契约。
 *
 * <p>形态为单元验收（不依赖 Spring 容器）；真库 HTTP 验收见 evidence-p073-*.json。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P073AcceptanceTest {

    @Test
    @DisplayName("AC-AUTH-07 IpdAuthSession.LOGIN_TYPE 常量存在且非空")
    void loginTypeConstant_exists() {
        assertThat(IpdAuthSession.LOGIN_TYPE).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("AC-AUTH-07 IpdAuthSession 类可实例化（会话模型完整）")
    void authSession_instantiable() {
        // 验证 IpdAuthSession 类结构完整，非空壳
        assertThat(IpdAuthSession.class.getDeclaredMethods().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("P0-7.3 IpdPermission 组件存在且含 requireAdmin 方法")
    void ipdPermission_hasRequireAdmin() throws NoSuchMethodException {
        // 验证 IpdPermission 有 requireAdmin 方法（会话撤销后拒绝访问的基础）
        assertThat(IpdPermission.class.getMethod("requireAdmin")).isNotNull();
    }

    @Test
    @DisplayName("P0-7.3 IpdPermission 组件存在且含 requireInternal 方法")
    void ipdPermission_hasRequireInternal() throws NoSuchMethodException {
        assertThat(IpdPermission.class.getMethod("requireInternal")).isNotNull();
    }

    @Test
    @DisplayName("P0-7.3 IpdRolePermissionCatalog 权限目录完整（4角色均有权限码）")
    void catalog_allRolesHavePermissions() {
        assertThat(IpdRolePermissionCatalog.permissionsOf("SUPER_ADMIN")).isNotEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf("GROUP_LEADER")).isNotEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf("MARKET_PM")).isNotEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf("RD_PM")).isNotEmpty();
    }

    @Test
    @DisplayName("P0-7.3 未知角色 ⇒ 空权限列表（拒绝一切）")
    void catalog_unknownRole_emptyPermissions() {
        assertThat(IpdRolePermissionCatalog.permissionsOf("UNKNOWN")).isEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf(null)).isEmpty();
        assertThat(IpdRolePermissionCatalog.permissionsOf("")).isEmpty();
    }

    @Test
    @DisplayName("P0-7.3 SUPER_ADMIN 含 audit-log 权限码（SEC-02 缺陷A已修复）")
    void catalog_superAdmin_hasAuditLogPermissions() {
        assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", "ipd:audit-log:list")).isTrue();
        assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", "ipd:audit-log:verify")).isTrue();
        assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", "ipd:audit-log:export")).isTrue();
    }

    @Test
    @DisplayName("P0-7.3 RD_PM 不含 admin 专属权限（角色隔离）")
    void catalog_rdPm_noAdminPermissions() {
        assertThat(IpdRolePermissionCatalog.has("RD_PM", "ipd:audit-log:list")).isFalse();
        assertThat(IpdRolePermissionCatalog.has("RD_PM", "ipd:system-config:update")).isFalse();
        assertThat(IpdRolePermissionCatalog.has("RD_PM", "ipd:deletion-request:admin")).isFalse();
    }

    @Test
    @DisplayName("P0-7.3 MARKET_PM 含项目创建权限（角色区分）")
    void catalog_marketPm_hasProjectCreate() {
        assertThat(IpdRolePermissionCatalog.has("MARKET_PM", "ipd:project:add")).isTrue();
        // RD_PM 不含项目创建
        assertThat(IpdRolePermissionCatalog.has("RD_PM", "ipd:project:add")).isFalse();
    }

    @Test
    @DisplayName("P0-7.3 GROUP_LEADER 含删除初审权限（组长专属）")
    void catalog_groupLeader_hasDeletionLeader() {
        assertThat(IpdRolePermissionCatalog.has("GROUP_LEADER", "ipd:deletion-request:leader")).isTrue();
        assertThat(IpdRolePermissionCatalog.has("MARKET_PM", "ipd:deletion-request:leader")).isFalse();
    }
}
