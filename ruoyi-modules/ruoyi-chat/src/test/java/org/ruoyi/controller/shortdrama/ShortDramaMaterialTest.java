package org.ruoyi.controller.shortdrama;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class ShortDramaMaterialTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void appendMigratesLegacyWithoutLosingOriginalOrShotMetadata() throws Exception {
        ObjectNode c=(ObjectNode)json.readTree("{\"start_state\":\"desk\",\"source_media\":{\"url\":\"a\",\"label\":\"book\",\"usage\":\"0-2s\"}}");
        var items=ShortDramaMaterialController.items(c);
        items.add(json.createObjectNode().put("url","b"));
        items.add(json.createObjectNode().put("url","c"));
        ShortDramaMaterialController.setItems(c,items);
        assertEquals("desk",c.path("start_state").asText());
        assertEquals("a",c.at("/source_media/url").asText());
        assertEquals("0-2s",c.at("/source_media/items/0/usage").asText());
        assertEquals(3,c.at("/source_media/items").size());
    }
    @Test void removingPrimaryPromotesNextAndLastRemovalClearsMode() throws Exception {
        ObjectNode c=(ObjectNode)json.readTree("{\"source_media\":{\"url\":\"a\",\"items\":[{\"url\":\"a\"},{\"url\":\"b\"}]}}");
        var items=ShortDramaMaterialController.items(c);items.remove(0);
        ShortDramaMaterialController.setItems(c,items);
        assertEquals("b",c.at("/source_media/url").asText());
        items.remove(0);ShortDramaMaterialController.setItems(c,items);
        assertFalse(c.has("source_media"));
    }
}
