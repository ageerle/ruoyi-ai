package org.ruoyi.ipd.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.satoken.core.service.SaPermissionImpl;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SEC-API-02：StpInterface 桥接 + 角色权限矩阵 + type=ipd 校验路径。
 */
@Tag("dev")
class SecApi02StpInterfaceTest {

    /**
     * SUPER_ADMIN 具备归档 purge；RD_PM 不具备建项与超管写权限。
     */
    @Test
    @DisplayName("目录矩阵：SUPER_ADMIN 全量；RD_PM 无 project:add / deletion purge")
    void catalogMatrix() {
        assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN",
            IpdPermissionCode.OPERATION_DELETION_REQUEST_PURGE)).isTrue();
        assertThat(IpdRolePermissionCatalog.has("SUPER_ADMIN",
            IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE)).isTrue();

        assertThat(IpdRolePermissionCatalog.has("RD_PM",
            IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE)).isFalse();
        assertThat(IpdRolePermissionCatalog.has("RD_PM",
            IpdPermissionCode.OPERATION_DELETION_REQUEST_PURGE)).isFalse();
        assertThat(IpdRolePermissionCatalog.has("RD_PM",
            IpdPermissionCode.OPERATION_MODULE_PROJECT)).isTrue();

        assertThat(IpdRolePermissionCatalog.has("MARKET_PM",
            IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE)).isTrue();
        assertThat(IpdRolePermissionCatalog.has("GUEST",
            IpdPermissionCode.OPERATION_MODULE_PROJECT)).isFalse();
    }

    /**
     * 桥接：loginType=ipd 时按 Person 返回权限；其它类型委托基线（此处用空实现桩）。
     */
    @Test
    @DisplayName("桥接 getPermissionList：ipd 走目录，非 ipd 委托 baseline")
    void bridgeDelegatesByLoginType() {
        PersonMapper mapper = mock(PersonMapper.class);
        Person pm = new Person();
        pm.setId(42L);
        pm.setPersonType("MARKET_PM");
        when(mapper.selectById(42L)).thenReturn(pm);

        StpInterface baseline = mock(StpInterface.class);
        when(baseline.getPermissionList("1:2", "login")).thenReturn(java.util.List.of("system:user:list"));

        IpdStpInterfaceBridge bridge = new IpdStpInterfaceBridge(baseline, mapper);

        assertThat(bridge.getPermissionList(42L, IpdAuthSession.LOGIN_TYPE))
            .contains(IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE)
            .doesNotContain(IpdPermissionCode.OPERATION_DELETION_REQUEST_PURGE);

        assertThat(bridge.getRoleList(42L, IpdAuthSession.LOGIN_TYPE))
            .containsExactly("MARKET_PM");

        assertThat(bridge.getPermissionList("1:2", "login"))
            .containsExactly("system:user:list");
    }

    /**
     * 用真实 StpLogic(ipd) + 注入 StpInterface，验证 hasPermission(loginId, …) 正反例（无需完整登录态）。
     */
    @Test
    @DisplayName("StpLogic(ipd).hasPermission(loginId)：有权 true、建项/purge 对 RD_PM 为 false")
    void stpLogicHasPermissionUsesBridge() {
        PersonMapper mapper = mock(PersonMapper.class);
        Person rd = new Person();
        rd.setId(7L);
        rd.setPersonType("RD_PM");
        when(mapper.selectById(7L)).thenReturn(rd);

        IpdStpInterfaceBridge bridge = new IpdStpInterfaceBridge(new SaPermissionImpl(), mapper);
        cn.dev33.satoken.SaManager.setStpInterface(bridge);

        StpLogic logic = new StpLogic(IpdAuthSession.LOGIN_TYPE);

        assertThat(logic.hasPermission(7L, IpdPermissionCode.OPERATION_MODULE_PROJECT)).isTrue();
        assertThat(logic.hasPermission(7L, IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE)).isFalse();
        assertThat(logic.hasPermission(7L, IpdPermissionCode.OPERATION_DELETION_REQUEST_PURGE)).isFalse();
    }

    /**
     * 控制器注解必须带 type=ipd，否则会落到默认 login StpLogic。
     */
    @Test
    @DisplayName("契约：ProjectController @SaCheckPermission 全部声明 type=ipd")
    void projectControllerAnnotationsUseIpdType() throws Exception {
        Class<?> clazz = Class.forName("org.ruoyi.ipd.controller.ProjectController");
        for (var method : clazz.getDeclaredMethods()) {
            cn.dev33.satoken.annotation.SaCheckPermission ann =
                method.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class);
            if (ann == null) {
                continue;
            }
            assertThat(ann.type())
                .as("method %s must set type=ipd", method.getName())
                .isEqualTo(IpdAuthSession.LOGIN_TYPE);
        }
    }
}
