package org.ruoyi.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent MySQL 配置属性
 * 前缀：agent.mysql
 *
 * 配置示例：
 * agent:
 *   mysql:
 *     enabled: true
 *     url: jdbc:mysql://localhost:3306/database
 *     username: user
 *     password: password
 *     max-pool-size: 10
 *     min-idle: 2
 */
@Data
@ConfigurationProperties(prefix = "agent.mysql")
public class AgentMysqlProperties {

    /**
     * 是否启用 Agent MySQL 查询功能
     */
    private Boolean enabled = false;

    /**
     * 数据库 URL (jdbc:mysql://host:port/database)
     */
    private String url;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 数据库连接池最大连接数
     */
    private Integer maxPoolSize = 10;

    /**
     * 数据库连接池最小空闲连接数
     */
    private Integer minIdle = 2;
}
