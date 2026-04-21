package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void buildStreamingChatModel_returnsNonNull() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey("test-api-key");
        modelVo.setModelName("MiniMax-M2.7");

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model);
    }

    @Test
    void buildStreamingChatModel_withThinkingEnabled() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey("test-api-key");
        modelVo.setModelName("MiniMax-M2.5");

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(true);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model);
    }

    @Test
    void buildStreamingChatModel_withHighspeedModel() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost("https://api.minimax.io/v1");
        modelVo.setApiKey("test-api-key");
        modelVo.setModelName("MiniMax-M2.5-highspeed");

        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        StreamingChatModel model = minimaxService.buildStreamingChatModel(modelVo, request);
        assertNotNull(model);
    }

    @Test
    void implementsAbstractChatService() {
        assertInstanceOf(org.ruoyi.service.chat.AbstractChatService.class, minimaxService);
    }
}
