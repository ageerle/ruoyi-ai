package org.ruoyi.service.media;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class AtlasPredictionServiceTest {
    @Test
    void keepsAudioTypeAndMimeThroughPolling() throws Exception {
        var service = new AtlasPredictionService();
        assertEquals("audio", AtlasPredictionService.mediaType("audio"));
        assertEquals("image", AtlasPredictionService.mediaType("image"));
        assertEquals("video", AtlasPredictionService.mediaType("video"));
        var pending = service.toResponse("{\"data\":{\"id\":\"task-123\",\"status\":\"processing\"}}", "audio");
        assertEquals("audio", pending.getType());
        assertEquals("audio/mpeg", pending.getMimeType());
        assertEquals("task-123", pending.getId());
        for (String[] sample : new String[][]{{"mp3", "audio/mpeg"}, {"wav", "audio/wav"}, {"ogg", "audio/ogg"}}) {
            var complete = service.toResponse("{\"data\":{\"status\":\"completed\",\"outputs\":[\"https://example.com/result."
                + sample[0] + "?token=test\"]}}", "audio");
            assertEquals(sample[1], complete.getMimeType());
        }
    }
}
