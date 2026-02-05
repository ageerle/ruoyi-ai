package org.ruoyi.config;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j配置类

 */
@Slf4j
@Configuration
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jConfig {

    public Neo4jConfig() {
        log.warn("========== Neo4jConfig 已激活 ==========");
        log.warn("知识图谱功能（Neo4j）: 已启用");
        log.warn("========================================");
    }

    /**
     * 创建Neo4j Driver Bean
     *
     * @param neo4jProperties Neo4j 配置属性
     * @return Neo4j Driver
     */
    @Bean
    public Driver neo4jDriver(Neo4jProperties neo4jProperties) {
        log.info("========== 正在初始化 Neo4j Driver ==========");
        log.info("Neo4j 连接地址: {}", neo4jProperties.getUri());
        log.info("Neo4j 用户名: {}", neo4jProperties.getUsername());
        log.info("Neo4j 数据库: {}", neo4jProperties.getDatabase());
        log.info("最大连接池大小: {}", neo4jProperties.getMaxConnectionPoolSize());
        log.info("连接超时时间: {} 秒", neo4jProperties.getConnectionTimeoutSeconds());

        try {
            Driver driver = GraphDatabase.driver(
                    neo4jProperties.getUri(),
                    AuthTokens.basic(neo4jProperties.getUsername(), neo4jProperties.getPassword()),
                    org.neo4j.driver.Config.builder()
                            .withMaxConnectionPoolSize(neo4jProperties.getMaxConnectionPoolSize())
                            .withConnectionTimeout(neo4jProperties.getConnectionTimeoutSeconds(), java.util.concurrent.TimeUnit.SECONDS)
                            .build()
            );
            log.info("========== Neo4j Driver 初始化完成 ==========");
            return driver;
        } catch (Exception e) {
            log.error("Neo4j Driver 初始化失败", e);
            throw new RuntimeException("Failed to initialize Neo4j Driver", e);
        }
    }
}
