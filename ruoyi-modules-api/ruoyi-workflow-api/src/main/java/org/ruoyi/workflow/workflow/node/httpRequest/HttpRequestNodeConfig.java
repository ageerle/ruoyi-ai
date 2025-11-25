package org.ruoyi.workflow.workflow.node.httpRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HTTP 请求节点配置
 */
@Data
public class HttpRequestNodeConfig {
    
    /**
     * HTTP 请求方法
     */
    private String method = "GET";
    
    /**
     * 请求 URL
     */
    private String url;
    
    /**
     * Content-Type
     */
    @JsonProperty("content_type")
    private String contentType = "text/plain";
    
    /**
     * 请求头列表
     */
    private List<HeaderItem> headers;
    
    /**
     * Query 参数列表
     */
    private List<ParamItem> params;
    
    /**
     * 纯文本请求体
     */
    @JsonProperty("text_body")
    private String textBody;
    
    /**
     * JSON 请求体
     */
    @JsonProperty("json_body")
    private Map<String, Object> jsonBody;
    
    /**
     * Form Data 请求体
     */
    @JsonProperty("form_data_body")
    private List<FormItem> formDataBody;
    
    /**
     * Form URL Encoded 请求体
     */
    @JsonProperty("form_urlencoded_body")
    private List<FormItem> formUrlencodedBody;
    
    /**
     * 请求体（通用）
     */
    private Map<String, Object> body;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout = 10;
    
    /**
     * 重试次数
     */
    @JsonProperty("retry_times")
    private Integer retryTimes = 0;
    
    /**
     * 是否清除 HTML 标签
     */
    @JsonProperty("clear_html")
    private Boolean clearHtml = false;
    
    /**
     * 请求头项
     */
    @Data
    public static class HeaderItem {
        private String name;
        private String value;
    }
    
    /**
     * Query 参数项
     */
    @Data
    public static class ParamItem {
        private String name;
        private String value;
    }
    
    /**
     * Form 表单项
     */
    @Data
    public static class FormItem {
        private String name;
        private String value;
    }
}
