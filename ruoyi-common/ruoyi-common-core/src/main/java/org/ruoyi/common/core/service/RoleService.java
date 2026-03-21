package org.ruoyi.common.core.service;

import java.util.List;
import java.util.Map;

/**
 * 通用 角色服务
 *
 * @author AprilWind
 */
public interface RoleService {

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    Map<Long, String> selectRoleNamesByIds(List<Long> roleIds);

}
