package org.ruoyi.service.chat.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.mapper.chat.ChatModelMapper;
import org.ruoyi.service.chat.IChatProviderService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class ChatModelDifyConfigurationTest {
    private static final String KEY = "app-test-key";
    private final ChatModelMapper mapper = mock(ChatModelMapper.class);
    private final IChatProviderService providers = mock(IChatProviderService.class);
    private final ChatModelServiceImpl service = new ChatModelServiceImpl(mapper, providers);

    @Test
    void insertsDifyWithTheAdminConfiguredAppKey() {
        when(mapper.insert(any(ChatModel.class))).thenReturn(1);
        assertTrue(service.insertByBo(command()));
        var saved = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).insert(saved.capture());
        assertEquals(KEY, saved.getValue().getApiKey());
        verify(providers).requireEnabled("dify");
    }

    @Test
    void replacesLegacyDifyPlaceholderThroughNormalModelEdit() {
        var current = new ChatModel();
        current.setId(1L);
        current.setProviderCode("dify");
        current.setModelName("dify-chat");
        current.setApiHost("https://api.dify.ai/v1");
        current.setApiKey("替换为你的DIFY_APP_API_KEY");
        when(mapper.selectOne(any())).thenReturn(current);
        when(mapper.updateById(any(ChatModel.class))).thenReturn(1);
        assertTrue(service.updateByBo(command()));
        var saved = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).updateById(saved.capture());
        assertEquals(KEY, saved.getValue().getApiKey());
        verify(providers).requireEnabled("dify");
    }

    @Test
    void acceptsASelfHostedDifyEndpoint() {
        var command = command();
        command.setApiHost("http://localhost:8080/v1");
        when(mapper.insert(any(ChatModel.class))).thenReturn(1);
        assertTrue(service.insertByBo(command));
        var saved = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).insert(saved.capture());
        assertEquals(command.getApiHost(), saved.getValue().getApiHost());
        assertEquals(KEY, saved.getValue().getApiKey());
    }

    private ChatModelBo command() {
        var command = new ChatModelBo();
        command.setId(1L);
        command.setCategory("chat");
        command.setProviderCode("dify");
        command.setModelName("dify-chat");
        command.setApiHost("https://api.dify.ai/v1");
        command.setApiKey(KEY);
        return command;
    }
}
