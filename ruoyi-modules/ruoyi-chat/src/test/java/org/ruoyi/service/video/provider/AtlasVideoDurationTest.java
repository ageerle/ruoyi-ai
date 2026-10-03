package org.ruoyi.service.video.provider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev") class AtlasVideoDurationTest {
    @Test void seedance25PayloadUsesNativeFieldsAndOrderedReferences() {
        var model=new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();
        model.setModelName("bytedance/seedance-2.5/reference-to-video");
        var context=org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model)
            .prompt("@image1保持作坊，@audio1只参考声线").size("16:9").seconds(3)
            .referenceImages(java.util.List.of("https://example.com/frame.png"))
            .referenceAudios(java.util.List.of("https://example.com/voice.mp3"))
            .generateAudio(true).returnLastFrame(true).build();
        var payload=AtlasVideoGenerationServiceImpl.buildPayload(context);
        assertEquals("16:9",payload.get("ratio").asText());
        assertFalse(payload.has("size"));
        assertEquals(3,payload.get("duration").asInt());
        assertEquals("720p",payload.get("resolution").asText());
        assertEquals("reference",payload.get("omni_reference_task_type").asText());
        assertEquals("https://example.com/frame.png",payload.get("reference_images").get(0).asText());
        assertTrue(payload.get("prompt").asText().contains("@Image1"));
        assertFalse(payload.get("prompt").asText().contains("前3秒"));
        assertTrue(payload.get("generate_audio").isBoolean());
        context.setReferenceImages(java.util.Collections.nCopies(31,"https://example.com/frame.png"));
        assertThrows(IllegalArgumentException.class,()->AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
    @Test void seedance25SupportsLongTakesWithoutChangingEditorialBudget() {
        String model="bytedance/seedance-2.5/reference-to-video";
        assertEquals(1,AtlasVideoGenerationServiceImpl.requestDuration(model,1));
        assertEquals(16,AtlasVideoGenerationServiceImpl.requestDuration(model,16));
        assertEquals(30,AtlasVideoGenerationServiceImpl.requestDuration(model,30));
        assertEquals(-1,AtlasVideoGenerationServiceImpl.requestDuration(model,-1));
        assertEquals(31,AtlasVideoGenerationServiceImpl.requestDuration(model,31));
        assertThrows(IllegalArgumentException.class,()->AtlasVideoGenerationServiceImpl.requestDuration(model,0));
    }
    @Test void requestedSuperResolutionIsNotSilentlyDowngraded() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();
        model.setModelName("bytedance/seedance-2.5/reference-to-video");
        var context = org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model)
            .prompt("Opening shot").seconds(16).resolution("1080p-sr").build();
        assertEquals("1080p-sr", AtlasVideoGenerationServiceImpl.buildPayload(context).path("resolution").asText());
        context.setResolution("1080psr");
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }

    @Test void unspecifiedDurationIsAbsentFromActualProviderPayload() {
        for(String name:java.util.List.of("bytedance/seedance-2.0-mini/reference-to-video","bytedance/seedance-2.5/reference-to-video")) {
            var model=new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();model.setModelName(name);
            var context=org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model).prompt("完整执行剧情").build();
            var payload=AtlasVideoGenerationServiceImpl.buildPayload(context);
            assertFalse(payload.has("duration"));assertFalse(payload.has("seconds"));
            assertEquals("完整执行剧情",payload.path("prompt").asText());
            context.setSeconds(12);
            assertEquals(12,AtlasVideoGenerationServiceImpl.buildPayload(context).path("duration").asInt());
            context.setSeconds(null);
            assertFalse(AtlasVideoGenerationServiceImpl.buildPayload(context).has("duration"));
        }
    }
    @Test void shortEditorialShotsUseSupportedProviderDuration() {
        String model="bytedance/seedance-2.0-mini/reference-to-video";
        assertEquals(3,AtlasVideoGenerationServiceImpl.requestDuration(model,3));
        assertEquals(9,AtlasVideoGenerationServiceImpl.requestDuration(model,9));
        assertEquals(-1,AtlasVideoGenerationServiceImpl.requestDuration(model,-1));
        assertEquals(16,AtlasVideoGenerationServiceImpl.requestDuration(model,16));
        assertEquals(3,AtlasVideoGenerationServiceImpl.requestDuration("another-model",3));
    }
    @Test void miniPayloadSendsNative720pAndPreservesProductionParameters() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();
        model.setModelName("bytedance/seedance-2.0-mini/reference-to-video");
        var context = org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model)
            .prompt("@image1保持县衙，@audio1只参考声线").size("16:9").seconds(3).resolution("720p")
            .referenceImages(java.util.List.of("https://example.com/frame.png", "https://example.com/identity.png"))
            .referenceAudios(java.util.List.of("https://example.com/voice.mp3"))
            .generateAudio(true).returnLastFrame(true).build();
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context);
        assertEquals(model.getModelName(), payload.path("model").asText());
        assertEquals("720p", payload.path("resolution").asText());
        assertEquals("16:9", payload.path("ratio").asText());
        assertFalse(payload.has("size"));
        assertEquals(3, payload.path("duration").asInt());
        assertEquals("https://example.com/frame.png", payload.path("reference_images").get(0).asText());
        assertEquals("https://example.com/identity.png", payload.path("reference_images").get(1).asText());
        assertEquals("https://example.com/voice.mp3", payload.path("reference_audios").get(0).asText());
        assertTrue(payload.path("generate_audio").isBoolean());
        assertTrue(payload.path("generate_audio").asBoolean());
        assertTrue(payload.path("return_last_frame").isBoolean());
        assertTrue(payload.path("return_last_frame").asBoolean());
        assertTrue(payload.path("prompt").asText().contains("@image1"));
        assertFalse(payload.path("prompt").asText().contains("前3秒"));
        assertFalse(payload.has("omni_reference_task_type"));
        context.setResolution(null);
        assertEquals("720p", AtlasVideoGenerationServiceImpl.buildPayload(context).path("resolution").asText());
    }
    @Test void miniUsesCanonicalSuperResolutionValuesAndRejectsUnsupportedNative1080p() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();
        model.setModelName("bytedance/seedance-2.0-mini/reference-to-video");
        var context = org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model)
            .prompt("Opening shot").seconds(15).resolution("1080p-sr").build();
        assertEquals("1080p-SR", AtlasVideoGenerationServiceImpl.buildPayload(context).path("resolution").asText());
        context.setResolution("720p-SR");
        assertEquals("720p-SR", AtlasVideoGenerationServiceImpl.buildPayload(context).path("resolution").asText());
        context.setResolution("1440p-sr");
        assertEquals("1440p-SR", AtlasVideoGenerationServiceImpl.buildPayload(context).path("resolution").asText());
        context.setResolution("480p");
        assertEquals("480p", AtlasVideoGenerationServiceImpl.buildPayload(context).path("resolution").asText());
        context.setResolution("1080p");
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
        context.setResolution("720p-esr");
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
    @Test void miniReferenceCountLimitsAreCheckedBeforeProviderSubmission() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();
        model.setModelName("bytedance/seedance-2.0-mini/reference-to-video");
        var context = org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model)
            .prompt("Opening shot").seconds(4)
            .referenceImages(java.util.Collections.nCopies(9, "https://example.com/frame.png"))
            .referenceAudios(java.util.Collections.nCopies(3, "https://example.com/voice.mp3")).build();
        assertEquals(9, AtlasVideoGenerationServiceImpl.buildPayload(context).path("reference_images").size());
        assertEquals(3, AtlasVideoGenerationServiceImpl.buildPayload(context).path("reference_audios").size());
        context.setReferenceImages(java.util.Collections.nCopies(10, "https://example.com/frame.png"));
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
        context.setReferenceImages(java.util.List.of("https://example.com/frame.png"));
        context.setReferenceAudios(java.util.Collections.nCopies(4, "https://example.com/voice.mp3"));
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
    @Test void miniSingleImageFallbackUsesReferenceFieldAndAudioRequiresVisualReference() {
        var model = new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();
        model.setModelName("bytedance/seedance-2.0-mini/reference-to-video");
        var context = org.ruoyi.common.chat.entity.video.VideoContext.builder().chatModelVo(model)
            .prompt("Opening shot").seconds(4).imageUrl("https://example.com/frame.png")
            .referenceAudios(java.util.List.of("https://example.com/voice.mp3"))
            .generateAudio(false).returnLastFrame(false).build();
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context);
        assertEquals("https://example.com/frame.png", payload.path("reference_images").get(0).asText());
        assertFalse(payload.has("image_url"));
        assertFalse(payload.path("generate_audio").asBoolean());
        assertFalse(payload.path("return_last_frame").asBoolean());
        context.setImageUrl(null);
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(context));
    }
}
