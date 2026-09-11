package org.ruoyi.ipd.security;

import cn.dev33.satoken.stp.StpInterface;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;

import java.util.Collections;
import java.util.List;

/**
 * SEC-API-02：StpInterface 桥接。
 * loginType=ipd 时按 Person.personType 返回权限/角色；其它 loginType 委托基线实现，避免串扰 sys_user。
 */
public class IpdStpInterfaceBridge implements StpInterface {

    private final StpInterface baseline;
    private final PersonMapper personMapper;

    /**
     * @param baseline     基线 SaPermissionImpl（login / sys_user）
     * @param personMapper IPD 人员表访问
     */
    public IpdStpInterfaceBridge(StpInterface baseline, PersonMapper personMapper) {
        this.baseline = baseline;
        this.personMapper = personMapper;
    }

    /**
     * 获取权限码列表。
     *
     * @param loginId   登录 id（ipd 下为 Person.id）
     * @param loginType 账号体系
     * @return 权限码；ipd 未知人员返回空列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (IpdAuthSession.LOGIN_TYPE.equals(loginType)) {
            return IpdRolePermissionCatalog.permissionsOf(resolvePersonType(loginId));
        }
        return baseline.getPermissionList(loginId, loginType);
    }

    /**
     * 获取角色列表。
     *
     * @param loginId   登录 id
     * @param loginType 账号体系
     * @return 角色码
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (IpdAuthSession.LOGIN_TYPE.equals(loginType)) {
            return IpdRolePermissionCatalog.rolesOf(resolvePersonType(loginId));
        }
        return baseline.getRoleList(loginId, loginType);
    }

    /**
     * 将 loginId 解析为 Person.personType；查无或非法 id 返回 null。
     *
     * @param loginId Sa-Token loginId
     * @return personType 或 null
     */
    private String resolvePersonType(Object loginId) {
        if (loginId == null) {
            return null;
        }
        long id;
        try {
            id = Long.parseLong(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            return null;
        }
        Person person = personMapper.selectById(id);
        return person == null ? null : person.getPersonType();
    }
}
