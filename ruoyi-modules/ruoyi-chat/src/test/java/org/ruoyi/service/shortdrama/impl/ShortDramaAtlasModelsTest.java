package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaAtlasModelsTest {
    private static ChatModelVo model(String provider, String name, String category) {
        var model = new ChatModelVo();
        model.setProviderCode(provider); model.setModelName(name); model.setCategory(category);
        model.setApiHost("https://api.atlascloud.ai/v1"); model.setApiKey("test-only-key");
        return model;
    }

    @Test void scopesLookupByAtlasCategoryAndExactNameWithoutUsingGlobalNameLookup() {
        var models = mock(IChatModelService.class);
        var expected = model("atlas", "shared-name", "chat");
        when(models.queryAvailableList(any())).thenReturn(List.of(model("openai", "shared-name", "chat"),
            model("atlas", "shared-name", "image"), model("atlas", "shared-name-longer", "chat"), expected));
        assertSame(expected, ShortDramaAtlasModels.require(models, "shared-name", "chat"));
        var query = ArgumentCaptor.forClass(ChatModelBo.class);
        verify(models).queryAvailableList(query.capture());
        assertEquals("atlas", query.getValue().getProviderCode());
        assertEquals("chat", query.getValue().getCategory());
        assertEquals("shared-name", query.getValue().getModelName());
        verify(models, never()).selectModelByName(any());
    }

    @Test void absentOrDisabledAtlasModelNeverFallsBackToAnotherProvider() {
        var models = mock(IChatModelService.class);
        when(models.queryAvailableList(any())).thenReturn(List.of(model("deepseek", "writer", "chat")));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.require(models, "writer", "chat"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.defaultModel(models, "chat"));
        when(models.queryAvailableList(any())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.defaultModel(models, "chat"));
    }

    @Test void ambiguousAtlasConfigurationMustBeCorrectedRatherThanChoosingAnArbitraryKey() {
        var models = mock(IChatModelService.class);
        when(models.queryAvailableList(any())).thenReturn(List.of(model("atlas", "writer", "chat"), model("atlas", "writer", "chat")));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.require(models, "writer", "chat"));
    }

    @Test void providerLabelCannotSendAtlasCredentialsToAnotherHost() {
        for (String host : List.of("https://api.deepseek.com/v1", "https://api.atlascloud.ai.evil.test/v1",
            "http://api.atlascloud.ai/v1", "https://key@api.atlascloud.ai/v1", "https://api.atlascloud.ai:8443/v1",
            "https://api.atlascloud.ai/v1?key=secret", "")) {
            var model = model("atlas", "writer", "chat"); model.setApiHost(host);
            assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.requireProvider(model), host);
        }
    }

    @Test void missingKeyAndNonAtlasProviderAreRejectedBeforeAnyMediaSubmission() {
        var model = model("atlas", "writer", "chat"); model.setApiKey(" ");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.requireProvider(model));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.requireProvider(model("custom_api", "writer", "chat")));
    }

    @Test void referenceUploadsAcceptAtlasImageOrVideoConfigurationsWithoutAcceptingChatModels() {
        var models = mock(IChatModelService.class);
        var video = model("atlas", "video", "video");
        when(models.queryAvailableList(any())).thenReturn(List.of(video));
        assertSame(video, ShortDramaAtlasModels.require(models, "video", "image", "video"));
        when(models.queryAvailableList(any())).thenReturn(List.of(model("atlas", "writer", "chat")));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaAtlasModels.require(models, "writer", "image", "video"));
    }
}
