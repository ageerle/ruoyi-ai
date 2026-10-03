package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Tag("dev") class ShortDramaSkillMarketTest {
    @TempDir Path directory;
    @Test void marketUsesActualTemplatesAndOnlyShortDramaSkills() {
        var catalog = new ShortDramaSkillCatalog(directory.toString());
        var entries = new ShortDramaSkillMarket(catalog).list();
        var prompts = ShortDramaServiceImpl.systemPromptCatalog();
        assertEquals(9, prompts.size());
        for (var entry : entries) {
            if (entry.type().equals("system")) assertEquals(prompts.get(entry.title()), entry.body());
            assertFalse(entry.body().isBlank());
            assertEquals(64, entry.version().length());
        }
        assertEquals(12, entries.stream().filter(e -> e.type().equals("production")).count());
        assertTrue(entries.stream().anyMatch(e -> e.name().equals("video-prompt") && e.body().contains("timing")));
        assertTrue(entries.stream().noneMatch(e -> e.name().contains("vue") || e.name().contains("mcp-builder")));
    }
    @Test void runtimeEditedStylesAndReferencesAreReadWithoutExecution() {
        var catalog = mock(ShortDramaSkillCatalog.class);
        var file = new ShortDramaSkillCatalog.SkillFile("scripts/example.py", "raise RuntimeError('must not execute')");
        var style = new ShortDramaSkillCatalog.Skill("runtime-style", "后台风格", "director", "实际方向", false, "", "v2", "后台正文", List.of(file));
        when(catalog.list(null,true)).thenReturn(List.of(new ShortDramaSkillCatalog.Option(style.name(),style.title(),style.type(),style.description(),style.enabled(),style.artStyle(),style.version())));
        when(catalog.detail(style.name())).thenReturn(style);
        var entry = new ShortDramaSkillMarket(catalog).list().stream().filter(e->e.name().equals(style.name())).findFirst().orElseThrow();
        assertFalse(entry.enabled()); assertEquals("v2",entry.version()); assertEquals(List.of(file),entry.files());
    }
    @Test void missingPropsExcludeApprovedMediaFramesAndExistingTaskReceipts() {
        var asset = new org.ruoyi.domain.entity.shortdrama.ShortDramaVisualAsset();
        asset.setKind("prop");asset.setStatus("pending");assertTrue(ShortDramaVisualAssetService.needsPropImage(asset));
        asset.setImageUrl("approved.jpg");assertFalse(ShortDramaVisualAssetService.needsPropImage(asset));
        asset.setImageUrl("");asset.setPredictionId("accepted-task");assertFalse(ShortDramaVisualAssetService.needsPropImage(asset));
        asset.setPredictionId("");asset.setStatus("waiting");assertFalse(ShortDramaVisualAssetService.needsPropImage(asset));
        asset.setStatus("pending");asset.setKind("shot_frame");assertFalse(ShortDramaVisualAssetService.needsPropImage(asset));
    }
    @Test void reviewedToneIsUsedWithoutRewritingTheScriptOrWorld() {
        var script = new org.ruoyi.domain.entity.shortdrama.ShortDramaScript();
        script.setTone("暖色、克制的写实情感剧");script.setWorldbuilding("小县城修理铺");script.setScriptText("已有对白");
        var context = ShortDramaScriptPreparation.worldContext(script);
        assertTrue(context.contains(script.getTone()));assertTrue(context.contains(script.getWorldbuilding()));
        assertEquals("已有对白",script.getScriptText());
    }
}
