package com.xmzs.common.tenant.helper;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.spring.SpringMVCUtil;
import cn.hutool.core.convert.Convert;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.xmzs.common.core.constant.GlobalConstants;
import com.xmzs.common.core.utils.SpringUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.redis.utils.RedisUtils;
import com.xmzs.common.satoken.utils.LoginHelper;

import java.util.function.Supplier;

/**
 * 租户助手
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TenantHelper {

    private static final String DYNAMIC_TENANT_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "dynamicTenant";

    private static final ThreadLocal<String> TEMP_DYNAMIC_TENANT = new TransmittableThreadLocal<>();

    /**
     * 租户功能是否启用
     */
    public static boolean isEnable() {
        return Convert.toBool(SpringUtils.getProperty("tenant.enable"), false);
    }

    /**
     * 开启忽略租户(开启后需手动调用 {@link #disableIgnore()} 关闭)
     */
    public static void enableIgnore() {
        InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
    }

    /**
     * 关闭忽略租户
     */
    public static void disableIgnore() {
        InterceptorIgnoreHelper.clearIgnoreStrategy();
    }

    /**
     * 在忽略租户中执行
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
     * 在忽略租户中执行
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

    /**
     * 设置动态租户(一直有效 需要手动清理)
     * <p>
     * 如果为非web环境 那么只在当前线程内生效
     */
    public static void setDynamic(String tenantId) {
        if (!SpringMVCUtil.isWeb()) {
            TEMP_DYNAMIC_TENANT.set(tenantId);
            return;
        }
        String cacheKey = DYNAMIC_TENANT_KEY + ":" + LoginHelper.getUserId();
        RedisUtils.setCacheObject(cacheKey, tenantId);
        SaHolder.getStorage().set(cacheKey, tenantId);
    }

    /**
     * 获取动态租户(一直有效 需要手动清理)
     * <p>
     * 如果为非web环境 那么只在当前线程内生效
     */
    public static String getDynamic() {
        if (!SpringMVCUtil.isWeb()) {
            return TEMP_DYNAMIC_TENANT.get();
        }
        String cacheKey = DYNAMIC_TENANT_KEY + ":" + LoginHelper.getUserId();
        String tenantId = (String) SaHolder.getStorage().get(cacheKey);
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }
        tenantId = RedisUtils.getCacheObject(cacheKey);
        SaHolder.getStorage().set(cacheKey, tenantId);
        return tenantId;
    }

    /**
     * 清除动态租户
     */
    public static void clearDynamic() {
        if (!SpringMVCUtil.isWeb()) {
            TEMP_DYNAMIC_TENANT.remove();
            return;
        }
        String cacheKey = DYNAMIC_TENANT_KEY + ":" + LoginHelper.getUserId();
        RedisUtils.deleteObject(cacheKey);
        SaHolder.getStorage().delete(cacheKey);
    }

    /**
     * 获取当前租户id(动态租户优先)
     */
    public static String getTenantId() {
        String tenantId = TenantHelper.getDynamic();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = LoginHelper.getTenantId();
        }
        return tenantId;
    }

}
