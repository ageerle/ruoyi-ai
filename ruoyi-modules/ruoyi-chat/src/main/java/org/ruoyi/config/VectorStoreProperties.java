package org.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 向量库配置属性
 *
 * @author ageer
 */
@Data
@Component
@ConfigurationProperties(prefix = "vector-store")
public class VectorStoreProperties {

    /**
     * 向量库类型
     */
    private String type;

    /**
     * Weaviate配置
     */
    private Weaviate weaviate = new Weaviate();

    /**
     * Milvus配置
     */
    private Milvus milvus = new Milvus();

    @Data
    public static class Weaviate {
        /**
         * 协议
         */
        private String protocol;

        /**
         * 主机地址
         */
        private String host;

        /**
         * 类名
         */
        private String classname;
    }

    @Data
    public static class Milvus {
        /**
         * 连接URL
         */
        private String url;

        /**
         * 集合名称
         */
        private String collectionname;
    }

    /**
     * Qdrant配置
     */
    private Qdrant qdrant = new Qdrant();

    @Data
    public static class Qdrant {
        /**
         * 主机地址
         */
        private String host = "localhost";

        /**
         * gRPC端口
         */
        private int port = 6334;

        /**
         * 集合名称
         */
        private String collectionname = "LocalKnowledge";

        /**
         * API密钥（可选）
         */
        private String apiKey;

        /**
         * 是否启用TLS
         */
        private boolean useTls = false;
    }
}
