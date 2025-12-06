package org.ruoyi.graph.config;

import lombok.Data;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j配置类
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@Configuration
@ConditionalOnProperty(prefix = "knowledge.graph", name = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "neo4j")
public class Neo4jConfig {

    /**
     * Neo4j连接URI
     * 例如: bolt://localhost:7687
     */
    private String uri;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 数据库名称（Neo4j 4.0+支持多数据库）
     * 默认: neo4j
     */
    private String database = "neo4j";

    /**
     * 最大连接池大小
     */
    private Integer maxConnectionPoolSize = 50;

    /**
     * 连接超时时间（秒）
     */
    private Integer connectionTimeoutSeconds = 30;

    /**
     * 创建Neo4j Driver Bean
     *
     * @return Neo4j Driver
     */
    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(
                uri,
                AuthTokens.basic(username, password),
                org.neo4j.driver.Config.builder()
                        .withMaxConnectionPoolSize(maxConnectionPoolSize)
                        .withConnectionTimeout(connectionTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
        );
    }
}
