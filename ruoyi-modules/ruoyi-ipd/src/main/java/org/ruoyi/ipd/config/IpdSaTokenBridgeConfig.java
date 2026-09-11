package org.ruoyi.ipd.config;

import cn.dev33.satoken.stp.StpInterface;
import org.ruoyi.common.satoken.core.service.SaPermissionImpl;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdStpInterfaceBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * SEC-API-02：注册带 IPD 分支的 StpInterface，覆盖基线单 Bean 注入到 SaManager。
 */
@Configuration
public class IpdSaTokenBridgeConfig {

    /**
     * 优先注入的 StpInterface：ipd 走目录，其它委托 SaPermissionImpl。
     *
     * @param personMapper 人员 Mapper
     * @return 桥接实现
     */
    @Bean
    @Primary
    public StpInterface ipdAwareStpInterface(PersonMapper personMapper) {
        return new IpdStpInterfaceBridge(new SaPermissionImpl(), personMapper);
    }
}
