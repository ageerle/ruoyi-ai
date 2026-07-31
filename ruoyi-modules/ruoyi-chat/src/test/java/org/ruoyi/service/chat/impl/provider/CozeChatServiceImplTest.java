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
class CozeChatServiceImplTest {

    private final CozeChatServiceImpl service = new CozeChatServiceImpl();

    @Test
    void getProviderName_isCoze() {
        assertEquals(ChatModeType.COZE.getCode(), service.getProviderName());
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
        modelVo.setProviderCode(ChatModeType.COZE.getCode());
        modelVo.setModelName("7480000000000000000");
        modelVo.setApiHost("https://api.coze.cn");
        modelVo.setApiKey("pat-test");
        return modelVo;
    }
}

