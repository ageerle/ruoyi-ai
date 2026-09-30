package org.ruoyi.service.shortdrama.composition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev") class CompositionPlannedDurationTest {
    @Test void generatedPaddingDoesNotLengthenTimelineAndShortSourcesAreRejected() {
        var source=new CompositionSource(Path.of("clip.mp4"),3d,3d);
        assertEquals(3d,source.forTimeline(new MediaInfo(4d,1280,720,true,0)).durationSeconds());
        assertThrows(IllegalArgumentException.class,()->source.forTimeline(new MediaInfo(2d,1280,720,true,0)));
        assertEquals(4d,new CompositionSource(Path.of("clip.mp4"),3d).forTimeline(new MediaInfo(4d,1280,720,true,0)).durationSeconds());
    }
}
