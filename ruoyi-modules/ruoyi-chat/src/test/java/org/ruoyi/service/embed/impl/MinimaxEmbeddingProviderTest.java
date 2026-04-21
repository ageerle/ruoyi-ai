package org.ruoyi.service.embed.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ModalityType;
import org.ruoyi.service.embed.BaseEmbedModelService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MinimaxEmbeddingProvider
 */
class MinimaxEmbeddingProviderTest {

    private MinimaxEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MinimaxEmbeddingProvider();
    }

    @Test
    void implementsBaseEmbedModelService() {
        assertInstanceOf(BaseEmbedModelService.class, provider);
    }

    @Test
    void extendsOpenAiEmbeddingProvider() {
        assertInstanceOf(OpenAiEmbeddingProvider.class, provider);
    }

    @Test
    void getSupportedModalities_returnsText() {
        Set<ModalityType> modalities = provider.getSupportedModalities();
        assertNotNull(modalities);
        assertTrue(modalities.contains(ModalityType.TEXT));
        assertEquals(1, modalities.size());
    }

    @Test
    void configure_setsModelConfig() {
        ChatModelVo config = new ChatModelVo();
        config.setApiHost("https://api.minimax.io/v1");
        config.setApiKey("test-api-key");
        config.setModelName("embo-01");
        config.setModelDimension(1536);

        provider.configure(config);
        // configure sets internal state; verify no exception thrown
        assertNotNull(provider);
    }
}
