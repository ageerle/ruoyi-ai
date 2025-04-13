package org.ruoyi.annotation;

import java.lang.annotation.*;

/**
 * 数据权限组注解，用于标记数据权限配置数组
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据权限配置数组，用于指定数据权限的占位符关键字和替换值
     *
     * @return 数据权限配置数组
     */
    DataColumn[] value();

    /**
     * 权限拼接标识符(用于指定连接语句的sql符号)
     * 如不填 默认 select 用 OR 其他语句用 AND
     * 内容 OR 或者 AND
     */
    String joinStr() default "";

}
