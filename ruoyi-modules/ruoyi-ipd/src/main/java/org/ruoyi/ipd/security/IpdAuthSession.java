package org.ruoyi.ipd.security;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.crypto.SecureUtil;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.springframework.stereotype.Component;

/** Person使用独立Sa-Token登录类型，绝不把Person.id写进基线sys_user会话。 */
@Component
public class IpdAuthSession {
    public static final String LOGIN_TYPE = "ipd";
    private final StpLogic logic = new StpLogicJwtForSimple(LOGIN_TYPE);
    private final PersonMapper personMapper;

    public IpdAuthSession(PersonMapper personMapper) {
        this.personMapper = personMapper;
    }

    public String login(Person person) {
        logic.login(person.getId(), new SaLoginParameter().setDeviceType("pc"));
        logic.getTokenSession().set("ipdPersonId", person.getId());
        // Bind to the credential snapshot that passed authentication, not a later database reload.
        logic.getTokenSession().set("ipdCredentialMarker", SecureUtil.sha256(person.getPasswordHash()));
        return logic.getTokenValue();
    }

    public Person currentPerson() {
        logic.checkLogin();
        Person person = personMapper.selectById(logic.getLoginIdAsLong());
        if (person == null || person.getPasswordHash() == null ||
            !SecureUtil.sha256(person.getPasswordHash()).equals(logic.getTokenSession().get("ipdCredentialMarker"))) {
            throw NotLoginException.newInstance(LOGIN_TYPE, NotLoginException.INVALID_TOKEN,
                "凭证已更新，请重新登录", logic.getTokenValue());
        }
        return person;
    }

    public void logout() {
        logic.logout();
    }

    public void revokeAll(Long personId) {
        logic.logout(personId);
    }

    /** P0-7.3：当前请求的 JWT 字符串；用于 refresh 期间做对比。 */
    public String tokenValue() {
        return logic.getTokenValue();
    }

    public long timeout() {
        return logic.getTokenTimeout();
    }

    /**
     * 暴露本会话绑定的 StpLogic（loginType=ipd），供注解鉴权与单测校验。
     *
     * @return ipd StpLogic 实例
     */
    public StpLogic stpLogic() {
        return logic;
    }
}
