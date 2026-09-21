package org.ruoyi.factory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.observability.EmbeddingModelListenerProvider;
import org.ruoyi.service.embed.BaseEmbedModelService;
import org.ruoyi.service.rerank.RerankModelService;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ModelKeyRotationTest {
    @ParameterizedTest
    @ValueSource(strings = {"same", "key", "host", "disabled"})
    void embeddingCacheUsesCurrentConfiguration(String change) {
        var context = mock(ApplicationContext.class);
        var configs = mock(IChatModelService.class);
        var listeners = mock(EmbeddingModelListenerProvider.class);
        when(listeners.getEmbeddingModelListeners()).thenReturn(List.of());
        var first = mock(BaseEmbedModelService.class);
        var next = mock(BaseEmbedModelService.class);
        when(context.getBean("alibailian", BaseEmbedModelService.class)).thenReturn(first, next);
        var initial = configuration("same");
        var updated = configuration(change);
        when(configs.selectModelByName("test-model")).thenReturn(initial, updated);
        var factory = new EmbeddingModelFactory(context, configs, listeners);
        assertSame(first, factory.createModel("test-model"));
        if ("disabled".equals(change)) {
            when(configs.selectModelByName("test-model")).thenThrow(new IllegalArgumentException("disabled"));
            assertThrows(IllegalArgumentException.class, () -> factory.createModel("test-model"));
        } else if ("same".equals(change)) {
            assertSame(first, factory.createModel("test-model"));
            verify(context, times(1)).getBean("alibailian", BaseEmbedModelService.class);
        } else {
            assertSame(next, factory.createModel("test-model"));
            verify(next).configure(updated);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"same", "key", "host", "disabled"})
    void rerankCacheUsesCurrentConfiguration(String change) {
        var context = mock(ApplicationContext.class);
        var configs = mock(IChatModelService.class);
        var first = mock(RerankModelService.class);
        var next = mock(RerankModelService.class);
        when(context.getBean("qianwenRerank", RerankModelService.class)).thenReturn(first, next);
        var initial = configuration("same");
        var updated = configuration(change);
        when(configs.selectModelByName("test-model")).thenReturn(initial, updated);
        var factory = new RerankModelFactory(context, configs);
        assertSame(first, factory.createModel("test-model"));
        if ("disabled".equals(change)) {
            when(configs.selectModelByName("test-model")).thenThrow(new IllegalArgumentException("disabled"));
            assertThrows(IllegalArgumentException.class, () -> factory.createModel("test-model"));
        } else if ("same".equals(change)) {
            assertSame(first, factory.createModel("test-model"));
            verify(context, times(1)).getBean("qianwenRerank", RerankModelService.class);
        } else {
            assertSame(next, factory.createModel("test-model"));
            verify(next).configure(updated);
        }
    }

    private ChatModelVo configuration(String change) {
        var config = new ChatModelVo();
        config.setId(1L);
        config.setModelName("test-model");
        config.setProviderCode("qianwen");
        config.setApiKey("key".equals(change) ? "test-new-key" : "test-old-key");
        config.setApiHost("host".equals(change) ? "https://new.example/v1" : "https://old.example/v1");
        return config;
    }
}
