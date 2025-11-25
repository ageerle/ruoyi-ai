package org.ruoyi.workflow.workflow.node.httpRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 请求节点
 */
@Slf4j
public class HttpRequestNode extends AbstractWfNode {

    public HttpRequestNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    @Override
    public NodeProcessResult onProcess() {
        try {
            HttpRequestNodeConfig config = checkAndGetConfig(HttpRequestNodeConfig.class);
            List<NodeIOData> inputs = state.getInputs();

            // 渲染 URL（支持变量替换）
            String url = renderTemplate(config.getUrl(), inputs);
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("请求 URL 不能为空");
            }

            // 添加 Query 参数
            url = buildUrlWithParams(url, config.getParams(), inputs);

            // 构建请求头
            HttpHeaders headers = buildHeaders(config.getHeaders(), inputs);

            // 构建请求体
            Object requestBody = buildRequestBody(config, inputs);

            // 执行 HTTP 请求（支持重试）
            String response = executeHttpRequest(url, config.getMethod(), headers, requestBody, config);

            // 清除 HTML 标签（如果需要）
            if (Boolean.TRUE.equals(config.getClearHtml()) && StringUtils.isNotBlank(response)) {
                response = Jsoup.parse(response).text();
            }

            // 构造输出
            List<NodeIOData> outputs = new ArrayList<>();
            outputs.add(NodeIOData.createByText("output", "HTTP响应", response));

            return NodeProcessResult.builder().content(outputs).build();

        } catch (Exception e) {
            log.error("HTTP 请求失败 in node: {}", node.getId(), e);
            
            // 异常时返回错误信息
            List<NodeIOData> errorOutputs = new ArrayList<>();
            errorOutputs.add(NodeIOData.createByText("output", "错误", ""));
            errorOutputs.add(NodeIOData.createByText("error", "HTTP请求错误", e.getMessage()));
            
            return NodeProcessResult.builder().content(errorOutputs).build();
        }
    }

    /**
     * 渲染模板（支持变量替换）
     * 支持格式：
     * 1. {var_01} - 直接替换整个变量值
     * 2. {var_01.name} - 从 JSON 中提取 name 字段
     * 3. {var_01.user.email} - 支持嵌套路径
     */
    private String renderTemplate(String template, List<NodeIOData> inputs) {
        if (StringUtils.isBlank(template)) {
            return "";
        }
        return renderTemplateWithJsonPath(template, inputs);
    }

    /**
     * 增强的模板渲染，支持 JSON 路径提取
     */
    private String renderTemplateWithJsonPath(String template, List<NodeIOData> inputs) {
        String result = template;
        ObjectMapper mapper = new ObjectMapper();

        for (NodeIOData input : inputs) {
            if (input == null || input.getName() == null) {
                continue;
            }

            String varName = input.getName();
            String varValue = input.valueToString();

            // 1. 处理简单变量替换 {var_01}
            result = result.replace("{" + varName + "}", varValue != null ? varValue : "");

            // 2. 处理 JSON 路径提取 {var_01.field} 或 {var_01.user.name}
            // 尝试解析为 JSON
            Map<String, Object> jsonMap = tryParseJson(varValue, mapper);
            if (jsonMap != null) {
                result = replaceJsonPaths(result, varName, jsonMap);
            }
        }

        return result;
    }

    /**
     * 尝试将字符串解析为 JSON Map
     */
    private Map<String, Object> tryParseJson(String value, ObjectMapper mapper) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        value = value.trim();
        if (!value.startsWith("{") && !value.startsWith("[")) {
            return null;
        }

        try {
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("无法解析为 JSON: {}", value);
            return null;
        }
    }

    /**
     * 替换 JSON 路径变量，如 {var_01.name} 或 {var_01.user.email}
     */
    private String replaceJsonPaths(String template, String varName, Map<String, Object> jsonMap) {
        String result = template;
        
        // 查找所有 {varName.xxx} 格式的占位符
        String pattern = "\\{" + varName + "\\.([\\.\\w]+)\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(template);

        while (m.find()) {
            String fullMatch = m.group(0);  // 如 {var_01.name}
            String jsonPath = m.group(1);   // 如 name 或 user.email

            Object value = extractJsonValue(jsonMap, jsonPath);
            String replacement = value != null ? value.toString() : "";
            
            result = result.replace(fullMatch, replacement);
        }

        return result;
    }

    /**
     * 从 JSON Map 中提取嵌套路径的值
     * 例如：path = "user.email" 会提取 map.get("user").get("email")
     */
    @SuppressWarnings("unchecked")
    private Object extractJsonValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * 构建带参数的 URL
     */
    private String buildUrlWithParams(String baseUrl, List<HttpRequestNodeConfig.ParamItem> params, List<NodeIOData> inputs) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean hasQuery = baseUrl.contains("?");

        for (HttpRequestNodeConfig.ParamItem param : params) {
            if (StringUtils.isBlank(param.getName())) {
                continue;
            }

            String name = renderTemplate(param.getName(), inputs);
            String value = renderTemplate(param.getValue(), inputs);

            if (hasQuery) {
                urlBuilder.append("&");
            } else {
                urlBuilder.append("?");
                hasQuery = true;
            }

            urlBuilder.append(name).append("=").append(value);
        }

        return urlBuilder.toString();
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders(List<HttpRequestNodeConfig.HeaderItem> headerItems, List<NodeIOData> inputs) {
        HttpHeaders headers = new HttpHeaders();

        if (headerItems != null) {
            for (HttpRequestNodeConfig.HeaderItem item : headerItems) {
                if (StringUtils.isNotBlank(item.getName())) {
                    String name = renderTemplate(item.getName(), inputs);
                    String value = renderTemplate(item.getValue(), inputs);
                    headers.add(name, value);
                }
            }
        }

        return headers;
    }

    /**
     * 构建请求体
     */
    private Object buildRequestBody(HttpRequestNodeConfig config, List<NodeIOData> inputs) {
        String method = config.getMethod();
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return null;
        }

        String contentType = config.getContentType();

        // JSON Body
        if ("application/json".equalsIgnoreCase(contentType)) {
            if (config.getJsonBody() != null && !config.getJsonBody().isEmpty()) {
                return renderJsonBody(config.getJsonBody(), inputs);
            }
        }

        // Form Data
        if ("multipart/form-data".equalsIgnoreCase(contentType)) {
            return buildFormData(config.getFormDataBody(), inputs);
        }

        // Form URL Encoded
        if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
            return buildFormUrlEncoded(config.getFormUrlencodedBody(), inputs);
        }

        // Text Body
        if (StringUtils.isNotBlank(config.getTextBody())) {
            return renderTemplate(config.getTextBody(), inputs);
        }

        return null;
    }

    /**
     * 渲染 JSON 请求体
     * 支持三种模式：
     * 1. 普通字段替换：{"name": "{var_01.name}"}
     * 2. 整体 JSON 合并：{"$merge": "{var_01}"} - 将整个 JSON 对象合并进来
     * 3. 智能合并：如果值是 {var_01} 且是有效 JSON，自动展开合并
     */
    private Map<String, Object> renderJsonBody(Map<String, Object> jsonBody, List<NodeIOData> inputs) {
        Map<String, Object> rendered = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<String, Object> entry : jsonBody.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 处理特殊的 $merge 指令
            if ("$merge".equals(key) && value instanceof String) {
                String varRef = (String) value;
                Map<String, Object> mergeData = resolveVariableAsJson(varRef, inputs, mapper);
                if (mergeData != null) {
                    rendered.putAll(mergeData);
                }
                continue;
            }

            if (value instanceof String) {
                String strValue = (String) value;
                
                // 检查是否是单纯的变量引用（如 {var_01}）
                if (strValue.matches("^\\{\\w+\\}$")) {
                    // 尝试解析为 JSON 对象
                    Map<String, Object> jsonValue = resolveVariableAsJson(strValue, inputs, mapper);
                    if (jsonValue != null) {
                        // 如果是 JSON 对象，合并所有字段
                        rendered.putAll(jsonValue);
                    } else {
                        // 否则作为普通字符串处理
                        rendered.put(key, renderTemplate(strValue, inputs));
                    }
                } else {
                    // 普通字符串或包含多个变量的模板
                    rendered.put(key, renderTemplate(strValue, inputs));
                }
            } else if (value instanceof Map) {
                // 递归处理嵌套的 Map
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                rendered.put(key, renderJsonBody(nestedMap, inputs));
            } else {
                // 其他类型直接保留
                rendered.put(key, value);
            }
        }
        return rendered;
    }

    /**
     * 解析变量引用为 JSON 对象
     * 例如：{var_01} -> 尝试解析 var_01 的值为 JSON Map
     */
    private Map<String, Object> resolveVariableAsJson(String varRef, List<NodeIOData> inputs, ObjectMapper mapper) {
        // 提取变量名（去掉 {}）
        String varName = varRef.replaceAll("[{}]", "");
        
        // 查找对应的输入变量
        for (NodeIOData input : inputs) {
            if (input != null && varName.equals(input.getName())) {
                String varValue = input.valueToString();
                return tryParseJson(varValue, mapper);
            }
        }
        
        return null;
    }

    /**
     * 构建 Form Data
     */
    private MultiValueMap<String, String> buildFormData(List<HttpRequestNodeConfig.FormItem> formItems, List<NodeIOData> inputs) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (formItems != null) {
            for (HttpRequestNodeConfig.FormItem item : formItems) {
                if (StringUtils.isNotBlank(item.getName())) {
                    String name = renderTemplate(item.getName(), inputs);
                    String value = renderTemplate(item.getValue(), inputs);
                    formData.add(name, value);
                }
            }
        }
        return formData;
    }

    /**
     * 构建 Form URL Encoded
     */
    private MultiValueMap<String, String> buildFormUrlEncoded(List<HttpRequestNodeConfig.FormItem> formItems, List<NodeIOData> inputs) {
        return buildFormData(formItems, inputs);
    }

    /**
     * 执行 HTTP 请求（支持重试）
     */
    private String executeHttpRequest(String url, String method, HttpHeaders headers, Object body, HttpRequestNodeConfig config) {
        RestTemplate restTemplate = createRestTemplate(config.getTimeout());
        
        int maxRetries = config.getRetryTimes() != null ? config.getRetryTimes() : 0;
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                // 设置 Content-Type
                if (StringUtils.isNotBlank(config.getContentType())) {
                    headers.setContentType(MediaType.parseMediaType(config.getContentType()));
                }

                HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
                HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

                ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, requestEntity, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    return response.getBody();
                } else {
                    throw new RuntimeException("HTTP 请求失败，状态码: " + response.getStatusCode());
                }

            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt <= maxRetries) {
                    log.warn("HTTP 请求失败，正在重试 ({}/{}): {}", attempt, maxRetries, e.getMessage());
                    try {
                        TimeUnit.SECONDS.sleep(1); // 重试前等待 1 秒
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
            }
        }

        throw new RuntimeException("HTTP 请求失败，已重试 " + maxRetries + " 次", lastException);
    }

    /**
     * 创建 RestTemplate（设置超时）
     */
    private RestTemplate createRestTemplate(Integer timeoutSeconds) {
        RestTemplate restTemplate = new RestTemplate();
        
        // 设置超时时间
        int timeout = (timeoutSeconds != null ? timeoutSeconds : 10) * 1000;
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        restTemplate.setRequestFactory(requestFactory);
        
        return restTemplate;
    }
}
