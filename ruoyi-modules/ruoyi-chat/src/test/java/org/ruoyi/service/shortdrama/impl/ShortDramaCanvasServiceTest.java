package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaCanvasServiceTest {
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();

    @Test void storesARealDocumentAndRejectsStaleWritesWithoutOverwriting() throws Exception {
        var owners = mock(ShortDramaSoundService.class);
        var service = new ShortDramaCanvasService(owners, root);
        var doc = json.readTree("{\"schema\":1,\"nodes\":[],\"edges\":[]}");
        assertEquals(0, service.read(1L, 2L).revision());
        var saved = service.save(1L, 2L, new ShortDramaCanvasService.Canvas(0, doc));
        assertEquals(1, saved.revision());
        assertEquals(doc, new ShortDramaCanvasService(owners, root).read(1L, 2L).document());
        assertThrows(IllegalArgumentException.class, () -> service.save(1L, 2L, new ShortDramaCanvasService.Canvas(0, doc)));
        assertEquals(1, service.read(1L, 2L).revision());
        verify(owners, atLeastOnce()).owner(1L, 2L);
    }

    @Test void ownershipFailurePreventsReadsAndWrites() throws Exception {
        var owners = mock(ShortDramaSoundService.class);
        doThrow(new IllegalArgumentException("无项目权限")).when(owners).owner(1L, 3L);
        var service = new ShortDramaCanvasService(owners, root);
        var doc = json.readTree("{\"schema\":1,\"nodes\":[],\"edges\":[]}");
        assertThrows(IllegalArgumentException.class, () -> service.read(1L, 3L));
        assertThrows(IllegalArgumentException.class, () -> service.save(1L, 3L, new ShortDramaCanvasService.Canvas(0, doc)));
        assertNull(service.read(1L, 2L).document());
    }

    @Test void cyclicAndDanglingGraphsCannotBePersisted() throws Exception {
        var doc = json.readTree("""
            {"schema":1,"nodes":[
              {"id":"a","kind":"video","position":{"x":0,"y":0},"data":{}},
              {"id":"b","kind":"video","position":{"x":300,"y":0},"data":{}}],
             "edges":[{"id":"ab","source":"a","target":"b"},{"id":"ba","source":"b","target":"a"}]}
            """);
        assertThrows(IllegalArgumentException.class, () -> ShortDramaCanvasService.validate(doc));
        ((com.fasterxml.jackson.databind.node.ObjectNode)doc.path("edges").get(1)).put("source", "missing");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaCanvasService.validate(doc));
    }
}
