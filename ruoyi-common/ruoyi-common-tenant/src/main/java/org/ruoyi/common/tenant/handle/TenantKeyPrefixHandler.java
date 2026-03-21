package org.ruoyi.common.tenant.handle;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.constant.GlobalConstants;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.redis.handler.KeyPrefixHandler;
import org.ruoyi.common.tenant.helper.TenantHelper;

/**
 * 多租户redis缓存key前缀处理
 *
 * @author Lion Li
 */
@Slf4j
public class TenantKeyPrefixHandler extends KeyPrefixHandler {

    public TenantKeyPrefixHandler(String keyPrefix) {
        super(keyPrefix);
    }

    /**
     * 增加前缀
     */
    @Override
    public String map(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        try {
            if (InterceptorIgnoreHelper.willIgnoreTenantLine("")) {
                return super.map(name);
            }
        } catch (NoClassDefFoundError ignore) {
            // 有些服务不需要mp导致类不存在 忽略即可
        }
        if (StringUtils.contains(name, GlobalConstants.GLOBAL_REDIS_KEY)) {
            return super.map(name);
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.debug("无法获取有效的租户id -> Null");
            return super.map(name);
        }
        if (StringUtils.startsWith(name, tenantId + "")) {
            // 如果存在则直接返回
            return super.map(name);
        }
        return super.map(tenantId + ":" + name);
    }

    /**
     * 去除前缀
     */
    @Override
    public String unmap(String name) {
        String unmap = super.unmap(name);
        if (StringUtils.isBlank(unmap)) {
            return null;
        }
        try {
            if (InterceptorIgnoreHelper.willIgnoreTenantLine("")) {
                return unmap;
            }
        } catch (NoClassDefFoundError ignore) {
            // 有些服务不需要mp导致类不存在 忽略即可
        }
        if (StringUtils.contains(name, GlobalConstants.GLOBAL_REDIS_KEY)) {
            return unmap;
        }
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.debug("无法获取有效的租户id -> Null");
            return unmap;
        }
        if (StringUtils.startsWith(unmap, tenantId + "")) {
            // 如果存在则删除
            return unmap.substring((tenantId + ":").length());
        }
        return unmap;
    }

}
