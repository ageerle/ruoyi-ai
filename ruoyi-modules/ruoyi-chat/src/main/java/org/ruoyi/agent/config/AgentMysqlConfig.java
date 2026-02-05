package org.ruoyi.agent.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent MySQL 数据源配置
 * 为 Agent 配置独立的 MySQL 数据库连接池（HikariCP）
 *
 * 仅在 agent.mysql.enabled=true 时启用
 */
@Configuration
@EnableConfigurationProperties(AgentMysqlProperties.class)
@ConditionalOnProperty(name = "agent.mysql.enabled", havingValue = "true")
public class AgentMysqlConfig {

    /**
     * 创建 Agent 专用的数据源
     * 与项目主数据源隔离，独立管理
     *
     * @param properties Agent MySQL 配置属性
     * @return HikariCP 数据源
     */
    @Bean("agentDataSource")
    public DataSource agentDataSource(AgentMysqlProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(properties.getMaxPoolSize());
        config.setMinimumIdle(properties.getMinIdle());
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
