package com.xmzs.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.excel.utils.ExcelUtil;
import com.xmzs.common.log.annotation.Log;
import com.xmzs.common.log.enums.BusinessType;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.common.web.core.BaseController;
import com.xmzs.system.domain.SysUserRole;
import com.xmzs.system.domain.bo.SysDeptBo;
import com.xmzs.system.domain.bo.SysRoleBo;
import com.xmzs.system.domain.bo.SysUserBo;
import com.xmzs.system.domain.vo.DeptTreeSelectVo;
import com.xmzs.system.domain.vo.SysRoleVo;
import com.xmzs.system.domain.vo.SysUserVo;
import com.xmzs.system.service.ISysDeptService;
import com.xmzs.system.service.ISysRoleService;
import com.xmzs.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色信息
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController {

    private final ISysRoleService roleService;
    private final ISysUserService userService;
    private final ISysDeptService deptService;

    /**
     * 获取角色信息列表
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public TableDataInfo<SysRoleVo> list(SysRoleBo role, PageQuery pageQuery) {
        return roleService.selectPageRoleList(role, pageQuery);
    }

    /**
     * 导出角色信息列表
     */
    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("system:role:export")
    @PostMapping("/export")
    public void export(SysRoleBo role, HttpServletResponse response) {
        List<SysRoleVo> list = roleService.selectRoleList(role);
        ExcelUtil.exportExcel(list, "角色数据", SysRoleVo.class, response);
    }

    /**
     * 根据角色编号获取详细信息
     *
     * @param roleId 角色ID
     */
    @SaCheckPermission("system:role:query")
    @GetMapping(value = "/{roleId}")
    public R<SysRoleVo> getInfo(@PathVariable Long roleId) {
        roleService.checkRoleDataScope(roleId);
        return R.ok(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     */
    @SaCheckPermission("system:role:add")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysRoleBo role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return R.fail("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        } else if (!roleService.checkRoleKeyUnique(role)) {
            return R.fail("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        return toAjax(roleService.insertRole(role));

    }

    /**
     * 修改保存角色
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysRoleBo role) {
        roleService.checkRoleAllowed(role.getRoleId());
        roleService.checkRoleDataScope(role.getRoleId());
        if (!roleService.checkRoleNameUnique(role)) {
            return R.fail("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        } else if (!roleService.checkRoleKeyUnique(role)) {
            return R.fail("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }

        if (roleService.updateRole(role) > 0) {
            roleService.cleanOnlineUserByRole(role.getRoleId());
            return R.ok();
        }
        return R.fail("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
    }

    /**
     * 修改保存数据权限
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/dataScope")
    public R<Void> dataScope(@RequestBody SysRoleBo role) {
        roleService.checkRoleAllowed(role.getRoleId());
        roleService.checkRoleDataScope(role.getRoleId());
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 状态修改
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysRoleBo role) {
        roleService.checkRoleAllowed(role.getRoleId());
        roleService.checkRoleDataScope(role.getRoleId());
        return toAjax(roleService.updateRoleStatus(role.getRoleId(), role.getStatus()));
    }

    /**
     * 删除角色
     *
     * @param roleIds 角色ID串
     */
    @SaCheckPermission("system:role:remove")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public R<Void> remove(@PathVariable Long[] roleIds) {
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    /**
     * 获取角色选择框列表
     */
    @SaCheckPermission("system:role:query")
    @GetMapping("/optionselect")
    public R<List<SysRoleVo>> optionselect() {
        return R.ok(roleService.selectRoleAll());
    }

    /**
     * 查询已分配用户角色列表
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/authUser/allocatedList")
    public TableDataInfo<SysUserVo> allocatedList(SysUserBo user, PageQuery pageQuery) {
        return userService.selectAllocatedList(user, pageQuery);
    }

    /**
     * 查询未分配用户角色列表
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/authUser/unallocatedList")
    public TableDataInfo<SysUserVo> unallocatedList(SysUserBo user, PageQuery pageQuery) {
        return userService.selectUnallocatedList(user, pageQuery);
    }

    /**
     * 取消授权用户
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancel")
    public R<Void> cancelAuthUser(@RequestBody SysUserRole userRole) {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    /**
     * 批量取消授权用户
     *
     * @param roleId  角色ID
     * @param userIds 用户ID串
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancelAll")
    public R<Void> cancelAuthUserAll(Long roleId, Long[] userIds) {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    /**
     * 批量选择用户授权
     *
     * @param roleId  角色ID
     * @param userIds 用户ID串
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/selectAll")
    public R<Void> selectAuthUserAll(Long roleId, Long[] userIds) {
        roleService.checkRoleDataScope(roleId);
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }

    /**
     * 获取对应角色部门树列表
     *
     * @param roleId 角色ID
     */
    @SaCheckPermission("system:role:list")
    @GetMapping(value = "/deptTree/{roleId}")
    public R<DeptTreeSelectVo> roleDeptTreeselect(@PathVariable("roleId") Long roleId) {
        DeptTreeSelectVo selectVo = new DeptTreeSelectVo();
        selectVo.setCheckedKeys(deptService.selectDeptListByRoleId(roleId));
        selectVo.setDepts(deptService.selectDeptTreeList(new SysDeptBo()));
        return R.ok(selectVo);
    }
}
