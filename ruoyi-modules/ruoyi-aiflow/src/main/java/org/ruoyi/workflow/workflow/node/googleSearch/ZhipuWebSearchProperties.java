package org.ruoyi.workflow.workflow.node.googleSearch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智谱 Web Search 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow.web-search.zhipu")
public class ZhipuWebSearchProperties {

    /**
     * 智谱国内开放平台 API 根地址。
     */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";

    /**
     * 智谱 API Key。建议通过环境变量 ZAI_API_KEY 注入。
     */
    private String apiKey;

    /**
     * 连接超时秒数。
     */
    private Integer connectTimeout = 10;

    /**
     * 读取超时秒数。
     */
    private Integer readTimeout = 30;
}
