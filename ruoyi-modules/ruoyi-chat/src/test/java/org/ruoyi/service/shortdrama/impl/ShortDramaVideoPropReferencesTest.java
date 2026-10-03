package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev") class ShortDramaVideoPropReferencesTest {
    @Test void indexUsesActualOrderIncludingOptionalFrame() {
        var props=List.of(new ShortDramaVideoPropReferences.Binding("撞木","log"));
        assertTrue(ShortDramaVideoPropReferences.direction(props,List.of("frame","actor","space","log")).contains("撞木对应@image4"));
        assertTrue(ShortDramaVideoPropReferences.direction(props,List.of("actor","space","log")).contains("撞木对应@image3"));
    }
    @Test void rejectsUnattachedPropRatherThanInventingIndex() {
        var props=List.of(new ShortDramaVideoPropReferences.Binding("炮","gun"));
        assertThrows(IllegalArgumentException.class,()->ShortDramaVideoPropReferences.direction(props,List.of("actor")));
    }
    @Test void noPropsDoesNotAddDirectionsAndNamesStayDistinct() {
        assertEquals("",ShortDramaVideoPropReferences.direction(List.of(),List.of("actor")));
        var direction=ShortDramaVideoPropReferences.direction(List.of(new ShortDramaVideoPropReferences.Binding("炮","gun"),new ShortDramaVideoPropReferences.Binding("撞木","log")),List.of("actor","gun","log"));
        assertTrue(direction.contains("炮对应@image2"));assertTrue(direction.contains("撞木对应@image3"));
    }
}
