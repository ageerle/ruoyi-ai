package org.ruoyi.mcpserve.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工具配置属性类
 *
 * @author OpenX
 */
@Data
@Component
@ConfigurationProperties(prefix = "tools")
public class ToolsProperties {

    /**
     * Pexels图片搜索配置
     */
    private Pexels pexels = new Pexels();

    /**
     * Tavily搜索配置
     */
    private Tavily tavily = new Tavily();

    /**
     * 文件操作配置
     */
    private FileConfig file = new FileConfig();

    @Data
    public static class Pexels {
        /**
         * Pexels API密钥
         */
        private String apiKey;

        /**
         * API地址
         */
        private String apiUrl;
    }

    @Data
    public static class Tavily {
        /**
         * Tavily API密钥
         */
        private String apiKey;

        /**
         * API地址
         */
        private String baseUrl;
    }

    @Data
    public static class FileConfig {
        /**
         * 文件保存目录
         */
        private String saveDir;
    }
}
