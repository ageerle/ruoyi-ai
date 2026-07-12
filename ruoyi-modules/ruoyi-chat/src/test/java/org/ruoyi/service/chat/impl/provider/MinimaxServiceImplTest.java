package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MinimaxServiceImpl
 */
class MinimaxServiceImplTest {

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
        "https://api.minimax.io/v1, MiniMax-M3, false",
        "https://api.minimax.io/v1, MiniMax-M2.7, true",
        "https://api.minimaxi.com/v1, MiniMax-M3, true",
        "https://api.minimaxi.com/v1, MiniMax-M2.7, true"
    })
    void buildStreamingChatModel_supportsCurrentModelsAndRegionalEndpoints(
        String apiHost, String modelName, boolean enableThinking) {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost(apiHost);
        modelVo.setApiKey("test-api-key");
        modelVo.setModelName(modelName);

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(enableThinking);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model);
    }

    @Test
    void implementsAbstractChatService() {
        assertInstanceOf(org.ruoyi.service.chat.AbstractChatService.class, minimaxService);
    }
}
