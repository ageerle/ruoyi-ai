package org.ruoyi.integration;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({
        "MiniMax-M3, false",
        "MiniMax-M2.7, true"
    })
    void buildStreamingChatModel_withCurrentModel(String modelName, boolean enableThinking) {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey(apiKey);
        modelVo.setModelName(modelName);

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(enableThinking);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model, "Should create streaming model with " + modelName);
    }
}
