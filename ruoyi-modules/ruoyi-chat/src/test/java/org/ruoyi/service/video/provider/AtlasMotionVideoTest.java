package org.ruoyi.service.video.provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.video.VideoContext;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AtlasMotionVideoTest {
    @Test void motionOnlyReferenceAndSoundReachMiniWithoutAnAppearanceImage() {
        var model = new ChatModelVo(); model.setModelName("bytedance/seedance-2.0-mini/reference-to-video");
        var context = VideoContext.builder().chatModelVo(model).prompt("参考@video1步法，真人写实")
            .referenceVideos(List.of("https://example.com/motion.mp4"))
            .referenceAudios(List.of("https://example.com/voice.mp3")).resolution("720p").build();
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context);
        assertEquals("https://example.com/motion.mp4", payload.path("reference_videos").get(0).asText());
        assertFalse(payload.has("reference_images")); assertFalse(payload.has("duration"));
        assertTrue(payload.path("prompt").asText().contains("@video1"));
        context.setReferenceVideos(List.of());
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
    @Test void fullModelUsesExplicitReferenceModeAndCanonicalVideoToken() {
        var model = new ChatModelVo(); model.setModelName("bytedance/seedance-2.5/reference-to-video");
        var context = VideoContext.builder().chatModelVo(model).prompt("复制@video1动作")
            .referenceVideos(List.of("https://example.com/motion.mp4")).build();
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context);
        assertEquals("reference", payload.path("omni_reference_task_type").asText());
        assertTrue(payload.path("prompt").asText().contains("@Video1"));
        model.setModelName("bytedance/seedance-2.0-mini/text-to-video");
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
}
