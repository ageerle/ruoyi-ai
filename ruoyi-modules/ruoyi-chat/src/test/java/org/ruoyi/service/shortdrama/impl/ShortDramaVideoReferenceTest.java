package org.ruoyi.service.shortdrama.impl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaVideoReferenceTest {
    private static ShortDramaVideoReferenceService.Clip clip(double duration) {
        return new ShortDramaVideoReferenceService.Clip("00000000-0000-0000-0000-000000000001", "白模.mp4", duration, 1280, 720, 24, 5_000_000, "hash");
    }
    @Test void enforcesPerModelReferenceBudgetsAndNeverDowngrades() {
        ShortDramaVideoReferenceService.validateModel("bytedance/seedance-2.0-mini/reference-to-video", List.of(clip(5),clip(5),clip(5)));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.validateModel("bytedance/seedance-2.0-mini/reference-to-video", List.of(clip(8),clip(8))));
        ShortDramaVideoReferenceService.validateModel("bytedance/seedance-2.5/reference-to-video", List.of(clip(26)));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.validateModel("bytedance/seedance-2.0-mini/text-to-video", List.of(clip(5))));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.validateModel("unknown/reference-to-video", List.of(clip(5))));
    }
    @Test void rejectsInvisibleOrUnsupportedVideoProfiles() {
        ShortDramaVideoReferenceService.validateClip(clip(12), "h264");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.validateClip(clip(33), "h264"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.validateClip(clip(12), "vp9"));
        var tiny = new ShortDramaVideoReferenceService.Clip("id", "tiny.mp4", 12, 240, 180, 24, 100, "hash");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.validateClip(tiny, "h264"));
    }
    @Test void referencePathsCannotEscapeTheirProject() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.checkedId("../other-project/video"));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceService.checkedId("1-1-1-1-1"));
        assertEquals(clip(5).id(), ShortDramaVideoReferenceService.checkedId(clip(5).id()));
    }
}
