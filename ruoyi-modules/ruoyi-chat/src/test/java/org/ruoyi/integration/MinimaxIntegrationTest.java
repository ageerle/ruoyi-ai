package org.ruoyi.integration;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.service.chat.impl.provider.MinimaxServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MiniMax provider.
 * These tests require a valid MINIMAX_API_KEY environment variable.
 */
@EnabledIfEnvironmentVariable(named = "MINIMAX_API_KEY", matches = ".+")
class MinimaxIntegrationTest {

    private MinimaxServiceImpl minimaxService;
    private String apiKey;

    @BeforeEach
    void setUp() {
        minimaxService = new MinimaxServiceImpl();
        apiKey = System.getenv("MINIMAX_API_KEY");
    }

    @Test
    void buildStreamingChatModel_withRealApiKey_M27() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey(apiKey);
        modelVo.setModelName("MiniMax-M2.7");

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model, "Should create streaming model with real API key");
    }

    @Test
    void buildStreamingChatModel_withRealApiKey_M25() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey(apiKey);
        modelVo.setModelName("MiniMax-M2.5");

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model, "Should create streaming model with M2.5");
    }

    @Test
    void buildStreamingChatModel_withRealApiKey_M25Highspeed() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey(apiKey);
        modelVo.setModelName("MiniMax-M2.5-highspeed");

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model, "Should create streaming model with M2.5-highspeed");
    }
}
