package org.ruoyi.service.video.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.video.VideoContext;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AtlasH3VideoTest {
    private VideoContext context() {
        var model = new ChatModelVo(); model.setModelName("minimax/h3-developer/image-to-video");
        return VideoContext.builder().chatModelVo(model).prompt("骑兵沿长蛇队列推进")
            .seconds(15).size("16:9").resolution("4k-sr")
            .referenceImages(java.util.List.of("https://example.com/army.png"))
            .generateAudio(true).returnLastFrame(true).build();
    }
    @Test void exactH3SchemaPreserves15SecondsAnd4kWithoutSeedanceFields() {
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context());
        assertEquals(java.util.Set.of("model", "prompt", "image", "duration", "resolution", "ratio", "prompt_expansion"),
            java.util.stream.StreamSupport.stream(((Iterable<String>)payload::fieldNames).spliterator(), false).collect(java.util.stream.Collectors.toSet()));
        assertEquals(15, payload.path("duration").asInt());
        assertEquals("4k-sr", payload.path("resolution").asText());
        assertEquals("adaptive", payload.path("ratio").asText());
        assertEquals("https://example.com/army.png", payload.path("image").asText());
        assertFalse(payload.path("prompt_expansion").asBoolean());
    }
    @Test void unsupportedResolutionDurationOrReferencesFailBeforePaidDispatch() {
        var context = context(); context.setResolution("4k-esr");
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
        context.setResolution("4k-sr"); context.setSeconds(16);
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
        context.setSeconds(15); context.setReferenceImages(java.util.List.of());
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
        context.setReferenceImages(java.util.List.of("first", "second"));
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
        context.setReferenceImages(java.util.List.of("first")); context.setReferenceVideos(java.util.List.of("motion"));
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
}
