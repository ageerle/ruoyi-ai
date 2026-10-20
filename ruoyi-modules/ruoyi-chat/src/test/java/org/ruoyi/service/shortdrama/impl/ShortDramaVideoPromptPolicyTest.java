package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaVideoPromptPolicyTest {
    @BeforeAll static void provideProductionJsonMapper() {
        ShortDramaVideoPromptReviewTest.provideProductionJsonMapper();
    }

    @Test void longMultiActorDirectionIsPreservedInDraftReviewAndExecution() throws Exception {
        String necessary = "铺内固定全景，王大头在过道训斥，伙计右手持刀、左手按肉，刘三留在门外；日光从铺口入，刀落砧板声近，街声远。".repeat(20);
        var panel = panel(1);
        String response = new ObjectMapper().writeValueAsString(List.of(
            Map.of("panel_number", 1, "video_prompt", necessary, "image_prompt", "铺内三人首态")));
        assertEquals(necessary, ShortDramaServiceImpl.reviewStoryboardDetailResponse(response, List.of(panel)).get(0).getVideoPrompt());
        assertEquals(necessary, ShortDramaVideoPromptPolicy.execution(necessary, ""));
    }

    @Test void originalDialogueAndManualPromptAreNeverTruncated() {
        String line = "原话".repeat(190), source = "将军：「" + line + "」";
        String prompt = "将军低声说「" + line + "」，士卒停步。";
        assertEquals(prompt, ShortDramaVideoPromptPolicy.execution(prompt, source));
        String manual = "人工导演稿".repeat(400);
        assertEquals(manual, ShortDramaVideoPromptPolicy.execution(manual, ""));
    }

    @Test void actualChanganShotsRetainLongStagingMultiSpeakerInterruptionsAndDisguiseStages() throws Exception {
        try (var input = getClass().getResourceAsStream("/short-drama/changan-prompt-regression.json")) {
            assertNotNull(input);
            var rows = new ObjectMapper().readTree(input);
            assertEquals(3, rows.size());
            for (var row : rows) {
                String original = row.path("videoPrompt").asText(), source = row.path("sourceText").asText();
                assertEquals(original, ShortDramaVideoPromptPolicy.execution(original, source));
                var panel = panel(row.path("sceneNo").asInt()); panel.setSourceText(source);
                String response = new ObjectMapper().writeValueAsString(List.of(Map.of(
                    "panel_number", panel.getPanelNumber(), "video_prompt", original, "image_prompt", "保留已有首态")));
                assertEquals(original, ShortDramaServiceImpl.reviewStoryboardDetailResponse(response, List.of(panel)).get(0).getVideoPrompt());
            }
            assertTrue(ShortDramaVideoPromptPolicy.characters(rows.get(0).path("videoPrompt").asText()) > 1000);
        }
    }

    @Test void alreadyEmbeddedSpeechAndActionAreNotAppendedAgain() {
        String prompt = "中景固定，将军抬手喊「停！」，士卒收步；天光与风声，结束在军旗停住的画面。";
        assertEquals(prompt, ShortDramaVideoPromptPolicy.execution(prompt, "将军：「停！」\n士卒收步。"));
        String result = ShortDramaVideoPromptPolicy.execution("将军抬手，士卒收步。", "将军：「停！」\n不应重复的动作资料。");
        assertTrue(result.contains("将军：「停！」"));
        assertFalse(result.contains("不应重复的动作资料"));
    }

    @Test void speechSplitAroundReactionIsRecognizedWithoutRepeatingTheOriginalLine() {
        String prompt = "王大头低声说「小翠，」，咬牙后接着说「我就知她安分不了，先生说的在理。」";
        String source = "王大头：「小翠，我就知她安分不了，先生说的在理。」";
        assertEquals(prompt, ShortDramaVideoPromptPolicy.execution(prompt, source));
    }

    @Test void selectedSkillsRemainAvailableForContentBasedSynthesisWithoutFixedBudgets() {
        String input = ShortDramaServiceImpl.detailInput(List.of(panel(1)));
        assertFalse(input.contains("video_prompt_max_chars"));
        String art = "[selected-skill:ink@v1]水墨淡彩，保留渗墨。\n[selected-skill:acting@v2]克制表演，动作接镜。";
        String drafting = ShortDramaServiceImpl.buildStoryboardDetailPrompt(input, "甲", "门厅", art, "16:9");
        assertTrue(drafting.contains(art));
        assertTrue(drafting.contains("审美skill提炼"));
        assertTrue(drafting.contains("导演skill提炼"));
        assertTrue(drafting.contains("效果镜保留触发、展开、破坏或余波"));
        assertFalse(drafting.contains("最多500字符"));
        assertFalse(drafting.contains("最多300字符"));
        assertFalse(drafting.contains("最多40字符"));
    }

    @Test void detailingReceivesOriginalSceneTimeAndOnlyEachPanelsSelectedCostume() {
        var assets = ShortDramaSceneCheckpointTest.assets();
        var p = ShortDramaSceneCheckpointTest.panel(); p.setPanelNumber(7);
        var chosen = assets.appearances().get(0); chosen.setDescription("蓝色官服，本镜已选");
        var unused = new org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance();
        unused.setId(999L); unused.setCharacterId(chosen.getCharacterId()); unused.setAppearanceIndex(9);
        unused.setChangeReason("未来候选"); unused.setDescription("红色盔甲未选"); assets.appearances().add(unused);
        var library = ShortDramaServiceImpl.panelCharacterLibraries(assets, List.of(p));
        assertTrue(library.get("appearances").contains("镜头7"));
        assertTrue(library.get("appearances").contains("蓝色官服，本镜已选"));
        assertFalse(library.get("appearances").contains("红色盔甲未选"));
        p.setSourceSceneContext(ShortDramaServiceImpl.sourceSceneContext(List.of("一 夜雨卧房\n主角惊醒。", "二 翌晨\n雨已经停了。"), 1));
        String input = ShortDramaServiceImpl.detailInput(List.of(p));
        assertTrue(input.contains("source_scene_context")); assertTrue(input.contains("雨已经停了"));
        String prompt = ShortDramaServiceImpl.buildStoryboardDetailPrompt(input, "演员", "场景母图：雨夜", "realistic", "16:9");
        assertTrue(prompt.contains("不能把原文冬夜改成白天"));
        assertTrue(prompt.contains("当前空间为"));
    }

    @Test void multiReferenceDisguiseKeepsBothCostumeBindingsAndTheirAppearanceTiming() throws Exception {
        var service = org.mockito.Mockito.mock(ShortDramaServiceImpl.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        var project = new org.ruoyi.domain.entity.shortdrama.ShortDramaProject(); project.setId(7L);
        var projects = org.mockito.Mockito.mock(org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper.class);
        org.mockito.Mockito.when(projects.selectById(7L)).thenReturn(project);
        var catalog = org.mockito.Mockito.mock(ShortDramaSkillCatalog.class);
        var assets = org.mockito.Mockito.mock(ShortDramaVisualAssetService.class);
        org.mockito.Mockito.when(assets.readyFrame(99L)).thenReturn("frame");
        var actors = org.mockito.Mockito.mock(org.ruoyi.mapper.shortdrama.ShortDramaCharacterMapper.class);
        var appearances = org.mockito.Mockito.mock(org.ruoyi.mapper.shortdrama.ShortDramaCharacterAppearanceMapper.class);
        var liu = new org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter(); liu.setId(1L);
        var zhang = new org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter(); zhang.setId(2L);
        org.mockito.Mockito.when(actors.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(liu), List.of(zhang), List.of(zhang));
        var liuArt = appearance(3, "liu"); var middle = appearance(1, "middle"); var young = appearance(2, "young");
        org.mockito.Mockito.when(appearances.selectList(org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(liuArt), List.of(middle, young), List.of(middle, young));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "projectMapper", projects);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "skillCatalog", catalog);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "visualAssets", assets);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "characterMapper", actors);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "characterAppearanceMapper", appearances);
        var shot = new org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard(); shot.setId(99L); shot.setProjectId(7L);
        shot.setVideoPrompt("张磊擦去中年伪装，镜尾显出年轻原貌，刘三在左侧坐着。");
        shot.setContinuityJson("{\"video_reference_mode\":\"multi_shot\"}");
        shot.setCharactersJson("[{\"name\":\"刘三\",\"appearance\":\"3\"},{\"name\":\"张磊\",\"appearance\":\"1\",\"slot\":\"揭面前\"},{\"name\":\"张磊\",\"appearance\":\"2\",\"slot\":\"擦去伪装后的镜尾\"}]");
        var method = ShortDramaServiceImpl.class.getDeclaredMethod("buildEnrichedVideoPrompt",
            org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard.class, List.class, String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, shot, List.of("frame", "liu", "middle", "young"), null);
        assertTrue(result.contains("张磊（形象1，揭面前）的身份与衣装对应@image3"));
        assertTrue(result.contains("张磊（形象2，擦去伪装后的镜尾）的身份与衣装对应@image4"));
        assertEquals(result.indexOf(shot.getVideoPrompt()), result.lastIndexOf(shot.getVideoPrompt()));
        assertFalse(result.contains("[skill:"));
    }

    private static org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance appearance(int index, String url) {
        var variant = new org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance();
        variant.setAppearanceIndex(index); variant.setReferenceImageUrl(url); return variant;
    }

    private static ShortDramaServiceImpl.StoryboardPanelData panel(int number) {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData();
        panel.setPanelNumber(number); panel.setSceneType("daily"); panel.setSourceText("");
        return panel;
    }
}
