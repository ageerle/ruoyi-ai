package org.ruoyi.service.vector.impl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.config.VectorStoreProperties;
import org.ruoyi.factory.EmbeddingModelFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Tag("dev")
class WeaviateVectorStoreStrategyTest {

    private HttpServer server;
    private final List<String> integrationHeaders = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        // Stand-in Weaviate endpoint that records the headers the client sends.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/schema", exchange -> {
            String header = exchange.getRequestHeaders().getFirst("X-Weaviate-Client-Integration");
            if (header != null) integrationHeaders.add(header);
            byte[] body = "{\"classes\":[{\"class\":\"LocalKnowledge42\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsIntegrationHeaderToWeaviate() {
        VectorStoreProperties properties = new VectorStoreProperties();
        properties.getWeaviate().setProtocol("http");
        properties.getWeaviate().setHost("127.0.0.1:" + server.getAddress().getPort());
        properties.getWeaviate().setClassname("LocalKnowledge");
        WeaviateVectorStoreStrategy strategy = new WeaviateVectorStoreStrategy(properties,
            mock(IChatModelService.class), mock(EmbeddingModelFactory.class), mock(KnowledgeAttachMapper.class));

        strategy.createSchema("42", "test-embedding");

        assertEquals(List.of(WeaviateVectorStoreStrategy.INTEGRATION_VALUE), integrationHeaders);
        assertTrue(WeaviateVectorStoreStrategy.INTEGRATION_VALUE.matches("ruoyi-ai/\\S+"));
    }
}
