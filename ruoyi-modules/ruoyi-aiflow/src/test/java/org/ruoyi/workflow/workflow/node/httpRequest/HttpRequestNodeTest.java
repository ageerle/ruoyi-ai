package org.ruoyi.workflow.workflow.node.httpRequest;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class HttpRequestNodeTest {

    /**
     * 验证 URL 模板变量按路径段和 Query 组件分别编码。
     *
     * @return 无
     */
    @Test
    void shouldEncodeTemplateValuesByUrlComponent() {
        String pathValue = "folder/sub + & # {\"city\":\"Beijing\"}";
        String queryValue = "c++ & go # {\"city\":\"Beijing\"}";
        List<NodeIOData> inputs = List.of(
            NodeIOData.createByText("path", "", pathValue),
            NodeIOData.createByText("query", "", queryValue)
        );
        HttpRequestNode node = new HttpRequestNode(null, null, null, null);

        String url = ReflectionTestUtils.invokeMethod(
            node,
            "renderUrlTemplate",
            "https://example.com/files/{path}?q={query}",
            inputs
        );
        URI uri = URI.create(url);

        assertEquals(pathValue, uri.getPath().substring("/files/".length()));
        assertTrue(uri.getRawPath().contains("%2F"));
        assertFalse(uri.getRawPath().contains("#"));
        assertFalse(uri.getRawPath().contains("{"));
        assertEquals(queryValue, URLDecoder.decode(uri.getRawQuery().substring("q=".length()), StandardCharsets.UTF_8));
    }

    /**
     * 验证配置的 Query 参数编码后仍位于 URL Fragment 之前。
     *
     * @return 无
     */
    @Test
    void shouldEncodeConfiguredQueryParametersBeforeFragment() {
        String value = "c++ & go # {\"city\":\"Beijing\"}";
        HttpRequestNodeConfig.ParamItem param = new HttpRequestNodeConfig.ParamItem();
        param.setName("filter");
        param.setValue(value);
        HttpRequestNode node = new HttpRequestNode(null, null, null, null);

        String url = ReflectionTestUtils.invokeMethod(
            node,
            "buildUrlWithParams",
            "https://example.com/search?existing=1#section",
            List.of(param),
            List.of()
        );
        URI uri = URI.create(url);

        assertEquals("existing=1&filter=" + URLEncoder.encode(value, StandardCharsets.UTF_8), uri.getRawQuery());
        assertEquals("section", uri.getFragment());
        assertEquals(value, URLDecoder.decode(uri.getRawQuery().substring("existing=1&filter=".length()), StandardCharsets.UTF_8));
    }

    /**
     * 验证 RestTemplate 使用 URI 请求含花括号的 Query 值而不执行模板展开。
     *
     * @return 无
     * @throws IOException 本地 HTTP 服务启动失败时抛出
     */
    @Test
    void shouldSendEncodedUriWithoutTemplateExpansion() throws IOException {
        String value = "{\"city\":\"Beijing\"}";
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            String rawQuery = exchange.getRequestURI().getRawQuery();
            String response = URLDecoder.decode(rawQuery.substring("q=".length()), StandardCharsets.UTF_8);
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });
        server.start();

        try {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                + "/echo?q=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            HttpRequestNode node = new HttpRequestNode(null, null, null, null);

            String response = ReflectionTestUtils.invokeMethod(
                node,
                "executeHttpRequest",
                uri,
                "GET",
                new HttpHeaders(),
                null,
                new HttpRequestNodeConfig()
            );

            assertEquals(value, response);
        } finally {
            server.stop(0);
        }
    }
}
