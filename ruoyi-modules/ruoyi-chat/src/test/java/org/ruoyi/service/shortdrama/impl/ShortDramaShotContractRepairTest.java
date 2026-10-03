package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaShotContractRepairTest {
    private final ObjectMapper json = new ObjectMapper();
    private List<ShortDramaServiceImpl.StoryboardPanelData> plan() throws Exception {
        var design = json.readTree("""
            {"focus":"右手拉索与翼面回正","viewer_gain":"理解操作引发的回正", "framing":"同侧中景", "axis":"左侧伴飞向右",
             "movement":"固定伴飞", "motivation":"同框呈现因果", "transition":"opening", "cut_in":"平稳悬挂", "cut_out":"侧风开始",
             "state_in":{"姿态":"悬挂"},"state_delta":{}}
            """);
        var first = new ShortDramaServiceImpl.StoryboardPanelData(); first.setPanelNumber(1); first.setDescription("原规划动作"); first.setDuration(6); first.setShotDesign(design);
        var next = new ShortDramaServiceImpl.StoryboardPanelData(); next.setPanelNumber(2); next.setDescription("拉索回正"); next.setDuration(5); next.setShotDesign(design.deepCopy());
        var b = (com.fasterxml.jackson.databind.node.ObjectNode) next.getShotDesign(); b.put("transition", "continuous"); b.put("cut_out", "布翼回正"); b.remove("state_in"); b.putObject("state_delta").put("视线", "翼面");
        return List.of(first, next);
    }
    @Test void repairsMissingInitialAnchorWithoutChangingPlanOrOriginalDraft() throws Exception {
        var original = plan(); var before = json.valueToTree(original);
        var repaired = ShortDramaShotContractRepair.apply(original, json.readTree("""
            [{"panel_number":1,"state_in":{"姿态":"悬挂","视线":"前方"},"state_delta":{}},
             {"panel_number":2,"state_delta":{"视线":"翼面"}}]
            """));
        assertEquals(before, json.valueToTree(original));
        assertEquals(6, repaired.get(0).getDuration()); assertEquals("拉索回正", repaired.get(1).getDescription());
        assertEquals("左侧伴飞向右", repaired.get(1).getShotDesign().path("axis").asText());
        assertEquals(repaired.get(0).getShotDesign().path("state_out"), repaired.get(1).getShotDesign().path("state_in"));
        assertEquals("侧风开始", repaired.get(1).getShotDesign().path("cut_in").asText());
        assertDoesNotThrow(() -> ShortDramaShotDesign.validate(repaired));
    }
    @Test void rejectsChangingNarrativeAndDroppingOrRenumberingShots() throws Exception {
        var original = plan();
        for (String invalid : List.of("[{\"panel_number\":1,\"description\":\"改剧情\",\"state_delta\":{}},{\"panel_number\":2,\"state_delta\":{}}]",
            "[{\"panel_number\":1,\"state_delta\":{}}]", "[{\"panel_number\":1,\"state_delta\":{}},{\"panel_number\":9,\"state_delta\":{}}]")) {
            var patches = json.readTree(invalid);
            assertThrows(ShortDramaShotDesign.InvalidDesign.class, () -> ShortDramaShotContractRepair.apply(original, patches));
        }
    }
    @Test void rejectsExplicitContinuousStateOverrideAndUnresolvedUnknownAnchor() throws Exception {
        var original = plan();
        var override = json.readTree("""
            [{"panel_number":1,"state_in":{"姿态":"悬挂"},"state_delta":{}},
             {"panel_number":2,"state_in":{"姿态":"坐下"},"state_delta":{}}]
            """);
        assertThrows(ShortDramaShotDesign.InvalidDesign.class, () -> ShortDramaShotContractRepair.apply(original, override));
        var unresolved = json.readTree("""
            [{"panel_number":1,"state_in":{"姿态":"悬挂"},"state_delta":{}},
             {"panel_number":2,"state_delta":{"视线":"翼面"}}]
            """);
        assertThrows(ShortDramaShotDesign.InvalidDesign.class, () -> ShortDramaShotContractRepair.apply(original, unresolved));
    }
    @Test void enumeratesAllLaterStateKeysBeforeRepairInsteadOfOnlyFirstFailure() throws Exception {
        var original = plan();
        var delta = (com.fasterxml.jackson.databind.node.ObjectNode) original.get(1).getShotDesign().path("state_delta");
        delta.put("神态", "释然");
        assertEquals(java.util.Set.of("姿态", "视线", "神态"), ShortDramaShotContractRepair.segmentKeys(original).get(1));
        var prompt = ShortDramaShotContractRepair.prompt("原文", original, "缺少视线");
        assertTrue(prompt.contains("完整准确锚点清单")); assertTrue(prompt.contains("神态"));
        assertTrue(prompt.contains("\"state_in\":{\"姿态\":\"悬挂\",\"视线\":\"\",\"神态\":\"\"}"));
    }

    @Test void identifiesExactlyWhichShotAndInitialAnchorTheRepairOmitted() throws Exception {
        var original = plan();
        var patches = json.readTree("""
            [{"panel_number":1,"state_in":{"姿态":"悬挂"},"state_delta":{}},
             {"panel_number":2,"state_delta":{"视线":"翼面"}}]
            """);
        var error = assertThrows(ShortDramaShotDesign.InvalidDesign.class, () -> ShortDramaShotContractRepair.apply(original, patches));
        assertTrue(error.getMessage().contains("镜头1"));
        assertTrue(error.getMessage().contains("缺少锚点[视线]"));
        assertTrue(error.getMessage().contains("多出锚点[]"));
    }
}
