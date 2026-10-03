package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaSceneBudgetTest {
    private static final String SCENE = "外景 修渠处（预计54秒）\n工匠：「可以试水了。」";
    @Test void repairPromptKeepsBudgetAdvisory() {
        String prompt = ShortDramaSceneBudget.repairPrompt(SCENE, List.of(ShortDramaSceneCheckpointTest.panel()), "原稿81秒");
        assertTrue(prompt.contains("剧本预计54秒")); assertTrue(prompt.contains("仅作制作参考")); assertFalse(prompt.contains("49至59秒"));
        assertTrue(prompt.contains("from_panels")); assertTrue(prompt.contains("原发言人")); assertTrue(prompt.contains("并行动作")); assertTrue(prompt.contains("不因公式估算差异强制拆镜"));
    }
    @Test void explicitDurationIsPreservedDespiteEstimatedSpeechDifference() {
        var panel = ShortDramaSceneCheckpointTest.panel();
        String safe = "[{\"from_panel\":1,\"source_text\":\"工匠：「可以试水了。」\",\"duration\":6,\"timing\":{\"spoken_text\":\"可以试水了\",\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1}}]";
        var fixed = ShortDramaSceneBudget.applyRepair(List.of(panel), safe); assertEquals(6, fixed.get(0).getDuration());
        assertEquals(1, ShortDramaSceneBudget.applyRepair(List.of(panel), safe.replace("\"duration\":6", "\"duration\":1")).get(0).getDuration());
        assertThrows(IllegalArgumentException.class, () -> ShortDramaSceneBudget.applyRepair(List.of(panel), safe.replace("\"duration\":6", "\"duration\":6.5")));
        assertDoesNotThrow(() -> ShortDramaSceneBudget.applyRepair(List.of(panel), safe.replace("\"speech_rate\":4", "\"speech_rate\":6")));
    }
    @Test void preservesSeveralShotsWithoutForcedTimingRepair() {
        var first = ShortDramaSceneCheckpointTest.panel();
        var second = ShortDramaSceneCheckpointTest.panel();
        String edit = "["
            + "{\"from_panel\":1,\"source_text\":\"工匠：「可以试水了。」\",\"duration\":2,\"timing\":{\"spoken_text\":\"可以试水了\",\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1}},"
            + "{\"from_panel\":2,\"source_text\":\"工匠：「可以试水了。」\",\"duration\":2,\"timing\":{\"spoken_text\":\"可以试水了\",\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1}}]";
        assertEquals(List.of(2, 2), ShortDramaSceneBudget.applyRepair(List.of(first, second), edit).stream().map(ShortDramaServiceImpl.StoryboardPanelData::getDuration).toList());
    }
    @Test void mergingConcurrentActionKeepsAllCastAndDoesNotDoubleCountAction() {
        var p1 = ShortDramaSceneCheckpointTest.panel(); var p2 = ShortDramaSceneCheckpointTest.panel();
        var listener = new ShortDramaServiceImpl.CharacterRef(); listener.setName("验收人"); listener.setAppearance("常服");
        p2.setCharacters(List.of(listener)); p2.setPresentCharacters(List.of("验收人"));
        String edit = "[{\"from_panels\":[1,2],\"source_text\":\"工匠：「可以试水了。」验收人同时观察出水。\",\"duration\":5,\"timing\":{\"spoken_text\":\"可以试水了\",\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1}}]";
        var fixed = ShortDramaSceneBudget.applyRepair(List.of(p1, p2), edit).get(0);
        assertEquals(2, fixed.getCharacters().size()); assertEquals(List.of("工匠", "验收人"), fixed.getPresentCharacters()); assertEquals(5, fixed.getDuration());
        p2.setLocation("另一处"); assertThrows(IllegalArgumentException.class, () -> ShortDramaSceneBudget.applyRepair(List.of(p1, p2), edit));
    }
    @Test void recordedSpeechAndDialogueRemainReviewReferences() {
        var p = ShortDramaSceneCheckpointTest.panel();
        String edit = "[{\"from_panel\":1,\"source_text\":\"工匠：「可以试水了。」\",\"duration\":3,\"timing\":{\"spoken_text\":\"\",\"speech_rate\":4,\"measured_audio_seconds\":5,\"action_seconds\":0,\"pause_seconds\":0}}]";
        assertDoesNotThrow(() -> ShortDramaSceneBudget.applyRepair(List.of(p), edit));
        var missing = ShortDramaSceneCheckpointTest.panel(); missing.setSourceText("工匠观察水渠");
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateScenePlan("外景（预计5秒）\n工匠：「可以试水了。」", List.of(missing)));
    }
    @Test void approvedMediaAndContinuityFieldsSurviveTimingRevision() {
        var p = ShortDramaSceneCheckpointTest.panel(); p.setImagePrompt("已审阅首帧指令"); p.setBridgeIn("原桥梁");
        String edit = "[{\"from_panel\":1,\"source_text\":\"工匠：「可以试水了。」\",\"timing\":{\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1}}]";
        var fixed = ShortDramaSceneBudget.applyRepair(List.of(p), edit).get(0);
        assertEquals("已审阅首帧指令", fixed.getImagePrompt()); assertEquals("原桥梁", fixed.getBridgeIn());
        assertEquals(5, p.getDuration()); assertEquals(3, fixed.getDuration());
    }
    @Test void legacyMinimumDoesNotRejectProductionEstimates() {
        var p = ShortDramaSceneCheckpointTest.panel();
        String edit = "[{\"from_panel\":1,\"source_text\":\"工匠：「可以试水了。」\",\"duration\":3,\"timing\":{\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1}}]";
        assertEquals(3, ShortDramaSceneBudget.applyRepair(List.of(p), edit).get(0).getDuration());
        assertEquals(3, ShortDramaSceneBudget.applyRepair(List.of(p), edit, 4).get(0).getDuration());
        assertTrue(ShortDramaSceneBudget.repairPrompt(SCENE, List.of(p), "原稿81秒", 4).contains("不强制最短整数秒或固定段长"));
    }

    @Test void mergingRetainsExplicitSourceStatesWhenModelOmitsTheContract() throws Exception {
        var mapper = org.ruoyi.service.media.AtlasMediaSupport.OBJECT_MAPPER;
        var first = ShortDramaSceneCheckpointTest.panel(); var second = ShortDramaSceneCheckpointTest.panel();
        first.setShotDesign(mapper.readTree("{\"state_in\":{\"古人.姿态\":\"悬挂\",\"右绳.张力\":\"绷紧\"},\"state_delta\":{\"古人.姿态\":\"左倾\",\"右绳.张力\":\"松弛\"}}"));
        second.setShotDesign(mapper.readTree("{\"state_delta\":{\"古人.姿态\":\"悬挂\",\"右绳.张力\":\"绷紧\"}}"));
        String edit = "[{\"from_panels\":[1,2],\"source_text\":\"工匠：「可以试水了。」\",\"segment_goal\":\"操作并确认结果\",\"duration\":6,\"timing\":{\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1},\"shot_design\":{\"transition\":\"opening\"}}]";
        var merged = ShortDramaSceneBudget.applyRepair(List.of(first, second), edit).get(0);
        assertEquals("悬挂", merged.getShotDesign().path("state_in").path("古人.姿态").asText());
        assertEquals("绷紧", merged.getShotDesign().path("state_delta").path("右绳.张力").asText());
        assertEquals("操作并确认结果", merged.getSegmentGoal());
        assertFalse(first.getShotDesign().has("transition"));
    }

    @Test void retainingKnownStatesDoesNotGuessMissingInitialValues() throws Exception {
        var mapper = org.ruoyi.service.media.AtlasMediaSupport.OBJECT_MAPPER;
        var first = ShortDramaSceneCheckpointTest.panel(); var second = ShortDramaSceneCheckpointTest.panel();
        first.setShotDesign(mapper.readTree("{\"state_in\":{\"古人.姿态\":\"悬挂\"},\"state_delta\":{}}"));
        second.setShotDesign(mapper.readTree("{\"state_delta\":{\"古人.视线\":\"前方\"}}"));
        String edit = "[{\"from_panels\":[1,2],\"source_text\":\"工匠：「可以试水了。」\",\"duration\":6,\"timing\":{\"speech_rate\":4,\"action_seconds\":0,\"pause_seconds\":1},\"shot_design\":{\"transition\":\"opening\"}}]";
        var design = ShortDramaSceneBudget.applyRepair(List.of(first, second), edit).get(0).getShotDesign();
        assertFalse(design.path("state_in").has("古人.视线"));
        assertEquals("前方", design.path("state_delta").path("古人.视线").asText());
    }
}
