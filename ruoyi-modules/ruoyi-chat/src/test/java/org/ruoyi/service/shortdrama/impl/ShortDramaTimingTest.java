package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaTimingTest {
    @Test void emptyEditableSpeechDoesNotEraseDialogueFromSource() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.validate(17, 2,
            "陈念：「六月十二，换电容，三十五元，未收。」", "{\"timing\":{\"spoken_text\":\"\"}}"));
    }
    @Test void dialogueCannotBeSqueezedIntoTwoSeconds() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.validate(17, 2,
            "陈念：「六月十二，换电容，三十五元，未收。」", "{}"));
    }
    @Test void actualAudioReplacesEstimatedSpeechAndAddsExclusiveActionAndPause() {
        assertEquals(9.5, ShortDramaTiming.requiredSeconds("", """
            {"timing":{"spoken_text":"你好","measured_audio_seconds":6,"action_seconds":2,"pause_seconds":1.5}}
            """));
    }
    @Test void silentActionStillRequiresItsOwnBudget() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.validate(1, 5, "", """
            {"timing":{"spoken_text":"","action_seconds":7,"pause_seconds":1}}
            """));
    }
    @Test void negativeBudgetsAndNonObjectMetadataAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.requiredSeconds("", "{\"timing\":{\"action_seconds\":-1}}"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.requiredSeconds("", "[]"));
    }
    @Test void digitsAreNotTreatedAsSilentPunctuation() {
        assertEquals(2.8, ShortDramaTiming.requiredSeconds("「35」", "{}"));
    }
    @Test void sceneTotalCannotHideAnOverfullDialogueShot() {
        var a=new ShortDramaServiceImpl.StoryboardPanelData();
        a.setDuration(3); a.setSourceText("陈念：「我毕业，他答应来的。我等到学校关门。」");
        var b=new ShortDramaServiceImpl.StoryboardPanelData(); b.setDuration(7); b.setSourceText("");
        var panels=java.util.List.of(a,b);
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateSceneDuration("第1场 预计10秒",panels));
        var error=assertThrows(IllegalStateException.class, () -> ShortDramaServiceImpl.validateScenePlan("第1场 预计10秒",panels));
        assertTrue(error.getMessage().contains("镜头1内容至少"));
    }
    @Test void naturallyBudgetedDialoguePassesPlanningGate() {
        var a=new ShortDramaServiceImpl.StoryboardPanelData();
        a.setDuration(7); a.setSourceText("陈念：「我毕业，他答应来的。我等到学校关门。」");
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateScenePlan("第1场 预计7秒",java.util.List.of(a)));
    }
    @Test void timingRepairSplitsSpeechAndComputesDurationsWithoutChangingOriginal() {
        var original=new ShortDramaServiceImpl.StoryboardPanelData();
        original.setLocation("客厅"); original.setDuration(9); original.setSourceText("「你好。坐吧。」");
        var repaired=ShortDramaServiceImpl.applyTimingRepair(java.util.List.of(original), """
          [{"from_panel":1,"source_text":"「你好。」","timing":{"spoken_text":"你好","speech_rate":4,"action_seconds":0,"pause_seconds":1}},
           {"from_panel":1,"source_text":"「坐吧。」","timing":{"spoken_text":"坐吧","speech_rate":4,"action_seconds":2,"pause_seconds":0}}]
          """);
        assertEquals(2,repaired.size()); assertEquals(2,repaired.get(0).getDuration());
        assertEquals(3,repaired.get(1).getDuration()); assertEquals("客厅",repaired.get(1).getLocation());
        assertEquals(9,original.getDuration());
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateScenePlan("预计5秒\n「你好。坐吧。」",repaired));
    }
    @Test void planningRejectsOmittedDialogueEvenWhenTimeFits() {
        var p=new ShortDramaServiceImpl.StoryboardPanelData();p.setDuration(7);p.setSourceText("「你好。」");
        assertTrue(assertThrows(IllegalStateException.class,()->ShortDramaServiceImpl.validateScenePlan(
            "预计7秒\n「你好。」「坐吧。」",java.util.List.of(p))).getMessage().contains("遗漏或乱序"));
    }
    @Test void reviewedPlanCannotOmitOrInventScenes() {
        var p=new ShortDramaServiceImpl.StoryboardPanelData();p.setDuration(5);p.setSceneNumber(1);p.setSourceText("整理工具");
        assertThrows(IllegalArgumentException.class,()->ShortDramaServiceImpl.validateReviewedPlan(java.util.List.of("预计5秒","预计5秒"),java.util.List.of(p)));
        p.setSceneNumber(3);
        assertThrows(IllegalArgumentException.class,()->ShortDramaServiceImpl.validateReviewedPlan(java.util.List.of("预计5秒"),java.util.List.of(p)));
    }
    @Test void reviewedPlanUsesTheSameTimingAndDialogueGate() {
        var p=new ShortDramaServiceImpl.StoryboardPanelData();p.setDuration(2);p.setSceneNumber(1);p.setSourceText("「我毕业，他答应来的。我等到学校关门。」");
        assertThrows(IllegalStateException.class,()->ShortDramaServiceImpl.validateReviewedPlan(java.util.List.of("预计2秒"),java.util.List.of(p)));
    }
}
