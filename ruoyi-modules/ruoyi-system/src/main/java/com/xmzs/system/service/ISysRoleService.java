package com.xmzs.system.service;

import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.system.domain.SysUserRole;
import com.xmzs.system.domain.bo.SysRoleBo;
import com.xmzs.system.domain.vo.SysRoleVo;

import java.util.List;
import java.util.Set;

/**
 * 角色业务层
 *
 * @author Lion Li
 */
public interface ISysRoleService {


    TableDataInfo<SysRoleVo> selectPageRoleList(SysRoleBo role, PageQuery pageQuery);

    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    List<SysRoleVo> selectRoleList(SysRoleBo role);

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRoleVo> selectRolesByUserId(Long userId);

    /**
     * 根据用户ID查询角色权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    Set<String> selectRolePermissionByUserId(Long userId);

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    List<SysRoleVo> selectRoleAll();

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    List<Long> selectRoleListByUserId(Long userId);

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    SysRoleVo selectRoleById(Long roleId);

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    boolean checkRoleNameUnique(SysRoleBo role);

    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    boolean checkRoleKeyUnique(SysRoleBo role);

    /**
     * 校验角色是否允许操作
     *
     * @param roleId 角色ID
     */
    void checkRoleAllowed(Long roleId);

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    void checkRoleDataScope(Long roleId);

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    long countUserRoleByRoleId(Long roleId);

    /**
     * 新增保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    int insertRole(SysRoleBo bo);

    /**
     * 修改保存角色信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    int updateRole(SysRoleBo bo);

    /**
     * 修改角色状态
     *
     * @param roleId 角色ID
     * @param status 角色状态
     * @return 结果
     */
    int updateRoleStatus(Long roleId, String status);

    /**
     * 修改数据权限信息
     *
     * @param bo 角色信息
     * @return 结果
     */
    int authDataScope(SysRoleBo bo);

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    int deleteRoleById(Long roleId);

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    int deleteRoleByIds(Long[] roleIds);

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    int deleteAuthUser(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    int deleteAuthUsers(Long roleId, Long[] userIds);

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    int insertAuthUsers(Long roleId, Long[] userIds);

    void cleanOnlineUserByRole(Long roleId);
}
