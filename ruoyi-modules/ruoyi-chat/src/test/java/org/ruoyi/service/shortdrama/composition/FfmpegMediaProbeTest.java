package org.ruoyi.service.shortdrama.composition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class FfmpegMediaProbeTest {

    @Test
    void parsesVideoAudioDurationAndRotation() {
        String json = """
            {
              "streams": [
                {
                  "index": 0,
                  "codec_type": "video",
                  "width": 600,
                  "height": 600,
                  "duration": "5.200",
                  "disposition": {"attached_pic": 1}
                },
                {
                  "index": 2,
                  "codec_type": "video",
                  "width": 1920,
                  "height": 1080,
                  "duration": "5.125",
                  "disposition": {"attached_pic": 0},
                  "side_data_list": [{"rotation": -90}]
                },
                {"index": 3, "codec_type": "audio", "duration": "5.100"}
              ],
              "format": {"duration": "5.200"}
            }
            """;

        MediaInfo info = FfmpegMediaProbe.parseProbeOutput(json, null);

        assertEquals(5.125, info.durationSeconds(), 0.000001);
        assertEquals(1080, info.width());
        assertEquals(1920, info.height());
        assertTrue(info.hasAudio());
        assertEquals(2, info.videoStreamIndex());
    }

    @Test
    void usesFallbackDurationAndRecognizesMissingAudio() {
        String json = """
            {
              "streams": [
                {
                  "index": 1,
                  "codec_type": "video",
                  "width": 720,
                  "height": 1280,
                  "duration": "N/A",
                  "disposition": {"attached_pic": 0}
                }
              ],
              "format": {"duration": "N/A"}
            }
            """;

        MediaInfo info = FfmpegMediaProbe.parseProbeOutput(json, 6.0);

        assertEquals(6, info.durationSeconds(), 0.000001);
        assertFalse(info.hasAudio());
        assertEquals(1, info.videoStreamIndex());
    }

    @Test
    void rejectsInputsWithoutVideoStreams() {
        String json = """
            {"streams": [{"codec_type": "audio"}], "format": {"duration": "3"}}
            """;

        assertThrows(MediaProcessException.class, () -> FfmpegMediaProbe.parseProbeOutput(json, null));
    }

    @Test
    void rejectsVideoStreamsWithoutAnAbsoluteIndex() {
        String json = """
            {
              "streams": [{
                "codec_type": "video",
                "width": 720,
                "height": 1280,
                "duration": "3",
                "disposition": {"attached_pic": 0}
              }]
            }
            """;

        MediaProcessException error = assertThrows(
            MediaProcessException.class,
            () -> FfmpegMediaProbe.parseProbeOutput(json, null)
        );
        assertTrue(error.getMessage().contains("stream index"));
    }
}
