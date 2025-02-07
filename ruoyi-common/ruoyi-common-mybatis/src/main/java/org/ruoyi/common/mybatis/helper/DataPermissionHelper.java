package org.ruoyi.common.mybatis.helper;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 数据权限助手
 *
 * @author Lion Li
 * @version 3.5.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked cast")
public class DataPermissionHelper {

    private static final String DATA_PERMISSION_KEY = "data:permission";

    public static <T> T getVariable(String key) {
        Map<String, Object> context = getContext();
        return (T) context.get(key);
    }


    public static void setVariable(String key, Object value) {
        Map<String, Object> context = getContext();
        context.put(key, value);
    }

    public static Map<String, Object> getContext() {
        SaStorage saStorage = SaHolder.getStorage();
        Object attribute = saStorage.get(DATA_PERMISSION_KEY);
        if (ObjectUtil.isNull(attribute)) {
            saStorage.set(DATA_PERMISSION_KEY, new HashMap<>());
            attribute = saStorage.get(DATA_PERMISSION_KEY);
        }
        if (attribute instanceof Map map) {
            return map;
        }
        throw new NullPointerException("data permission context type exception");
    }

    /**
     * 开启忽略数据权限(开启后需手动调用 {@link #disableIgnore()} 关闭)
     */
    public static void enableIgnore() {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().dataPermission(true).build());
    }

    /**
     * 关闭忽略数据权限
     */
    public static void disableIgnore() {
        InterceptorIgnoreHelper.clearIgnoreStrategy();
    }

    /**
     * 在忽略数据权限中执行
     *
     * @param handle 处理执行方法
     */
    public static void ignore(Runnable handle) {
        enableIgnore();
        try {
            handle.run();
        } finally {
            disableIgnore();
        }
    }

    /**
     * 在忽略数据权限中执行
     *
     * @param handle 处理执行方法
     */
    public static <T> T ignore(Supplier<T> handle) {
        enableIgnore();
        try {
            return handle.get();
        } finally {
            disableIgnore();
        }
    }

}
