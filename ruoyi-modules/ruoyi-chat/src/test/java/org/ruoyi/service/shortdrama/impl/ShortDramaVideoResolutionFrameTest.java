package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaVideoResolutionFrameTest {
    private ShortDramaStoryboard shot(String continuity) {
        var shot = new ShortDramaStoryboard();
        shot.setContinuityJson(continuity);
        shot.setImagePrompt("Approved frame");
        shot.setCharactersJson("[]");
        shot.setLocationName("Courtyard");
        return shot;
    }
    @Test void changingDeliveryResolutionCanKeepAnApprovedFrame() {
        assertTrue(ShortDramaVisualAssetService.onlyVideoResolutionChanged(
            shot("{\"start_state\":\"standing\",\"video_resolution\":\"720p\"}"),
            shot("{\"video_resolution\":\"1080p-sr\",\"start_state\":\"standing\"}")));
    }
    @Test void changedActionOrIdentityMustNotReuseTheFrame() {
        var before = shot("{\"start_state\":\"standing\"}");
        var after = shot("{\"start_state\":\"sitting\",\"video_resolution\":\"1080p-sr\"}");
        assertFalse(ShortDramaVisualAssetService.onlyVideoResolutionChanged(before, after));
        after.setContinuityJson("{\"start_state\":\"standing\",\"video_resolution\":\"1080p-sr\"}");
        after.setCharactersJson("[{\"name\":\"Different person\"}]");
        assertFalse(ShortDramaVisualAssetService.onlyVideoResolutionChanged(before, after));
    }

    @Test void settingAndClearingSecondsPreserveTheApprovedFrame() {
        var before=shot("{\"start_state\":\"standing\"}");
        var after=shot("{\"start_state\":\"standing\",\"video_seconds\":12}");
        assertTrue(ShortDramaVisualAssetService.onlyVideoResolutionChanged(before,after));
        assertTrue(ShortDramaVisualAssetService.onlyVideoResolutionChanged(after,before));
        after.setContinuityJson("{\"start_state\":\"sitting\",\"video_seconds\":12}");
        assertFalse(ShortDramaVisualAssetService.onlyVideoResolutionChanged(before,after));
    }

    @Test void optionalVideoReferencesKeepTheCandidateFrame() {
        var before=shot("{\"start_state\":\"standing\"}");
        var after=shot("{\"start_state\":\"standing\",\"video_use_start_frame\":false,\"video_reference_include_location\":false}");
        assertTrue(ShortDramaVisualAssetService.onlyVideoResolutionChanged(before,after));
        assertTrue(ShortDramaVisualAssetService.onlyVideoResolutionChanged(after,before));
    }
}
