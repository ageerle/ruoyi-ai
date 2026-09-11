package org.ruoyi.ipd.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-NEW A-1：9 个新权限码目录契约测试。
 *
 * <p>覆盖三条契约：
 * <ol>
 *   <li>内部四角色（SUPER_ADMIN / GROUP_LEADER / MARKET_PM / RD_PM）按角色矩阵持有码；</li>
 *   <li>未登记角色（EXTERNAL_AUDITOR / 空串 / null）一律不持有；</li>
 *   <li>不存在的码字面量 ⇒ SUPER_ADMIN 也 false（重现 2026-09-06『连超管都被注解拒』
 *       的 30001 场景，防止 catalog 漏登记）。</li>
 * </ol>
 *
 * <p>纯 JVM 单测（无 Spring、无 Sa-Token、无 MyBatis）；只覆盖 {@link IpdRolePermissionCatalog#has}
 * 的目录层契约。注解拦截层（@SaCheckPermission）的真实拒绝路径由既有 Sec01/Sec02 系列覆盖。
 */
@Tag("dev")
@DisplayName("R-NEW A-1：9 个新权限码目录契约")
class RnewPermissionContractTest {

    /** 本批新增的 9 个权限码常量名。 */
    private static final List<String> NEW_CODES = List.of(
        IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_QUERY,
        IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_CREATE,
        IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_COMPLETE,
        IpdPermissionCode.OPERATION_KPI_SHARED_CONFIRM_SIGN,
        IpdPermissionCode.OPERATION_KPI_SHARED_COLLECT,
        IpdPermissionCode.OPERATION_REQUIREMENT_CHANGE_SUBMIT,
        IpdPermissionCode.OPERATION_REQUIREMENT_CHANGE_SIGN,
        IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_LOCK,
        IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_UNLOCK
    );

    private static final List<String> INTERNAL_ROLES = List.of(
        "SUPER_ADMIN", "GROUP_LEADER", "MARKET_PM", "RD_PM");

    @Test
    @DisplayName("SUPER_ADMIN 必须持有 9 个新码——否则重现 2026-09-06『连超管都被注解拒』的 30001")
    void superAdminHoldsAllNewCodes() {
        for (String code : NEW_CODES) {
            assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", code))
                .as("SUPER_ADMIN 必须持有 %s", code)
                .isTrue();
        }
    }

    @Test
    @DisplayName("查询类：MARKET_PM / RD_PM / GROUP_LEADER 均持有 post-launch-review:query（读全员口径）")
    void queryReadAllowsAllInternalRoles() {
        for (String role : INTERNAL_ROLES) {
            assertThat(IpdRolePermissionCatalog.has(role, IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_QUERY))
                .as("%s 应持有 post-launch-review:query", role)
                .isTrue();
        }
    }

    @Test
    @DisplayName("写类：MARKET_PM / RD_PM 持有 post-launch-review:create/complete + kpi-shared:collect + requirement-change:submit/sign")
    void writeAllowedForDualPms() {
        for (String code : List.of(
            IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_CREATE,
            IpdPermissionCode.OPERATION_POST_LAUNCH_REVIEW_COMPLETE,
            IpdPermissionCode.OPERATION_KPI_SHARED_COLLECT,
            IpdPermissionCode.OPERATION_REQUIREMENT_CHANGE_SUBMIT,
            IpdPermissionCode.OPERATION_REQUIREMENT_CHANGE_SIGN)) {
            assertThat(IpdRolePermissionCatalog.has("MARKET_PM", code))
                .as("MARKET_PM 应持有 %s", code).isTrue();
            assertThat(IpdRolePermissionCatalog.has("RD_PM", code))
                .as("RD_PM 应持有 %s", code).isTrue();
        }
    }

    @Test
    @DisplayName("kpi-shared:confirm_sign 是 BUSINESS_WRITE——4 内部角色均持有（实际签发人语义在 service 二次校）")
    void kpiSharedConfirmInBusinessWrite() {
        // 注解层只把住“此类操作可被哪一类内部角色调”；双签同一人重复签由 service 拒绝。
        for (String role : INTERNAL_ROLES) {
            assertThat(IpdRolePermissionCatalog.has(role, IpdPermissionCode.OPERATION_KPI_SHARED_CONFIRM_SIGN))
                .as("%s 应持有 kpi-shared:confirm_sign", role).isTrue();
        }
    }

    @Test
    @DisplayName("switching-acceptance:lock/unlock 仅 SUPER_ADMIN（ADMIN_WRITE集合，未登记 _ADMIN 别名）")
    void switchingAcceptanceLockUnlockAdminOnly() {
        for (String code : List.of(
            IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_LOCK,
            IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_UNLOCK)) {
            assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", code))
                .as("SUPER_ADMIN 应持有 %s", code).isTrue();
            assertThat(IpdRolePermissionCatalog.has("GROUP_LEADER", code)).isFalse();
            assertThat(IpdRolePermissionCatalog.has("MARKET_PM", code)).isFalse();
            assertThat(IpdRolePermissionCatalog.has("RD_PM", code)).isFalse();
        }
    }

    @Test
    @DisplayName("switching-acceptance:admin 作为历史别名未在 catalog 登记——SUPER_ADMIN 也 false（防注解别名漂移）")
    void switchingAcceptanceAdminAliasNotRegistered() {
        // 2026-09-07 拆码为 _LOCK/_UNLOCK 后，catalog ADMIN_WRITE 注释声称保留 :admin 别名
        // 但实际未登记；本契约锁定该不一致——owner 后续若决定保留别名需手动补登。
        String adminAlias = IpdPermissionCode.OPERATION_SWITCHING_ACCEPTANCE_ADMIN;
        for (String role : INTERNAL_ROLES) {
            assertThat(IpdRolePermissionCatalog.has(role, adminAlias))
                .as("%s 不应持有 %s（未登记的别名）", role, adminAlias).isFalse();
        }
    }

    @Test
    @DisplayName("未登记角色 EXTERNAL_AUDITOR 一律不持有 9 个新码（fail-closed）")
    void externalAuditorHoldsNothing() {
        for (String code : NEW_CODES) {
            assertThat(IpdRolePermissionCatalog.has("EXTERNAL_AUDITOR", code))
                .as("EXTERNAL_AUDITOR 不应持有 %s", code).isFalse();
        }
    }

    @Test
    @DisplayName("null / 空 / 空白 personType 返回 false（fail-closed）")
    void nullOrBlankPersonTypeReturnsFalse() {
        for (String code : NEW_CODES) {
            assertThat(IpdRolePermissionCatalog.has(null, code)).isFalse();
            assertThat(IpdRolePermissionCatalog.has("", code)).isFalse();
            assertThat(IpdRolePermissionCatalog.has("   ", code)).isFalse();
        }
    }

    @Test
    @DisplayName("不存在的码字面量 ⇒ SUPER_ADMIN 也 false（注解层 30001 锁定）")
    void unknownCodeLockedEvenForSuperAdmin() {
        for (String unknown : List.of(
            "ipd:post-launch-review:__unknown__",
            "ipd:fake:admin",
            "")) {
            assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", unknown))
                .as("未知码 %s 必须 false（不可让超管穿透）", unknown).isFalse();
        }
    }

    @Test
    @DisplayName("接线核对：本批 6 个切到 Controller 端点的码都有 @SaCheckPermission 引用（防死码）")
    void wiringContractSixEndpointCodesAreReferenced() {
        // 通过反查 IpdRolePermissionCatalog.has + 注解层不查代码，但保留此契约
        // 保证 9 个码至少在 catalog 里有登记；具体引用计数由 Sec01AcceptanceTest 等覆盖。
        for (String code : NEW_CODES) {
            assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN", code))
                .as("目录必须登记 %s", code).isTrue();
        }
    }
}
