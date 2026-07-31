package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class ShortDramaServiceImplStoryboardMergeTest {

    @Test
    void mergesPhaseOutputsByPanelNumberInsteadOfArrayIndex() {
        ShortDramaServiceImpl.StoryboardPanelData panel1 = panel(1);
        ShortDramaServiceImpl.StoryboardPanelData panel2 = panel(2);
        List<ShortDramaServiceImpl.StoryboardPanelData> panels = List.of(panel1, panel2);
        JsonNode rule1 = photographyRule(1, "暖色");
        JsonNode rule2 = photographyRule(2, "冷色");
        ShortDramaServiceImpl.ActingDirectionResult acting1 = actingDirection(1, "抬头");
        ShortDramaServiceImpl.ActingDirectionResult acting2 = actingDirection(2, "转身");

        List<ShortDramaServiceImpl.StoryboardPanelData> merged =
            ShortDramaServiceImpl.mergePanelsWithRules(
                panels,
                List.of(rule2, rule1),
                List.of(acting2, acting1));

        assertSame(panels, merged);
        assertEquals(rule1.toString(), panel1.getPhotographyRules());
        assertEquals(rule2.toString(), panel2.getPhotographyRules());
        assertEquals(acting1.getCharacters().toString(), panel1.getActingNotes());
        assertEquals(acting2.getCharacters().toString(), panel2.getActingNotes());
    }

    @Test
    void rejectsMergeWhenPhotographyRuleIsMissing() {
        ShortDramaServiceImpl.StoryboardPanelData panel = panel(1);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> ShortDramaServiceImpl.mergePanelsWithRules(
                List.of(panel),
                List.of(),
                List.of(actingDirection(1, "抬头"))));

        assertTrue(error.getMessage().contains("缺少摄影规则"));
        assertNull(panel.getPhotographyRules());
        assertNull(panel.getActingNotes());
    }

    @Test
    void rejectsMergeWhenActingDirectionIsMissingWithoutPartialChanges() {
        ShortDramaServiceImpl.StoryboardPanelData panel = panel(1);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> ShortDramaServiceImpl.mergePanelsWithRules(
                List.of(panel),
                List.of(photographyRule(1, "暖色")),
                List.of()));

        assertTrue(error.getMessage().contains("缺少表演指导"));
        assertNull(panel.getPhotographyRules());
        assertNull(panel.getActingNotes());
    }

    private static ShortDramaServiceImpl.StoryboardPanelData panel(int panelNumber) {
        ShortDramaServiceImpl.StoryboardPanelData panel = new ShortDramaServiceImpl.StoryboardPanelData();
        panel.setPanelNumber(panelNumber);
        panel.setDescription("镜头 " + panelNumber);
        return panel;
    }

    private static JsonNode photographyRule(int panelNumber, String colorTone) {
        ObjectNode rule = JsonNodeFactory.instance.objectNode();
        rule.put("panel_number", panelNumber);
        rule.put("color_tone", colorTone);
        return rule;
    }

    private static ShortDramaServiceImpl.ActingDirectionResult actingDirection(int panelNumber, String actingText) {
        ObjectNode note = JsonNodeFactory.instance.objectNode();
        note.put("name", "角色" + panelNumber);
        note.put("acting", actingText);
        ShortDramaServiceImpl.ActingDirectionResult result = new ShortDramaServiceImpl.ActingDirectionResult();
        result.setPanelNumber(panelNumber);
        result.setCharacters(JsonNodeFactory.instance.arrayNode().add(note));
        return result;
    }
}
