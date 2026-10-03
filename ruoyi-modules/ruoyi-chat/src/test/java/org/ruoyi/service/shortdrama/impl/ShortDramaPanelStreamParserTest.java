package org.ruoyi.service.shortdrama.impl;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class ShortDramaPanelStreamParserTest {
    @Test void arbitraryChunkBoundariesNestedArraysEscapedQuotesAndUnicodeDoNotEmitIncompleteObjects() {
        var rows = new ArrayList<JsonNode>(); var parser = new ShortDramaPanelStreamParser(rows::add);
        String input = "```json\n[{\"panel_number\":1,\"description\":\"手持{竹}，\\\"风\\\"\",\"characters\":[{\"name\":\"匠人\"}]}";
        for (int i=0; i<input.length()-1; i++) parser.accept(input.substring(i,i+1));
        assertEquals(0, rows.size()); parser.accept(input.substring(input.length()-1));
        assertEquals(1, rows.size()); assertEquals("匠人", rows.get(0).path("characters").get(0).path("name").asText());
        parser.accept(",{\"panel_number\":2,\"description\":\"回正\"}]\n```"); assertEquals(2, rows.size());
    }
    @Test void malformedAndNestedObjectsCannotMasqueradeAsCompletedPanels() {
        var rows = new ArrayList<JsonNode>(); var parser = new ShortDramaPanelStreamParser(rows::add);
        parser.accept("[{\"description\":\"not a panel\",\"nested\":{\"panel_number\":9}},{\"panel_number\":2, bad}]");
        assertTrue(rows.isEmpty());
    }
    @Test void planningEnvelopeStreamsRealPanelsAndKeepsOutlineSeparate() {
        var rows = new ArrayList<JsonNode>(); var parser = new ShortDramaPanelStreamParser(rows::add);
        parser.accept("{\"scene_plan\":{\"segment_outline\":[{\"panel_number\":99,\"camera_beats\":[\"切到手部\"]}]},\"panels\":[{\"panel_number\":1,\"characters\":[{\"name\":\"古人\"}]");
        assertTrue(rows.isEmpty());
        parser.accept("},{\"panel_number\":2,\"description\":\"回正\"}]}");
        assertEquals(2, rows.size()); assertEquals(1, rows.get(0).path("panel_number").asInt());
        assertEquals(2, rows.get(1).path("panel_number").asInt());
    }
}
