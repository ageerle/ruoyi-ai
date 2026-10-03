package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaPlanResponseTest {
    @Test void sceneOutlineIsNotMistakenForSavedPanels() throws Exception {
        var panels = ShortDramaPlanResponse.parse("{\"scene_plan\":{\"estimated_seconds\":12,\"segment_outline\":[{\"goal\":\"拉绳回正\",\"camera_beats\":[\"中景\",\"手部特写\"]}]},\"panels\":[{\"panel_number\":1,\"duration\":12,\"source_text\":\"古人拉绳，布翼回正。\"}]}");
        assertEquals(1, panels.size()); assertEquals(12, panels.get(0).getDuration());
        assertEquals("古人拉绳，布翼回正。", panels.get(0).getSourceText());
    }
    @Test void advisoryOutlineDoesNotRejectADifferentActualShotCount() throws Exception {
        var panels = ShortDramaPlanResponse.parse("{\"scene_plan\":{\"estimated_seconds\":12,\"segment_outline\":[{\"goal\":\"原计划\"}]},\"panels\":[{\"panel_number\":1,\"duration\":4},{\"panel_number\":2,\"duration\":4},{\"panel_number\":3,\"duration\":4}]}");
        assertEquals(3, panels.size());
    }
    @Test void legacyArraysKeepBindingsAndPhysicalContracts() throws Exception {
        var panels = ShortDramaPlanResponse.parse("[{\"panel_number\":1,\"characters\":[{\"name\":\"古人\",\"appearance\":\"常服\"}],\"shot_design\":{\"state_in\":{\"古人.姿态\":\"悬挂\"}},\"extra_annotation\":\"不覆盖已有字段\"}]");
        assertEquals("古人", panels.get(0).getCharacters().get(0).getName());
        assertEquals("悬挂", panels.get(0).getShotDesign().path("state_in").path("古人.姿态").asText());
    }
    @Test void summaryAloneCannotBeUsedAsAProductionStoryboard() throws Exception {
        assertNull(ShortDramaPlanResponse.parse("{\"scene_plan\":{\"estimated_seconds\":12}}"));
    }
}
