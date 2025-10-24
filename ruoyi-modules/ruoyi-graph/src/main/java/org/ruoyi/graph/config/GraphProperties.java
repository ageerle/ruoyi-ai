package org.ruoyi.graph.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识图谱配置属性
 *
 * @author ruoyi
 * @date 2025-09-30
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge.graph")
public class GraphProperties {

    /**
     * 是否启用知识图谱功能
     */
    private Boolean enabled = true;

    /**
     * 图数据库类型: neo4j 或 apache-age
     */
    private String databaseType = "neo4j";

    /**
     * 是否自动创建索引
     */
    private Boolean autoCreateIndex = true;

    /**
     * 批量处理大小
     */
    private Integer batchSize = 1000;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount = 3;

    /**
     * 实体抽取配置
     */
    private ExtractionConfig extraction = new ExtractionConfig();

    /**
     * 查询配置
     */
    private QueryConfig query = new QueryConfig();

    @Data
    public static class ExtractionConfig {
        /**
         * 置信度阈值（低于此值的实体将被过滤）
         */
        private Double confidenceThreshold = 0.7;

        /**
         * 最大实体数量（每个文档）
         */
        private Integer maxEntitiesPerDoc = 100;

        /**
         * 最大关系数量（每个文档）
         */
        private Integer maxRelationsPerDoc = 200;

        /**
         * 文本分片大小（用于长文档）
         */
        private Integer chunkSize = 2000;

        /**
         * 分片重叠大小
         */
        private Integer chunkOverlap = 200;
    }

    @Data
    public static class QueryConfig {
        /**
         * 默认查询限制数量
         */
        private Integer defaultLimit = 100;

        /**
         * 最大查询限制数量
         */
        private Integer maxLimit = 1000;

        /**
         * 路径查询最大深度
         */
        private Integer maxPathDepth = 5;

        /**
         * 查询超时时间（秒）
         */
        private Integer timeoutSeconds = 30;

        /**
         * 是否启用查询缓存
         */
        private Boolean cacheEnabled = true;

        /**
         * 缓存过期时间（分钟）
         */
        private Integer cacheExpireMinutes = 60;
    }
}
