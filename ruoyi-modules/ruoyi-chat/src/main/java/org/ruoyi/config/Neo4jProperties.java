package org.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Neo4j 配置属性
 *
 * 分离 @ConfigurationProperties 避免与 @ConditionalOnProperty 冲突
 * 不使用 @Component，而是在 Neo4jConfig 中通过 @EnableConfigurationProperties 启用
 * 参考: https://github.com/spring-projects/spring-boot/issues/26251
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@ConfigurationProperties(prefix = "neo4j")
public class Neo4jProperties {

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
}
