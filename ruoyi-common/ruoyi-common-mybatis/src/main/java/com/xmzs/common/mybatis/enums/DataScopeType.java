package com.xmzs.common.mybatis.enums;

import com.xmzs.common.core.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.xmzs.common.mybatis.helper.DataPermissionHelper;

/**
 * 数据权限类型
 * <p>
 * 语法支持 spel 模板表达式
 * <p>
 * 内置数据 user 当前用户 内容参考 LoginUser
 * 如需扩展数据 可使用 {@link DataPermissionHelper} 操作
 * 内置服务 sdss 系统数据权限服务 内容参考 SysDataScopeService
 * 如需扩展更多自定义服务 可以参考 sdss 自行编写
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {

    /**
     * 全部数据权限
     */
    ALL("1", "", ""),

    /**
     * 自定数据权限
     */
    CUSTOM("2", " #{#deptName} IN ( #{@sdss.getRoleCustom( #user.roleId )} ) ", ""),

    /**
     * 部门数据权限
     */
    DEPT("3", " #{#deptName} = #{#user.deptId} ", ""),

    /**
     * 部门及以下数据权限
     */
    DEPT_AND_CHILD("4", " #{#deptName} IN ( #{@sdss.getDeptAndChild( #user.deptId )} )", ""),

    /**
     * 仅本人数据权限
     */
    SELF("5", " #{#userName} = #{#user.userId} ", " 1 = 0 ");

    private final String code;

    /**
     * 语法 采用 spel 模板表达式
     */
    private final String sqlTemplate;

    /**
     * 不满足 sqlTemplate 则填充
     */
    private final String elseSql;

    public static DataScopeType findCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (DataScopeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
