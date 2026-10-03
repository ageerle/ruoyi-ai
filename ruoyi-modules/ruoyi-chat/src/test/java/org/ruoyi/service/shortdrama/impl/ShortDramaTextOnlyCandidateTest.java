package org.ruoyi.service.shortdrama.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev") class ShortDramaTextOnlyCandidateTest {
    @Test void referencesAreOmittedOnlyForTheExplicitSupportedCandidateMode() throws Exception {
        var json = new ObjectMapper();
        var optedIn = json.readTree("{\"keyframe_reference_mode\":\"text_only_candidate\"}");
        assertTrue(ShortDramaVisualAssetService.textOnlyCandidate("bytedance/seedream-v4.7/text-to-image", optedIn));
        assertFalse(ShortDramaVisualAssetService.textOnlyCandidate("bytedance/seedream-v4.7/text-to-image", json.readTree("{}")));
        assertFalse(ShortDramaVisualAssetService.textOnlyCandidate("bytedance/seedream-v4.7/text-to-image", null));
        assertFalse(ShortDramaVisualAssetService.textOnlyCandidate("openai/gpt-image-2/edit", optedIn));
        assertFalse(ShortDramaVisualAssetService.textOnlyCandidate("bytedance/seedream-v4.7/text-to-image", json.readTree("{\"keyframe_reference_mode\":\"text_only\"}")));
    }
}
