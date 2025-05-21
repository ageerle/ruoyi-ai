package org.ruoyi.config.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PDF 配置属性
 *
 * @author zpx
 */
@Data
@Component
@ConfigurationProperties(prefix = "pdf")
public class PdfProperties {

    /**
     * Extract 配置
     */
    private ExtractConfig extract;

    /**
     * Transition 配置
     */
    private TransitionConfig transition;

    @Data
    @NoArgsConstructor
    public static class ExtractConfig {
        /**
         * Service 配置
         */
        private ServiceConfig service;

        /**
         * AI API 配置
         */
        private AiApiConfig aiApi;

        @Data
        @NoArgsConstructor
        public static class ServiceConfig {
            /**
             * 服务地址 URL
             */
            private String url;
        }

        @Data
        @NoArgsConstructor
        public static class AiApiConfig {
            /**
             * AI API 地址 URL
             */
            private String url;

            /**
             * API 密钥
             */
            private String key;
        }
    }

    @Data
    @NoArgsConstructor
    public static class TransitionConfig {
        /**
         * 是否启用 MinerU
         */
        private boolean enableMinerU;

        /**
         * MinerU Conda 环境路径
         */
        private String condaEnvPath;

        /**
         * 是否启用图片 OCR
         */
        private boolean enableOcr;
    }
}