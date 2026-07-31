package org.ruoyi.service.chat.impl.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MinimaxServiceImpl
 */
class MinimaxServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MinimaxServiceImpl minimaxService;

    @BeforeEach
    void setUp() {
        minimaxService = new MinimaxServiceImpl();
    }

    @Test
    void getProviderName_returnsMinimaxCode() {
        assertEquals("minimax", minimaxService.getProviderName());
        assertEquals(ChatModeType.MINIMAX.getCode(), minimaxService.getProviderName());
    }

    @ParameterizedTest
    @CsvSource({
        "https://api.minimax.io/v1, MiniMax-M3, false, openai",
        "https://api.minimax.io/v1, MiniMax-M2.7, true, openai",
        "https://api.minimaxi.com/v1, MiniMax-M3, true, openai",
        "https://api.minimaxi.com/v1, MiniMax-M2.7, true, openai",
        "https://api.minimax.io/anthropic, MiniMax-M3, false, anthropic",
        "https://api.minimax.io/anthropic, MiniMax-M2.7, true, anthropic",
        "https://api.minimaxi.com/anthropic, MiniMax-M3, true, anthropic",
        "https://api.minimaxi.com/anthropic, MiniMax-M2.7, true, anthropic"
    })
    void buildStreamingChatModel_supportsCurrentModelsRegionsAndProtocols(
        String apiHost, String modelName, boolean enableThinking, String protocol) {
        ChatRequest request = new ChatRequest();
        request.setEnableThinking(enableThinking);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(
            modelVo(apiHost, modelName), request);

        if ("anthropic".equals(protocol)) {
            assertInstanceOf(AnthropicStreamingChatModel.class, model);
        } else {
            assertInstanceOf(OpenAiStreamingChatModel.class, model);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "https://api.minimax.io/v1, openai",
        "https://api.minimaxi.com/v1, openai",
        "https://api.minimax.io/anthropic, anthropic",
        "https://api.minimaxi.com/anthropic, anthropic"
    })
    void buildChatModel_supportsRegionalProtocolEndpoints(String apiHost, String protocol) {
        ChatModel model = minimaxService.buildChatModel(modelVo(apiHost, "MiniMax-M3"));

        if ("anthropic".equals(protocol)) {
            assertInstanceOf(AnthropicChatModel.class, model);
        } else {
            assertInstanceOf(OpenAiChatModel.class, model);
        }
    }

    @ParameterizedTest
    @CsvSource({"true, adaptive", "false, disabled"})
    void buildStreamingChatModel_mapsM3ThinkingSetting(boolean enabled, String expectedType) {
        ChatRequest request = new ChatRequest();
        request.setEnableThinking(enabled);

        OpenAiStreamingChatModel model = assertInstanceOf(OpenAiStreamingChatModel.class,
            minimaxService.buildStreamingChatModel(
                modelVo("https://api.minimax.io/v1", "MiniMax-M3"), request));
        OpenAiChatRequestParameters parameters = assertInstanceOf(
            OpenAiChatRequestParameters.class, model.defaultRequestParameters());
        Map<?, ?> thinking = assertInstanceOf(
            Map.class, parameters.customParameters().get("thinking"));

        assertEquals(expectedType, thinking.get("type"));
        assertEquals(expectedType, MinimaxServiceImpl.thinkingType("MiniMax-M3", enabled));
    }

    @Test
    void buildStreamingChatModel_leavesM27ThinkingAlwaysOn() {
        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        OpenAiStreamingChatModel model = assertInstanceOf(OpenAiStreamingChatModel.class,
            minimaxService.buildStreamingChatModel(
                modelVo("https://api.minimax.io/v1", "MiniMax-M2.7"), request));
        OpenAiChatRequestParameters parameters = assertInstanceOf(
            OpenAiChatRequestParameters.class, model.defaultRequestParameters());

        assertNull(MinimaxServiceImpl.thinkingType("MiniMax-M2.7", false));
        assertTrue(parameters.customParameters() == null || parameters.customParameters().isEmpty());
    }

    @Test
    void buildChatModel_appendsAnthropicMessagesPathInternally() throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"id\":\"msg_test\",\"type\":\"message\","
                + "\"role\":\"assistant\",\"model\":\"MiniMax-M3\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"ok\"}],"
                + "\"stop_reason\":\"end_turn\",\"stop_sequence\":null,"
                + "\"usage\":{\"input_tokens\":1,\"output_tokens\":1}}")
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/anthropic";
            ChatModel model = minimaxService.buildChatModel(modelVo(baseUrl, "MiniMax-M3"));

            assertEquals("ok", model.chat("Hello"));
            assertEquals("/anthropic/v1/messages", requestPath.get());
            JsonNode body = OBJECT_MAPPER.readTree(requestBody.get());
            assertEquals("MiniMax-M3", body.path("model").asText());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void implementsAbstractChatService() {
        assertInstanceOf(org.ruoyi.service.chat.AbstractChatService.class, minimaxService);
    }

    private static ChatModelVo modelVo(String apiHost, String modelName) {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost(apiHost);
        modelVo.setApiKey("test-api-key");
        modelVo.setModelName(modelName);
        return modelVo;
    }
}
