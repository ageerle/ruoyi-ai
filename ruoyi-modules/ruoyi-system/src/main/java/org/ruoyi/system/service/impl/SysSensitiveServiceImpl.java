package org.ruoyi.system.service.impl;

import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sensitive.core.SensitiveService;
import org.springframework.stereotype.Service;

/**
 * 脱敏服务
 * 默认管理员不过滤
 * 需自行根据业务重写实现
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Service
public class SysSensitiveServiceImpl implements SensitiveService {

    /**
     * 是否脱敏
     */
    @Override
    public boolean isSensitive() {
        return !LoginHelper.isSuperAdmin() || !LoginHelper.isTenantAdmin();
    }

}
