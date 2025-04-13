package org.ruoyi.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解，用于标记数据权限的占位符关键字和替换值
 * <p>
 * 一个注解只能对应一个模板
 * </p>
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataColumn {

    /**
     * 数据权限模板的占位符关键字，默认为 "deptName"
     *
     * @return 占位符关键字数组
     */
    String[] key() default "deptName";

    /**
     * 数据权限模板的占位符替换值，默认为 "dept_id"
     *
     * @return 占位符替换值数组
     */
    String[] value() default "dept_id";

    /**
     * 权限标识符 用于通过菜单权限标识符来获取数据权限
     * 拥有此标识符的角色 将不会拼接此角色的数据过滤sql
     *
     * @return 权限标识符
     */
    String permission() default "";
}
