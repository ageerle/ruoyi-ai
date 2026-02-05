package org.ruoyi.common.core.utils;

import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * 对象工具类
 *
 * @author 秋辞未寒
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectUtils extends ObjectUtil {

    /**
     * 如果对象不为空，则获取对象中的某个字段 ObjectUtils.notNullGetter(user, User::getName);
     *
     * @param obj 对象
     * @param func 获取方法
     * @return 对象字段
     */
    public static <T, E> E notNullGetter(T obj, Function<T, E> func) {
        if (isNotNull(obj) && isNotNull(func)) {
            return func.apply(obj);
        }
        return null;
    }

    /**
     * 如果对象不为空，则获取对象中的某个字段，否则返回默认值
     *
     * @param obj          对象
     * @param func         获取方法
     * @param defaultValue 默认值
     * @return 对象字段
     */
    public static <T, E> E notNullGetter(T obj, Function<T, E> func, E defaultValue) {
        if (isNotNull(obj) && isNotNull(func)) {
            return func.apply(obj);
        }
        return defaultValue;
    }

    /**
     * 如果值不为空，则返回值，否则返回默认值
     *
     * @param obj          对象
     * @param defaultValue 默认值
     * @return 对象字段
     */
    public static <T> T notNull(T obj, T defaultValue) {
        if (isNotNull(obj)) {
            return obj;
        }
        return defaultValue;
    }

}
