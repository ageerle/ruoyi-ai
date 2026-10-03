package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.domain.entity.shortdrama.ShortDramaVisualAsset;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev") @ExtendWith(MockitoExtension.class)
class ShortDramaVisualAssetVersioningTest {
    @Mock ShortDramaVisualAssetMapper assets;
    @Mock ShortDramaProjectMapper projects;
    @Mock ShortDramaScriptMapper scripts;
    @Mock ShortDramaStoryboardMapper boards;
    @Mock ShortDramaCharacterMapper characters;
    @Mock ShortDramaCharacterAppearanceMapper appearances;
    @Mock ShortDramaLocationMapper locations;
    @Mock org.ruoyi.common.chat.service.chat.IChatModelService models;
    @Mock org.ruoyi.factory.ChatServiceFactory chats;
    @Mock org.ruoyi.common.chat.factory.ImageServiceFactory images;
    @Mock org.ruoyi.service.media.AtlasPredictionService predictions;
    @Mock ShortDramaSkillCatalog skillCatalog;
    @InjectMocks ShortDramaVisualAssetService service;
    @AfterEach void close() { service.close(); }

    @Test void acceptedOldTaskQueriesOriginalModelWithoutLookingAtNewSkillOrReferences() {
        var asset = new ShortDramaVisualAsset(); asset.setId(1L); asset.setProjectId(7L); asset.setStoryboardId(11L);
        asset.setKind("shot_frame"); asset.setStatus("waiting"); asset.setPredictionId("original-prediction"); asset.setModel("openai/gpt-image-2/edit"); asset.setSourceHash("old-version");
        var original = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo(); original.setModelName(asset.getModel());
        when(models.selectModelByName(asset.getModel())).thenReturn(original);
        when(predictions.retrieve(original, "original-prediction")).thenReturn(org.ruoyi.common.chat.entity.media.MediaGenerationResponse.builder()
            .id("original-prediction").status("completed").url("https://example.com/old-result.png").build());
        ReflectionTestUtils.invokeMethod(service, "generateOne", asset, "bytedance/seedream-v4.7/text-to-image");
        assertEquals("done", asset.getStatus()); assertEquals("old-version", asset.getSourceHash());
        assertEquals("original-prediction", asset.getPredictionId()); assertEquals("https://example.com/old-result.png", asset.getImageUrl());
        verifyNoInteractions(images, skillCatalog, projects, boards, characters, appearances, locations);
        verify(predictions).retrieve(original, "original-prediction");
    }
    @Test void updatingAFrameCreatesRecoverableHistoryBeforeClearingCurrentCandidate() {
        var old = new ShortDramaVisualAsset(); old.setId(2L); old.setProjectId(7L); old.setStoryboardId(11L); old.setAssetKey("shot:11");
        old.setKind("shot_frame"); old.setSourceHash("old-version"); old.setStatus("done"); old.setImageUrl("https://example.com/approved.png"); old.setPredictionId("approved-task"); old.setPrompt("old prompt");
        when(assets.selectOne(any())).thenReturn(old);
        ReflectionTestUtils.invokeMethod(service, "upsert", 7L, 11L, "shot:11", "shot_frame", "new frame", "new prompt", "{}", "new-version");
        var captured = ArgumentCaptor.forClass(ShortDramaVisualAsset.class); verify(assets).insert(captured.capture());
        var history = captured.getValue(); assertEquals("archived", history.getStatus()); assertEquals("archived_shot_frame", history.getKind());
        assertEquals("https://example.com/approved.png", history.getImageUrl()); assertEquals("approved-task", history.getPredictionId()); assertEquals("old-version", history.getSourceHash());
        assertEquals("pending", old.getStatus()); assertEquals("", old.getImageUrl()); assertEquals("", old.getPredictionId());
    }
    @Test void newGenerationRejectsAStaleSkillMarkerButDoesNotRejectUnboundLegacyPrompts() {
        assertThrows(IllegalStateException.class, () -> ShortDramaVisualAssetService.ensureSelectedMarkersCurrent("[selected-skill:art@old]", "[selected-skill:art@new]"));
        assertDoesNotThrow(() -> ShortDramaVisualAssetService.ensureSelectedMarkersCurrent("[selected-skill:art@new]", "[selected-skill:art@new]"));
        assertDoesNotThrow(() -> ShortDramaVisualAssetService.ensureSelectedMarkersCurrent("legacy approved asset description", ""));
    }
}
