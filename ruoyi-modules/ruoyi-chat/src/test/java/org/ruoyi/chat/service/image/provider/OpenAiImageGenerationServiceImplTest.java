package org.ruoyi.chat.service.image.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.chat.service.media.OpenAiMediaSupport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("dev")
class OpenAiImageGenerationServiceImplTest {

    @Test
    void arkUsesThePlanEndpointAndSeedreamPayload() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var received = new AtomicReference<ImageRequest>();
        server.createContext("/", exchange -> {
            try (exchange) {
                JsonNode payload = OpenAiMediaSupport.OBJECT_MAPPER.readTree(exchange.getRequestBody());
                received.set(new ImageRequest(exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), payload));
                byte[] response = "{\"data\":[{\"url\":\"https://example.test/seedream.png\"}]}"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        });
        server.start();
        try {
            var model = new ChatModelVo();
            model.setProviderCode("ark");
            model.setApiKey("test-plan-token");
            String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/plan/v3";
            model.setApiHost(base + "/");
            model.setModelName("doubao-seedream-5.0-pro");
            var adapter = new OpenAiImageGenerationServiceImpl();

            assertEquals("https://example.test/seedream.png", adapter.generateImage(image(model, " ")));
            ImageRequest first = received.get();
            assertEquals("/api/plan/v3/images/generations", first.path());
            assertEquals("Bearer test-plan-token", first.authorization());
            assertEquals("doubao-seedream-5.0-pro", first.payload().path("model").asText());
            assertEquals("A blue book", first.payload().path("prompt").asText());
            assertEquals("2K", first.payload().path("size").asText());
            assertEquals("png", first.payload().path("output_format").asText());
            assertEquals("url", first.payload().path("response_format").asText());
            assertFalse(first.payload().has("n"));
            assertFalse(first.payload().has("seed"));

            model.setApiHost(base + "/images/generations");
            assertEquals("https://example.test/seedream.png", adapter.generateImage(image(model, "1K")));
            assertEquals(first.path(), received.get().path());
            assertEquals("1K", received.get().payload().path("size").asText());
        } finally {
            server.stop(0);
        }
    }

    private ImageContext image(ChatModelVo model, String size) {
        return ImageContext.builder().chatModelVo(model).prompt("A blue book").size(size).seed(21).build();
    }

    private record ImageRequest(String path, String authorization, JsonNode payload) {
    }
}
