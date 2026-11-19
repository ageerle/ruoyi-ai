package org.ruoyi.chat.service.chat;

import org.ruoyi.chat.mapper.ChatMessageImageStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.ConfigService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ChatImageServiceTest {

    private final ChatMessageImageStore store = mock(ChatMessageImageStore.class);
    private final ConfigService config = mock(ConfigService.class);
    private final IChatModelService models = mock(IChatModelService.class);
    private final ChatImageService images = new ChatImageService(store, config, models);

    @AfterEach
    void shutdown() {
        images.shutdown();
    }

    @Test
    void usesFirstNonblankConfiguredModelAsDefault() {
        when(config.getConfigValue(ChatImageService.VISION_MODELS_KEY))
            .thenReturn(" glm-5.3-flash, ,ark-code-latest,deepseek-v4.1-flash ");
        assertEquals("glm-5.3-flash", images.defaultVisionModelName());
        when(config.getConfigValue(ChatImageService.VISION_MODELS_KEY)).thenReturn(" , ");
        assertNull(images.defaultVisionModelName());
    }

    @Test
    void visionOptionsKeepOnlyModelsThatResolveToAnEligibleRow() {
        var usable = model("actual-vision", "ark", "chat", true);
        usable.setModelShow("N");
        usable.setModelDescribe("方舟视觉模型");
        usable.setApiKey("test-only-secret");
        var conflicted = model("duplicate", "qianwen", "chat", true);
        var otherDuplicate = model("duplicate", "zhipu", "chat", true);
        var disabled = model("offline-vision", "ollama", "chat", true);
        var nonChat = model("image-generation", "ark", "image", true);
        var unsupported = model("unsupported", "dify", "chat", true);
        var textOnly = model("text", "ark", "chat", false);
        when(models.queryAvailableList(any(ChatModelBo.class)))
            .thenReturn(List.of(usable, conflicted, otherDuplicate, disabled, nonChat, unsupported, textOnly));
        when(models.selectModelByName("actual-vision")).thenReturn(usable);
        when(models.selectModelByName("duplicate")).thenReturn(model("duplicate", "ark", "chat", false));
        when(models.selectModelByName("offline-vision")).thenThrow(new ServiceException("provider disabled"));
        when(config.getConfigValue(ChatImageService.VISION_MODELS_KEY)).thenReturn("old-model,other-model");

        var settings = images.visionDefault();

        assertEquals("old-model", settings.modelName());
        assertEquals(List.of(new ChatImageService.VisionOption("actual-vision", "方舟视觉模型", "ark")),
            settings.options());
        verify(models).queryAvailableList(argThat(query -> "chat".equals(query.getCategory())
            && query.getModelShow() == null));
        verify(models).selectModelByName("duplicate");
        verify(models, never()).selectModelByName("unsupported");
        verify(models, never()).selectModelByName("text");
    }

    @Test
    void savesAnEligibleModelNameToTheExistingTenantSetting() {
        when(models.selectModelByName("ark-vision")).thenReturn(model("ark-vision", "ark", "chat", true));

        assertTrue(images.updateVisionDefault("ark-vision"));

        verify(config).upsertConfigValue(ChatImageService.VISION_MODELS_KEY, "聊天默认视觉模型", "ark-vision");
    }

    @Test
    void preventsSavingDisabledMissingUnmarkedOrNonCanonicalNames() {
        when(models.selectModelByName("disabled")).thenThrow(new ServiceException("provider disabled"));
        when(models.selectModelByName("text")).thenReturn(model("text", "ark", "chat", false));
        when(models.selectModelByName("ARK-VISION")).thenReturn(model("ark-vision", "ark", "chat", true));

        assertFalse(images.updateVisionDefault("disabled"));
        assertFalse(images.updateVisionDefault("missing"));
        assertFalse(images.updateVisionDefault("text"));
        assertFalse(images.updateVisionDefault("ARK-VISION"));
        assertFalse(images.updateVisionDefault("first,second"));
        assertFalse(images.updateVisionDefault(" "));

        verifyNoInteractions(config);
    }

    private ChatModelVo model(String name, String providerCode, String category, boolean vision) {
        var model = new ChatModelVo();
        model.setModelName(name);
        model.setProviderCode(providerCode);
        model.setCategory(category);
        model.setSupportsVision(vision);
        return model;
    }

    @Test
    void preservesOcrTextButRemovesLegacyImageMarkersFromModelHistory() {
        String input = ChatImageService.modelInput("读一下标题",
            "# 通知\n![](page=0,bbox=[12,50,500,700])\n日期 11 月 19 日");
        assertTrue(input.contains("# 通知"));
        assertTrue(input.contains("11 月 19 日"));
        assertFalse(input.contains("bbox"));
        assertEquals("读一下", ChatImageService.modelInput("读一下", " ![](page=0,bbox=[0,0,10,10]) "));
    }
}
