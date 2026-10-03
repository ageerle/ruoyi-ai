package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaScriptProgressTest {
    @Test void contentAndThinkingHaveDifferentMilestonesAndSavingDoesNotPretendModelActivity() {
        var events = new ArrayList<ShortDramaScriptProgress.Snapshot>();
        var tracker = new ShortDramaScriptProgress(events::add);
        tracker.start(800);
        tracker.thinking(20); tracker.state("waiting");
        var thought = events.get(events.size() - 1);
        assertEquals(-1, thought.firstContentMs()); assertEquals(20, thought.thinkingChars());
        tracker.content(50); tracker.script(30); tracker.state("saving");
        var saving = events.get(events.size() - 1);
        assertEquals("saving", saving.state()); assertTrue(saving.firstContentMs() >= 0);
        assertTrue(saving.firstScriptMs() >= saving.firstContentMs());
        assertEquals(50, saving.contentChars()); assertEquals(30, saving.scriptChars());
        tracker.state("done");
        assertEquals(saving.lastActivityAt(), events.get(events.size() - 1).lastActivityAt());
    }
}
