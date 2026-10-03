package org.ruoyi.service.chat.impl.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class SeedThinkingParametersTest {
    @Test void bothSyncAdaptersSendThinkingDisabledToTheProvider() throws Exception {
        for (var provider : java.util.List.of(new AtlaServiceImpl(), new OpenAIServiceImpl())) {
            var body = new AtomicReference<JsonNode>();
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/chat/completions", exchange -> {
                body.set(new ObjectMapper().readTree(exchange.getRequestBody()));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] answer = "{\"id\":\"test\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, answer.length); exchange.getResponseBody().write(answer); exchange.close();
            });
            server.start();
            try {
                var config = new ChatModelVo(); config.setApiHost("http://127.0.0.1:" + server.getAddress().getPort());
                config.setApiKey("local-test-key"); config.setModelName("bytedance/doubao-seed-2.1-pro-260628");
                var request = new ChatRequest(); request.setReasoningEffort("none");
                assertEquals("ok", provider.buildChatModel(config, request).chat("a local test"));
                assertEquals("disabled", body.get().path("thinking").path("type").asText());
                assertEquals("none", body.get().path("reasoning_effort").asText());
            } finally { server.stop(0); }
        }
    }
    @Test void bothAdaptersSendTheRealThinkingSwitchAndReasoningEffortOnTheWire() throws Exception {
        for (var provider : java.util.List.of(new AtlaServiceImpl(), new OpenAIServiceImpl())) {
            var body = new AtomicReference<JsonNode>();
            var failure = new AtomicReference<Throwable>();
            var done = new CountDownLatch(1);
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/chat/completions", exchange -> {
                body.set(new ObjectMapper().readTree(exchange.getRequestBody()));
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                String response = "data: {\"id\":\"test\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"ok\"}}]}\n\n"
                    + "data: {\"id\":\"test\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8)); exchange.close();
            });
            server.start();
            try {
                var config = new ChatModelVo(); config.setApiHost("http://127.0.0.1:" + server.getAddress().getPort());
                config.setApiKey("local-test-key"); config.setModelName("bytedance/doubao-seed-2.1-pro-260628");
                var request = new ChatRequest(); request.setReasoningEffort("none"); request.setEnableThinking(true);
                StreamingChatModel model = provider.buildStreamingChatModel(config, request);
                model.chat("a local test", new StreamingChatResponseHandler() {
                    public void onPartialResponse(String text) {}
                    public void onCompleteResponse(ChatResponse response) { done.countDown(); }
                    public void onError(Throwable error) { failure.set(error); done.countDown(); }
                });
                assertTrue(done.await(10, TimeUnit.SECONDS)); assertNull(failure.get()); assertNotNull(body.get());
                assertEquals("disabled", body.get().path("thinking").path("type").asText());
                assertEquals("none", body.get().path("reasoning_effort").asText());
            } finally { server.stop(0); }
        }
    }
    @Test void otherModelsAndUnrequestedThinkingModesHaveNoProviderOverride() {
        var request = new ChatRequest(); request.setReasoningEffort("none");
        assertTrue(SeedThinkingParameters.forRequest("other-model", request).isEmpty());
        request.setReasoningEffort("low");
        assertTrue(SeedThinkingParameters.forRequest("bytedance/doubao-seed-2.1-pro-260628", request).isEmpty());
        request.setReasoningEffort(null);
        assertTrue(SeedThinkingParameters.forRequest("bytedance/doubao-seed-2.1-pro-260628", request).isEmpty());
    }
}
