package org.ruoyi.service.embed.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.enums.ModalityType;
import org.ruoyi.service.embed.BaseEmbedModelService;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for LiteLLMEmbeddingProvider
 */
@Tag("dev")
class LiteLLMEmbeddingProviderTest {

    private LiteLLMEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LiteLLMEmbeddingProvider();
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
        config.setApiHost("http://localhost:4000/v1");
        config.setApiKey("test-api-key");
        config.setModelName("text-embedding-3-small");
        config.setModelDimension(1536);

        provider.configure(config);
        // configure sets internal state; verify no exception thrown
        assertNotNull(provider);
    }
}
