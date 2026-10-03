package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaTimingTest {
    @Test void screenshotRegressionEstimatedThirteenPointThreeSecondsDoesNotBlockElevenSeconds() {
        String timing = "{\"timing\":{\"action_seconds\":13.3,\"pause_seconds\":0,\"strict_fill\":true}}";
        assertEquals(13.3, ShortDramaTiming.requiredSeconds("", timing));
        assertFalse(ShortDramaTiming.issues(1, 11, "", timing).isEmpty());
        assertDoesNotThrow(() -> ShortDramaTiming.validate(1, 11, "", timing));
        assertDoesNotThrow(() -> ShortDramaTiming.validateSubmission(1, 11, "", timing));
        var panel = panel(11, "");
        try { panel.setTiming(new com.fasterxml.jackson.databind.ObjectMapper().readTree(timing).path("timing")); }
        catch (Exception e) { throw new AssertionError(e); }
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateScenePlan("外景 云梦泽", List.of(panel)));
        assertEquals(11, panel.getDuration());
    }
    @Test void strictFillAndMissingPacingNotesAreAdvisory() {
        String timing = "{\"timing\":{\"spoken_text\":\"快走\",\"action_seconds\":0,\"pause_seconds\":0,\"strict_fill\":true}}";
        assertFalse(ShortDramaTiming.issues(1, 12, "", timing).isEmpty());
        assertDoesNotThrow(() -> ShortDramaTiming.validate(1, 12, "", timing));
    }
    @Test void sceneTotalAndSegmentLengthAreNotAcceptanceLimits() {
        var panels = List.of(panel(3, ""), panel(25, ""));
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateScenePlan("外景 山谷（预计20秒）", panels, 4));
        assertFalse(ShortDramaServiceImpl.scenePlanIssues("外景 山谷（预计20秒）", panels).isEmpty());
        assertEquals(List.of(3, 25), panels.stream().map(ShortDramaServiceImpl.StoryboardPanelData::getDuration).toList());
    }
    @Test void reviewedImportUsesTheSameAdvisoryPolicy() {
        var panel = panel(2, "陈念：「我毕业，他答应来的。我等到学校关门。」");
        panel.setSceneNumber(1);
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateReviewedPlan(List.of("预计2秒"), List.of(panel)));
    }
    @Test void missingDialogueIsReportedForReviewWithoutDiscardingDraft() {
        var panel = panel(7, "「你好。」");
        String source = "预计7秒\n「你好。」「坐吧。」";
        assertTrue(ShortDramaServiceImpl.scenePlanIssues(source, List.of(panel)).stream().anyMatch(s -> s.contains("遗漏或乱序")));
        assertDoesNotThrow(() -> ShortDramaServiceImpl.validateScenePlan(source, List.of(panel)));
    }
    @Test void measuredSpeechStillProvidesAnEstimate() {
        assertEquals(9.5, ShortDramaTiming.requiredSeconds("", "{\"timing\":{\"spoken_text\":\"你好\",\"measured_audio_seconds\":6,\"action_seconds\":2,\"pause_seconds\":1.5}}"));
        assertEquals(2.8, ShortDramaTiming.requiredSeconds("「35」", "{}"));
    }
    @Test void numericDataAndJsonMustStillBeUsable() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.validate(1, 0, "", "{}"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.requiredSeconds("", "{\"timing\":{\"action_seconds\":-1}}"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaTiming.requiredSeconds("", "[]"));
        assertDoesNotThrow(() -> ShortDramaTiming.validate(1, 75, "", "{\"timing\":{\"speech_rate\":6}}"));
    }
    @Test void reviewedImportStillRequiresValidSceneIdentity() {
        var panel = panel(5, "整理工具"); panel.setSceneNumber(3);
        assertThrows(IllegalArgumentException.class, () -> ShortDramaServiceImpl.validateReviewedPlan(List.of("第一场"), List.of(panel)));
    }
    private static ShortDramaServiceImpl.StoryboardPanelData panel(int seconds, String source) {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData(); panel.setDuration(seconds); panel.setSourceText(source); return panel;
    }
}
