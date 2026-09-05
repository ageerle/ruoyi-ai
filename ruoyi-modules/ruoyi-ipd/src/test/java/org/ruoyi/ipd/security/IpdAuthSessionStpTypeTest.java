package org.ruoyi.ipd.security;

import cn.dev33.satoken.stp.StpLogic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-02 白盒验收：IpdAuthSession 的 StpLogic loginType 严格为 "ipd"，
 * 防止误用基线 "login" 类型导致与 sys_user 会话串扰。
 */
@Tag("dev")
class IpdAuthSessionStpTypeTest {

    @Test
    @DisplayName("LOGIN_TYPE 常量必须为 ipd（SEC-02 隔离基线 sys_user 会话）")
    void loginTypeConstant() {
        assertThat(IpdAuthSession.LOGIN_TYPE).isEqualTo("ipd");
        assertThat(IpdAuthSession.LOGIN_TYPE).isNotEqualTo("login");
    }

    @Test
    @DisplayName("StpLogic 实例 loginType 与 LOGIN_TYPE 一致")
    void stpLogicInstanceLoginType() throws Exception {
        // logic 是实例字段（private final StpLogic logic = new StpLogicJwtForSimple(LOGIN_TYPE)），
        // 需先创建 IpdAuthSession 实例。PersonMapper 传 null（仅用于反射取字段，不触发调用）
        IpdAuthSession session = new IpdAuthSession(null);
        java.lang.reflect.Field f = IpdAuthSession.class.getDeclaredField("logic");
        f.setAccessible(true);
        StpLogic logic = (StpLogic) f.get(session);
        assertThat(logic).isNotNull();
        assertThat(logic.getLoginType()).isEqualTo(IpdAuthSession.LOGIN_TYPE);
    }
}
