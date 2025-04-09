package org.ruoyi.system.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.*;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * HttpUtils
 *
 * @author NSL
 * @since 2024-12-30
 */
public class HttpUtils {

    public static final String CHARSET_DEFAULT = "UTF-8";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM_DATA = "application/x-www-form-urlencoded";
    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final int MAX_CONNECT_TIMEOUT = 8000;
    private static final int MAX_SOCKET_TIMEOUT = 90000;

    static {
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(getDefaultRegistry());
        CONNECTION_MANAGER.setMaxTotal(500);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(50);
        CONNECTION_MANAGER.setValidateAfterInactivity(2000);
    }

    public static HttpResponse get(String url) {
        return get(url, null);
    }

    public static HttpResponse get(String url, Map<String, Object> params) {
        String urlLinks = getUrlLinks(params);
        if (urlLinks != null) {
            if (url.contains("?")) {
                url = url + "&" + urlLinks;
            } else {
                url = url + "?" + urlLinks;
            }
        }
        return request(HttpRequest.build(url, "GET"));
    }

    public static void download(String url, File destFile) throws Exception {
        HttpRequest request = HttpRequest.build(url, "GET");
        request.setResponseHandler(entity -> {
            try {
                if (!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }
                try (FileOutputStream fs = new FileOutputStream(destFile)) {
                    entity.writeTo(fs);
                }
                return destFile;
            } catch (Exception e) {
                return e;
            }
        });
        HttpResponse response = request(request);
        if (response.getResponse() instanceof Exception) {
            throw (Exception) response.getResponse();
        }
    }

    public static HttpResponse postJson(String url, String bodyJson) {
        HttpRequest request = HttpRequest.build(url, "POST").setBody(bodyJson);
        if (request.getHeaders() == null) {
            request.setHeaders(Collections.singletonMap(CONTENT_TYPE, CONTENT_TYPE_JSON));
        } else {
            request.getHeaders().put(CONTENT_TYPE, CONTENT_TYPE_JSON);
        }
        return request(request);
    }

    public static HttpResponse postForm(String url, Map<String, Object> params) {
        String urlLinks = getUrlLinks(params);
        HttpRequest request = HttpRequest.build(url, "POST").setBody(urlLinks != null ? urlLinks : "");
        if (request.getHeaders() == null) {
            request.setHeaders(Collections.singletonMap(CONTENT_TYPE, CONTENT_TYPE_FORM_DATA));
        } else {
            request.getHeaders().put(CONTENT_TYPE, CONTENT_TYPE_FORM_DATA);
        }
        return request(request);
    }

    public static HttpResponse requestWithEventStream(ExecutorService executorService, long firstReadTimeout, HttpRequest request, Consumer<String> dataConsumer) throws ExecutionException, InterruptedException, TimeoutException {
        return requestWithEventStream(executorService, firstReadTimeout, request, dataConsumer, null);
    }

    public static HttpResponse requestWithEventStream(ExecutorService executorService, long firstReadTimeout, HttpRequest request, Consumer<String> dataConsumer, Consumer<Future<?>> futureConsumer) throws ExecutionException, InterruptedException, TimeoutException {
        // status: 0 start 1 run 2 timeout
        AtomicInteger status = new AtomicInteger(0);
        Future<HttpResponse> submit = executorService.submit(() -> {
            if (request.getMaxSocketTimeout() == null) {
                request.setMaxSocketTimeout(30_000);
            }
            request.setResponseHandler(entity -> {
                StringBuilder sb = new StringBuilder();
                try {
                    try (InputStream is = entity.getContent()) {
                        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                if (status.get() == 2) {
                                    throw new TimeoutException();
                                }
                                status.set(1);
                                sb.append(line).append("\n");
                                if (line.startsWith("data:")) {
                                    dataConsumer.accept(line.substring(line.startsWith("data: ") ? 6 : 5));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("eventStream 请求异常", e);
                }
                return sb.toString();
            });
            return request(request);
        });
        if (futureConsumer != null) {
            futureConsumer.accept(submit);
        }
        try {
            HttpResponse httpResponse = submit.get(firstReadTimeout, TimeUnit.MILLISECONDS);
            if (httpResponse != null) {
                return httpResponse;
            }
        } catch (TimeoutException e) {
            if (status.get() == 0) {
                status.set(2);
                submit.cancel(true);
                throw e;
            }
        }
        return submit.get();
    }

    public static HttpResponse requestWithEventStream(HttpRequest request, Consumer<String> dataConsumer) {
        if (request.getMaxSocketTimeout() == null) {
            request.setMaxSocketTimeout(30_000);
        }
        request.setResponseHandler(entity -> {
            StringBuilder sb = new StringBuilder();
            try {
                try (InputStream is = entity.getContent()) {
                    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            sb.append(line).append("\n");
                            if (line.startsWith("data:")) {
                                dataConsumer.accept(line.substring(line.startsWith("data: ") ? 6 : 5));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("eventStream 请求异常", e);
            }
            return sb.toString();
        });
        return request(request);
    }

    public static int getUrlHttpStatus(String _url) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(_url);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            return urlConnection.getResponseCode();
        } catch (Exception ignored) {
            return -1;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public static String getUrlLinks(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try {
            String[] sortedKeys = params.keySet().toArray(new String[0]);
            Arrays.sort(sortedKeys);
            for (String key : sortedKeys) {
                if (key == null || key.isEmpty()) {
                    continue;
                }
                Object value = params.get(key);
                sb.append(key).append("=");
                if (value != null) {
                    sb.append(URLEncoder.encode(value.toString(), "UTF-8"));
                }
                sb.append("&");
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseUrlLinks(String params) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return result;
        }
        for (String param : params.split("&")) {
            String[] split = param.split("=");
            String key = split[0];
            String value = "";
            if (split.length > 1) {
                value = split[1];
            }
            if (result.containsKey(key)) {
                Object o = result.get(key);
                if (o instanceof List) {
                    ((List<Object>) o).add(value);
                } else {
                    List<Object> list = new ArrayList<>();
                    list.add(o);
                    list.add(value);
                    result.put(key, list);
                }
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 通用接口请求
     */
    public static HttpResponse request(HttpRequest request) {
        return request(request, 0);
    }

    private static HttpResponse request(HttpRequest request, int retryCount) {
        HttpRequestBase requestBase = toRequest(request);
        Map<String, String> headers = request.getHeaders();
        ContentType contentType = null;
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isEmpty()) {
                    continue;
                }
                String value = entry.getValue();
                if (CONTENT_TYPE.equalsIgnoreCase(key) && value != null) {
                    contentType = ContentType.parse(value);
                }
                requestBase.setHeader(key, value);
            }
        }

        // body
        setBodyEntity(requestBase, contentType, request.getBody());

        try {
            HttpClient client = getHttpClient(request, requestBase);
            org.apache.http.HttpResponse response;
            long startTime = System.currentTimeMillis();
            response = client.execute(requestBase);
            HttpResponse httpResponse = new HttpResponse();
            httpResponse.setReqTime(System.currentTimeMillis() - startTime);
            httpResponse.setStatus(response.getStatusLine().getStatusCode());
            Header[] allHeaders = response.getAllHeaders();
            if (allHeaders != null && allHeaders.length > 0) {
                httpResponse.setHeaders(new HashMap<>());
                for (Header header : allHeaders) {
                    httpResponse.getHeaders().put(header.getName(), header.getValue());
                }
            }
            HttpEntity entity = response.getEntity();
            if (request.responseHandler != null) {
                httpResponse.setResponse(request.responseHandler.apply(entity));
            } else {
                String charset = null;
                if (entity.getContentType() != null && entity.getContentType().getValue() != null) {
                    contentType = ContentType.parse(entity.getContentType().getValue());
                    if (contentType.getCharset() != null) {
                        charset = contentType.getCharset().name();
                    }
                }
                if (charset == null) {
                    charset = CHARSET_DEFAULT;
                }
                httpResponse.setResponse(IOUtils.toString(entity.getContent(), charset));
            }
            return httpResponse;
        } catch (Exception e) {
            requestBase.abort();
            if (request.getMaxRetryCount() > retryCount) {
                return request(request, retryCount + 1);
            } else if (e instanceof SocketException && "Connection reset".equals(e.getMessage()) && retryCount == 0 && request.getMaxRetryCount() == 0) {
                // 遇到 Connection reset 默认重试一次
                return request(request, retryCount + 1);
            } else {
                throw new RuntimeException("请求异常", e);
            }
        } finally {
            requestBase.releaseConnection();
        }
    }

    private static void setBodyEntity(HttpRequestBase requestBase, ContentType contentType, Object body) {
        if (body != null && requestBase instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) requestBase;
            if (body instanceof HttpEntity) {
                entityRequest.setEntity((HttpEntity) body);
            } else if (body instanceof String) {
                entityRequest.setEntity(getStringEntity((String) body, contentType));
            } else if (body instanceof byte[]) {
                entityRequest.setEntity(new ByteArrayEntity((byte[]) body, contentType));
            } else if (body instanceof File) {
                entityRequest.setEntity(new FileEntity((File) body, contentType));
            } else if (body instanceof InputStream) {
                entityRequest.setEntity(new InputStreamEntity((InputStream) body, contentType));
            } else if (ContentType.APPLICATION_JSON.equals(contentType)) {
                entityRequest.setEntity(getStringEntity(JSON.toJSONString(body), contentType));
            } else {
                entityRequest.setEntity(getStringEntity(body.toString(), contentType));
            }
        }
    }

    private static StringEntity getStringEntity(String body, ContentType contentType) {
        if (contentType != null && contentType.getCharset() != null) {
            return new StringEntity(body, contentType);
        } else {
            return new StringEntity(body, CHARSET_DEFAULT);
        }
    }

    public static HttpRequestBase toRequest(HttpRequest request) {
        String url = request.getUrl();
        String method = request.getMethod();
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("url不能为空");
        }
        if (method == null || method.isEmpty()) {
            method = "GET";
        }
        switch (method.toUpperCase()) {
            case "GET":
                return new HttpGet(url);
            case "POST":
                return new HttpPost(url);
            case "PUT":
                return new HttpPut(url);
            case "PATCH":
                return new HttpPatch(url);
            case "DELETE":
                return new HttpDelete(url);
            default:
                throw new RuntimeException("不支持的请求方式：" + method);
        }
    }

    private static HttpClient getHttpClient(HttpRequest req, HttpRequestBase request) {
        RequestConfig.Builder customReqConf = RequestConfig.custom();
        if (req.getMaxSocketTimeout() != null) {
            customReqConf.setSocketTimeout(req.getMaxSocketTimeout());
        } else {
            customReqConf.setSocketTimeout(MAX_SOCKET_TIMEOUT);
        }
        customReqConf.setConnectTimeout(MAX_CONNECT_TIMEOUT);
        customReqConf.setConnectionRequestTimeout(MAX_CONNECT_TIMEOUT);
        if (req.getRequestConfigConsumer() != null) {
            req.getRequestConfigConsumer().accept(customReqConf);
        }
        request.setConfig(customReqConf.build());
        return HttpClients.custom().setConnectionManager(CONNECTION_MANAGER).build();
    }

    private static Registry<ConnectionSocketFactory> getDefaultRegistry() {
        try {
            // ssl: TLS / TLSv1.2 / TLSv1.3
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }

                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            return RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", new SSLConnectionSocketFactory(context)).build();
        } catch (Exception e) {
            return RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
        }
    }

    public static String toParamLinks(Map<String, Object> params, boolean encode) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        try {
            for (String key : keys) {
                if (key == null || "".equals(key)) {
                    continue;
                }
                Object value = params.get(key);
                if (value == null || "".equals(value)) {
                    continue;
                }
                if (encode) {
                    sb.append(key).append("=").append(URLEncoder.encode(value.toString(), "UTF-8")).append("&");
                } else {
                    sb.append(key).append("=").append(value).append("&");
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("编码失败", e);
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static class HttpRequest implements Serializable {

        private String url;
        private String method;
        private Map<String, String> headers;
        private Object body;
        private int maxRetryCount = 0;
        private Integer maxSocketTimeout;
        private Consumer<RequestConfig.Builder> requestConfigConsumer;
        private Function<HttpEntity, Object> responseHandler;

        public static HttpRequest build(String url, String method) {
            HttpRequest request = new HttpRequest();
            request.url = url;
            request.method = method;
            return request;
        }

        public static HttpRequest get(String url) {
            return build(url, "GET");
        }

        public static HttpRequest postJson(String url) {
            return build(url, "POST").setContentType(CONTENT_TYPE_JSON);
        }

        public static HttpRequest postFormData(String url) {
            return build(url, "POST").setContentType(CONTENT_TYPE_FORM_DATA);
        }

        public String getUrl() {
            return url;
        }

        public HttpRequest setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getMethod() {
            return method;
        }

        public HttpRequest setMethod(String method) {
            this.method = method;
            return this;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public HttpRequest setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public HttpRequest setContentType(String contentType) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.put(CONTENT_TYPE, contentType);
            return this;
        }

        public HttpRequest addHeaders(String key, Object value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put(key, value != null ? value.toString() : null);
            return this;
        }

        public Object getBody() {
            return body;
        }

        public HttpRequest setBody(Object body) {
            this.body = body;
            return this;
        }

        public void setMaxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
        }

        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        public HttpRequest setMaxSocketTimeout(Integer maxSocketTimeout) {
            this.maxSocketTimeout = maxSocketTimeout;
            return this;
        }

        public Integer getMaxSocketTimeout() {
            return maxSocketTimeout;
        }

        public Consumer<RequestConfig.Builder> getRequestConfigConsumer() {
            return requestConfigConsumer;
        }

        public void setRequestConfigConsumer(Consumer<RequestConfig.Builder> requestConfigConsumer) {
            this.requestConfigConsumer = requestConfigConsumer;
        }

        public Function<HttpEntity, Object> getResponseHandler() {
            return responseHandler;
        }

        public HttpRequest setResponseHandler(Function<HttpEntity, Object> responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }
    }

    public static class HttpResponse implements Serializable {

        private int status;
        private long reqTime;
        private Object response;
        private Map<String, String> headers;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public long getReqTime() {
            return reqTime;
        }

        public void setReqTime(long reqTime) {
            this.reqTime = reqTime;
        }

        public Object getResponse() {
            return response;
        }

        public String getResponseToString() {
            if (response == null) {
                return null;
            }
            return response instanceof String ? (String) response : String.valueOf(response);
        }

        public JSONObject getResponseToJson() {
            return JSON.parseObject(getResponseToString());
        }

        public void setResponse(Object response) {
            this.response = response;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

}
