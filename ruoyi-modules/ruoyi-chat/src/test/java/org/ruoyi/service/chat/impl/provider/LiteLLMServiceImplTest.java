package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;
import org.ruoyi.service.chat.AbstractChatService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Unit tests for LiteLLMServiceImpl
 */
@Tag("dev")
class LiteLLMServiceImplTest {

    private LiteLLMServiceImpl liteLLMService;

    @BeforeEach
    void setUp() {
        liteLLMService = new LiteLLMServiceImpl();
    }

    @Test
    void getProviderName_returnsLiteLLMCode() {
        assertEquals("litellm", liteLLMService.getProviderName());
        assertEquals(ChatModeType.LITE_LLM.getCode(), liteLLMService.getProviderName());
    }

    @Test
    void buildStreamingChatModel_usesOpenAiCompatibleProtocol() {
        ChatRequest request = new ChatRequest();
        request.setEnableThinking(false);

        StreamingChatModel model = liteLLMService.buildStreamingChatModel(
            modelVo("http://localhost:4000/v1", "gpt-4o-mini"), request);

        assertInstanceOf(OpenAiStreamingChatModel.class, model);
    }

    @Test
    void buildChatModel_usesOpenAiCompatibleProtocol() {
        ChatModel model = liteLLMService.buildChatModel(
            modelVo("http://localhost:4000/v1", "gpt-4o-mini"));

        assertInstanceOf(OpenAiChatModel.class, model);
    }

    @Test
    void implementsAbstractChatService() {
        assertInstanceOf(AbstractChatService.class, liteLLMService);
    }

    private static ChatModelVo modelVo(String apiHost, String modelName) {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setApiHost(apiHost);
        modelVo.setApiKey("test-api-key");
        modelVo.setModelName(modelName);
        return modelVo;
    }
}
