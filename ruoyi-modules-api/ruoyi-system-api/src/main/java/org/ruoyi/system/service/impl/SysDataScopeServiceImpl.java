package org.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.helper.DataBaseHelper;
import org.ruoyi.system.domain.SysDept;
import org.ruoyi.system.domain.SysRoleDept;
import org.ruoyi.system.mapper.SysDeptMapper;
import org.ruoyi.system.mapper.SysRoleDeptMapper;
import org.ruoyi.system.service.ISysDataScopeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据权限 实现
 * <p>
 * 注意: 此Service内不允许调用标注`数据权限`注解的方法
 * 例如: deptMapper.selectList 此 selectList 方法标注了`数据权限`注解 会出现循环解析的问题
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service("sdss")
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public String getRoleCustom(Long roleId) {
        List<SysRoleDept> list = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>()
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId));
        if (CollUtil.isNotEmpty(list)) {
            return StreamUtils.join(list, rd -> Convert.toStr(rd.getDeptId()));
        }
        // 问题描述: 当前用户【如果没有绑定任何部门的情况下】在查询用户列表分页的时候 调用 SysUserMapper 的 selectPageUserList() 时候报错：sql错误
        // 通过打印sql发现 select count(*) as total from sysUser u left join sys_dept d on u.dept_id where (u.del_flag = ?) and (d.dept_id in ())
        // 在 (d.dept_id in ())  这个位置报错了, 这个在sql中执行也是不行的
        // 我发现 在 PlusPostInitTableInfoHandler 中的
        //                 String sql = DataPermissionHelper.ignore(() ->
        //                    parser.parseExpression(type.getSqlTemplate(), parserContext).getValue(context, String.class)
        //                );
        // 把这个方法交给框架去处理了, 所以没办法进行判空
        // 于是我找到了这里，想着给这里默认赋值一个 -1
        // 下面的那个 getDeptAndChild 暂时没发现有问题,如果发现问题可以考虑参考这种方法
        return "-1";
    }

    @Override
    public String getDeptAndChild(Long deptId) {
        List<SysDept> deptList = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .apply(DataBaseHelper.findInSet(deptId, "ancestors")));
        List<Long> ids = StreamUtils.toList(deptList, SysDept::getDeptId);
        ids.add(deptId);
        List<SysDept> list = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
            .select(SysDept::getDeptId)
            .in(SysDept::getDeptId, ids));
        if (CollUtil.isNotEmpty(list)) {
            return StreamUtils.join(list, d -> Convert.toStr(d.getDeptId()));
        }
        return null;
    }

}
