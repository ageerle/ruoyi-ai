package org.ruoyi.agent.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 架构初始化器
 * 在应用启动完成后自动初始化表结构缓存
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "agent.mysql.enabled", havingValue = "true")
public class TableSchemaInitializer {

    @Autowired(required = false)
    private TableSchemaManager tableSchemaManager;

    /**
     * 应用启动完成后初始化
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initializeOnStartup() {
        if (tableSchemaManager != null) {
            tableSchemaManager.initializeSchema();
        }
    }
}
