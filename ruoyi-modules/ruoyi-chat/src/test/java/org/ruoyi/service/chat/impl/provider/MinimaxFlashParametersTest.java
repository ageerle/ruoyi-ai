package org.ruoyi.service.chat.impl.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MinimaxFlashParametersTest {
    private static final String FLASH = "MiniMax-M3.1-Flash";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final MinimaxServiceImpl service = new MinimaxServiceImpl();

    static Stream<Arguments> requests() {
        return Stream.of(false, true).flatMap(anthropic -> Stream.of(false, true)
            .flatMap(streaming -> Stream.of(false, true).flatMap(visible ->
                Stream.of("default", "low", "medium", "high", "xhigh", "max")
                    .map(effort -> Arguments.of(anthropic, streaming, visible, effort)))));
    }

    @ParameterizedTest
    @MethodSource("requests")
    void serializesAdaptiveThinkingAndProtocolEffort(
        boolean anthropic, boolean streaming, boolean visible, String effort) throws Exception {
        AtomicReference<JsonNode> body = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            body.set(MAPPER.readTree(exchange.getRequestBody()));
            path.set(exchange.getRequestURI().getPath());
            byte[] response = response(anthropic, streaming).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", streaming ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ChatModelVo config = config("http://127.0.0.1:" + server.getAddress().getPort()
                + (anthropic ? "/anthropic" : "/v1"), FLASH);
            ChatRequest request = new ChatRequest();
            request.setEnableThinking(visible);
            request.setReasoningEffort("default".equals(effort) ? null : effort);
            if (streaming) {
                CountDownLatch done = new CountDownLatch(1);
                AtomicReference<Throwable> failure = new AtomicReference<>();
                service.buildStreamingChatModel(config, request).chat("local test", new StreamingChatResponseHandler() {
                    public void onPartialResponse(String text) {}
                    public void onCompleteResponse(ChatResponse response) { done.countDown(); }
                    public void onError(Throwable error) { failure.set(error); done.countDown(); }
                });
                assertTrue(done.await(10, TimeUnit.SECONDS));
                assertNull(failure.get());
            } else {
                assertEquals("ok", service.buildChatModel(config, request).chat("local test"));
            }
            assertEquals(anthropic ? "/anthropic/v1/messages" : "/v1/chat/completions", path.get());
            assertEquals(FLASH, body.get().path("model").asText());
            assertEquals("adaptive", body.get().path("thinking").path("type").asText());
            assertEquals("default".equals(effort) ? "max" : effort, anthropic
                ? body.get().path("output_config").path("effort").asText()
                : body.get().path("reasoning_effort").asText());
            assertFalse(body.get().has(anthropic ? "reasoning_effort" : "output_config"));
            assertFalse(body.get().has("reasoning_split"));
            assertFalse(body.get().has("service_tier"));
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "minimal", "invalid"})
    void rejectsUnsupportedFlashEffortBeforeSending(String effort) {
        ChatRequest request = new ChatRequest();
        request.setReasoningEffort(effort);
        for (String baseUrl : List.of("https://api.minimax.io/v1", "https://api.minimax.io/anthropic")) {
            assertThrows(IllegalArgumentException.class,
                () -> service.buildStreamingChatModel(config(baseUrl, FLASH), request));
            assertThrows(IllegalArgumentException.class,
                () -> service.buildChatModel(config(baseUrl, FLASH), request));
        }
    }

    @Test
    void preservesM3LegacyAndCustomModelBehavior() {
        ChatRequest request = new ChatRequest();
        request.setReasoningEffort("none");
        request.setEnableThinking(false);
        for (String model : List.of("MiniMax-M3", "MiniMax-M2.7", "custom-minimax")) {
            assertDoesNotThrow(() -> service.buildStreamingChatModel(config("https://api.minimax.io/v1", model), request));
            assertDoesNotThrow(() -> service.buildChatModel(config("https://api.minimax.io/anthropic", model), request));
        }
        assertEquals("disabled", MinimaxServiceImpl.thinkingType("MiniMax-M3", false));
        assertEquals("adaptive", MinimaxServiceImpl.thinkingType("MiniMax-M3", true));
        assertNull(MinimaxServiceImpl.thinkingType("MiniMax-M2.7", false));
        assertNull(MinimaxServiceImpl.thinkingType("custom-minimax", true));
        assertEquals("adaptive", MinimaxServiceImpl.thinkingType(FLASH, false));
        assertDoesNotThrow(() -> service.buildStreamingChatModel(config("https://api.minimax.io/v1", FLASH), null));
    }

    private static ChatModelVo config(String baseUrl, String model) {
        ChatModelVo config = new ChatModelVo();
        config.setApiHost(baseUrl);
        config.setApiKey("test-api-key");
        config.setModelName(model);
        return config;
    }

    private static String response(boolean anthropic, boolean streaming) {
        String message = "{\"id\":\"msg_test\",\"type\":\"message\",\"role\":\"assistant\","
            + "\"model\":\"" + FLASH + "\",\"content\":[{\"type\":\"text\",\"text\":\"ok\"}],"
            + "\"stop_reason\":\"end_turn\",\"stop_sequence\":null,\"usage\":{\"input_tokens\":1,\"output_tokens\":1}}";
        if (!streaming) {
            return anthropic ? message : "{\"id\":\"test\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}";
        }
        if (!anthropic) {
            return "data: {\"id\":\"test\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"ok\"}}]}\n\n"
                + "data: {\"id\":\"test\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
        }
        return "event: message_start\ndata: {\"type\":\"message_start\",\"message\":" + message + "}\n\n"
            + "event: content_block_start\ndata: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}\n\n"
            + "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"ok\"}}\n\n"
            + "event: content_block_stop\ndata: {\"type\":\"content_block_stop\",\"index\":0}\n\n"
            + "event: message_delta\ndata: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"},\"usage\":{\"output_tokens\":1}}\n\n"
            + "event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n";
    }
}
