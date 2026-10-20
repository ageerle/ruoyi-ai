package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaMediaRepairTest {
    private ShortDramaStoryboard board(long id) {
        var b = new ShortDramaStoryboard(); b.setId(id); b.setProjectId(2L); b.setScriptId(3L);
        b.setLocationName("库房"); b.setCharactersJson("[{\"name\":\"周慎\",\"appearance\":\"1\"}]");
        return b;
    }
    @Test void repairNeedsAProducedSourceAndTheSameAssetBindings() {
        var source = board(1); source.setVideoId("paid-task"); source.setVideoUrl("https://example.test/video.mp4"); source.setVideoStatus("done");
        var candidate = board(4); candidate.setContinuityJson("{\"repair_source_board\":\"1\"}");
        assertTrue(ShortDramaMediaRepair.usesProducedAssets(candidate, source));
        candidate.setProjectId(8L); assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, source)); candidate.setProjectId(2L);
        candidate.setScriptId(9L); assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, source)); candidate.setScriptId(3L);
        candidate.setLocationName("庭院"); assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, source)); candidate.setLocationName("库房");
        candidate.setCharactersJson("[{\"name\":\"周慎\",\"appearance\":\"2\"}]"); assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, source)); candidate.setCharactersJson(source.getCharactersJson());
        source.setVideoStatus("generating"); assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, source)); source.setVideoStatus("done");
        source.setVideoUrl(""); assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, source));
        assertFalse(ShortDramaMediaRepair.usesProducedAssets(candidate, null));
        for (String json : new String[]{"{}", "bad", "{\"repair_source_board\":\"4\"}", "{\"repair_source_board\":-1}"}) {
            candidate.setContinuityJson(json); assertNull(ShortDramaMediaRepair.sourceId(candidate));
        }
    }
}
