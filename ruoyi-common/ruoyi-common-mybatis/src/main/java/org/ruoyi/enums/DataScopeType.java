package org.ruoyi.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.helper.DataPermissionHelper;

/**
 * 数据权限类型枚举
 * <p>
 * 支持使用 SpEL 模板表达式定义 SQL 查询条件
 * 内置数据：
 * - {@code user}: 当前登录用户信息，参考 {@link LoginUser}
 * 内置服务：
 * - {@code sdss}: 系统数据权限服务，参考 ISysDataScopeService
 * 如需扩展数据，可以通过 {@link DataPermissionHelper} 进行操作
 * 如需扩展服务，可以通过 ISysDataScopeService 自行编写
 * </p>
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
    CUSTOM("2", " #{#deptName} IN ( #{@sdss.getRoleCustom( #user.roleId )} ) ", " 1 = 0 "),

    /**
     * 部门数据权限
     */
    DEPT("3", " #{#deptName} = #{#user.deptId} ", " 1 = 0 "),

    /**
     * 部门及以下数据权限
     */
    DEPT_AND_CHILD("4", " #{#deptName} IN ( #{@sdss.getDeptAndChild( #user.deptId )} )", " 1 = 0 "),

    /**
     * 仅本人数据权限
     */
    SELF("5", " #{#userName} = #{#user.userId} ", " 1 = 0 "),

    /**
     * 部门及以下或本人数据权限
     */
    DEPT_AND_CHILD_OR_SELF("6", " #{#deptName} IN ( #{@sdss.getDeptAndChild( #user.deptId )} ) OR #{#userName} = #{#user.userId} ", " 1 = 0 ");

    private final String code;

    /**
     * SpEL 模板表达式，用于构建 SQL 查询条件
     */
    private final String sqlTemplate;

    /**
     * 如果不满足 {@code sqlTemplate} 的条件，则使用此默认 SQL 表达式
     */
    private final String elseSql;

    /**
     * 根据枚举代码查找对应的枚举值
     *
     * @param code 枚举代码
     * @return 对应的枚举值，如果未找到则返回 null
     */
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
