package org.ruoyi.chat.factory;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.chat.service.chat.AbstractChatService;
import org.ruoyi.chat.service.chat.impl.provider.CustomApiServiceImpl;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class ArkProviderRoutingTest {

    @Test
    void arkPresetRoutesToTheExistingOpenAiAdapter() {
        CustomApiServiceImpl adapter = new CustomApiServiceImpl();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansOfType(AbstractChatService.class)).thenReturn(Map.of("customApi", adapter));
        ChatServiceFactory factory = new ChatServiceFactory();
        factory.setApplicationContext(context);

        assertEquals("ark", ChatModeType.ARK.getCode());
        assertEquals("火山方舟", ChatModeType.ARK.getDescription());
        assertSame(adapter, factory.getOriginalService("custom_api"));
        AbstractChatService ark = factory.getOriginalService(ChatModeType.ARK.getCode());
        assertSame(adapter, ark);

        ChatModelVo model = new ChatModelVo();
        model.setProviderCode(ChatModeType.ARK.getCode());
        model.setModelName("ark-code-latest");
        model.setApiKey("test-only");
        assertInstanceOf(OpenAiChatModel.class, ark.buildChatModel(model));
        assertInstanceOf(OpenAiStreamingChatModel.class, ark.buildStreamingChatModel(model, new ChatRequest()));
        model.setProviderCode("custom_anthropic");
        assertThrows(IllegalArgumentException.class, () -> ark.buildChatModel(model));
    }
}
