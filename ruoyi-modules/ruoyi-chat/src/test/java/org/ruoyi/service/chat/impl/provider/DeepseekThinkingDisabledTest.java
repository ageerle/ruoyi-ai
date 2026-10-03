package org.ruoyi.service.chat.impl.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class DeepseekThinkingDisabledTest {
    @Test void synchronousAndStreamingCallsSendDisabledOnTheWire() throws Exception {
        var bodies = new CopyOnWriteArrayList<JsonNode>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            var body = new ObjectMapper().readTree(exchange.getRequestBody()); bodies.add(body);
            boolean stream = body.path("stream").asBoolean();
            exchange.getResponseHeaders().set("Content-Type", stream ? "text/event-stream" : "application/json");
            String answer = stream
                ? "data: {\"id\":\"local\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"ok\"}}]}\n\ndata: {\"id\":\"local\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n"
                : "{\"id\":\"local\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}";
            var bytes = answer.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, stream ? 0 : bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var config = new ChatModelVo(); config.setApiHost("http://127.0.0.1:" + server.getAddress().getPort());
            config.setApiKey("local-test-key"); config.setModelName("deepseek-ai/deepseek-v4.1-flash");
            var request = new ChatRequest(); request.setEnableThinking(false); request.setReasoningEffort("none");
            var provider = new DeepseekServiceImpl();
            assertEquals("ok", provider.buildChatModel(config, request).chat("local request"));
            var done = new CountDownLatch(1); var failure = new AtomicReference<Throwable>();
            provider.buildStreamingChatModel(config, request).chat("local request", new StreamingChatResponseHandler() {
                public void onPartialResponse(String text) {}
                public void onCompleteResponse(ChatResponse response) { done.countDown(); }
                public void onError(Throwable error) { failure.set(error); done.countDown(); }
            });
            assertTrue(done.await(10, TimeUnit.SECONDS)); assertNull(failure.get()); assertEquals(2, bodies.size());
            for (var body : bodies) {
                assertEquals("disabled", body.path("thinking").path("type").asText());
                assertNotEquals("high", body.path("reasoning_effort").asText());
            }
        } finally { server.stop(0); }
    }
}
