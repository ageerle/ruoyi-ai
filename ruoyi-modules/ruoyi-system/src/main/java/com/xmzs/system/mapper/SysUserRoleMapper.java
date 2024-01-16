package com.xmzs.system.mapper;

import com.xmzs.common.mybatis.core.mapper.BaseMapperPlus;
import com.xmzs.system.domain.SysUserRole;

import java.util.List;

/**
 * 用户与角色关联表 数据层
 *
 * @author Lion Li
 */
public interface SysUserRoleMapper extends BaseMapperPlus<SysUserRole, SysUserRole> {

    List<Long> selectUserIdsByRoleId(Long roleId);

}
