package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.service.media.AtlasMediaSupport;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class ShortDramaDetailDeltaTest {
    @BeforeAll static void mapper() { ShortDramaVideoPromptReviewTest.provideProductionJsonMapper(); }
    private ShortDramaServiceImpl.StoryboardPanelData panel(int number) {
        var panel = new ShortDramaServiceImpl.StoryboardPanelData(); panel.setPanelNumber(number); panel.setDuration(6);
        panel.setSourceText(ShortDramaVideoPromptReviewTest.SOURCE); panel.setStartState("原首态"); panel.setEndState("原末态");
        panel.setShotDesign(AtlasMediaSupport.OBJECT_MAPPER.createObjectNode().put("focus", "文书")); return panel;
    }
    private String delta(int number, String prompt) throws Exception {
        return AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(List.of(Map.of("panel_number", number, "video_prompt", prompt, "image_prompt", "原首态：朱承晏站在桌前")));
    }
    @Test void detailKeepsOriginalMetadataWithoutRejectingParaphrases() throws Exception {
        var panel=panel(1); String response=delta(1,ShortDramaVideoPromptReviewTest.PROMPT);
        var result=ShortDramaServiceImpl.reviewStoryboardDetailResponse(response,List.of(panel)).get(0);
        assertEquals("原首态",result.getStartState()); assertEquals("原末态",result.getEndState());
        assertEquals("原首态", ShortDramaServiceImpl.reviewStoryboardDetailResponse(response.replace("\"panel_number\":1", "\"start_state\":\"坐下\",\"panel_number\":1"),List.of(panel)).get(0).getStartState());
    }
    @Test void repairOnlyInvalidNumbersRetainsSuccessfulRowsAndFullBatchReview() throws Exception {
        var panels=List.of(panel(1),panel(2));
        String first=delta(1,ShortDramaVideoPromptReviewTest.PROMPT), bad=delta(2,"");
        String combined=first.substring(0,first.length()-1)+","+bad.substring(1);
        var invalid=ShortDramaServiceImpl.detailRepairNumbers(combined,panels); assertEquals(Set.of(2),invalid);
        String merged=ShortDramaServiceImpl.mergeDetailRepair(combined,delta(2,ShortDramaVideoPromptReviewTest.PROMPT),invalid);
        assertEquals(2,ShortDramaServiceImpl.reviewStoryboardDetailResponse(merged,panels).size());
        assertEquals(AtlasMediaSupport.OBJECT_MAPPER.readTree(first).get(0),AtlasMediaSupport.OBJECT_MAPPER.readTree(merged).get(0));
    }
    @Test void compactBindingOnlyRemovesIdenticalDefaultBodyAndKeepsEditedVersions() {
        String rule=ShortDramaDirectorSkills.read("cinematic-storyboard"); String body=rule.substring(rule.indexOf('\n')+1).strip();
        String selected="[selected-skill:cinematic-storyboard@version]"+body+"references retained";
        String compact=ShortDramaDirectorSkills.compactSelected(selected,"cinematic-storyboard");
        assertTrue(compact.contains("@version")); assertTrue(compact.contains("references retained")); assertFalse(compact.contains(body));
        String edited=selected.replace("# 专业分镜", "# 后台已改版"); assertEquals(edited,ShortDramaDirectorSkills.compactSelected(edited,"cinematic-storyboard"));
    }
}
