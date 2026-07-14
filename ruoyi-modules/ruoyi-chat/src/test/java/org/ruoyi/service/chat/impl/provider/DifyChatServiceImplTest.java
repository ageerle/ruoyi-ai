package org.ruoyi.service.chat.impl.provider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ChatModeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
class DifyChatServiceImplTest {

    private final DifyChatServiceImpl service = new DifyChatServiceImpl();

    @Test
    void getProviderName_isDify() {
        assertEquals(ChatModeType.DIFY.getCode(), service.getProviderName());
    }

    @Test
    void buildStreamingChatModel_returnsModel() {
        StreamingChatModel model = service.buildStreamingChatModel(modelVo(), new ChatRequest());

        assertNotNull(model);
    }

    @Test
    void buildChatModel_returnsModel() {
        ChatModel model = service.buildChatModel(modelVo());

        assertNotNull(model);
    }

    private ChatModelVo modelVo() {
        ChatModelVo modelVo = new ChatModelVo();
        modelVo.setProviderCode(ChatModeType.DIFY.getCode());
        modelVo.setModelName("dify-chat");
        modelVo.setApiHost("https://api.dify.ai/v1");
        modelVo.setApiKey("app-test");
        return modelVo;
    }
}
