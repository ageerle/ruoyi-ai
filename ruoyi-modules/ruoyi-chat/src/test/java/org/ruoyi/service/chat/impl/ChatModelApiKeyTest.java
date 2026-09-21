package org.ruoyi.service.chat.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.entity.chat.ChatModel;
import org.ruoyi.mapper.chat.ChatModelMapper;
import org.ruoyi.service.chat.IChatProviderService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ChatModelApiKeyTest {
    private final ChatModelMapper mapper = mock(ChatModelMapper.class);
    private final IChatProviderService providers = mock(IChatProviderService.class);
    private final ChatModelServiceImpl service = new ChatModelServiceImpl(mapper, providers);

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), ChatModel.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"qianwen", "zhipu", "openai", "deepseek", "ppio", "atlas",
        "minimax", "dify", "coze", "xiaomi", "custom_api", "custom_anthropic", "ollama"})
    void insertPersistsExactlyTheSuppliedKey(String provider) {
        var command = command(provider, " test-key/with:any-format ");
        when(mapper.insert(any(ChatModel.class))).thenReturn(1);
        assertTrue(service.insertByBo(command));
        var saved = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).insert(saved.capture());
        assertEquals(command.getApiKey(), saved.getValue().getApiKey());
        assertEquals(command.getApiHost(), saved.getValue().getApiHost());
        verify(providers).requireEnabled(provider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sk-new-key", " env:LITERAL_VALUE ", "env:ATLAS_API_KEY"})
    void editReplacesAnOldReferenceWithExactlyTheSuppliedValue(String key) {
        when(mapper.selectOne(any())).thenReturn(currentModel());
        when(mapper.updateById(any(ChatModel.class))).thenReturn(1);
        assertTrue(service.updateByBo(command("qianwen", key)));
        var saved = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).updateById(saved.capture());
        assertEquals(key, saved.getValue().getApiKey());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void editWithoutANewKeyDoesNotOverwriteTheExistingKey(String key) {
        when(mapper.selectOne(any())).thenReturn(currentModel());
        when(mapper.updateById(any(ChatModel.class))).thenReturn(1);
        assertTrue(service.updateByBo(command("qianwen", key)));
        var saved = ArgumentCaptor.forClass(ChatModel.class);
        verify(mapper).updateById(saved.capture());
        assertNull(saved.getValue().getApiKey());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void batchUpdateUsesTheSuppliedKeyAndOnlyTheSelectedProvider() {
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(2);
        String key = " raw-batch-key ";
        assertTrue(service.updateApiKeyByProvider("qianwen", key));
        var update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mapper).update(isNull(), update.capture());
        var wrapper = update.getValue();
        assertTrue(wrapper.getSqlSet().contains("api_key"));
        assertTrue(wrapper.getSqlSegment().contains("provider_code"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(key));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("qianwen"));
    }

    @Test
    void emptyBatchKeyCannotEraseProviderCredentials() {
        assertThrows(IllegalArgumentException.class, () -> service.updateApiKeyByProvider("qianwen", ""));
        verifyNoInteractions(mapper);
    }

    private ChatModelBo command(String provider, String key) {
        var command = new ChatModelBo();
        command.setId(1L);
        command.setCategory("embedding");
        command.setProviderCode(provider);
        command.setModelName("text-embedding-v3");
        command.setApiHost("http://localhost:11434/v1");
        command.setApiKey(key);
        return command;
    }

    private ChatModel currentModel() {
        var current = new ChatModel();
        current.setId(1L);
        current.setProviderCode("qianwen");
        current.setApiKey("env:OLD_API_KEY");
        return current;
    }
}
