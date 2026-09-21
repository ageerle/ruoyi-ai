package org.ruoyi.service.embed.impl;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@Tag("dev")
class QianwenAdminApiKeyTest {
    @Test
    void embeddingUsesTheAdminConfiguredKeyWithoutEnvironmentVariables() {
        var config = new ChatModelVo();
        config.setProviderCode("qianwen");
        config.setModelName("text-embedding-v3");
        config.setModelDimension(1024);
        config.setApiKey("sk-test-qianwen-key");
        var provider = new AliBaiLianBaseEmbedProvider();
        provider.configure(config);
        var builder = spy(QwenEmbeddingModel.builder());
        var client = mock(QwenEmbeddingModel.class);
        var response = Response.from(List.of(Embedding.from(new float[]{1, 0})));
        doReturn(client).when(builder).build();
        when(client.embedAll(anyList())).thenReturn(response);
        try (var factory = mockStatic(QwenEmbeddingModel.class)) {
            factory.when(QwenEmbeddingModel::builder).thenReturn(builder);
            assertSame(response, provider.embedAll(List.of(TextSegment.from("test document"))));
            verify(builder).apiKey("sk-test-qianwen-key");
            verify(builder).modelName("text-embedding-v3");
            verify(client).embedAll(anyList());
        }
    }
}
