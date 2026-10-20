package org.ruoyi.service.video.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.video.VideoContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AtlasSeedanceVoicePayloadTest {
    private VideoContext context(String modelName, String prompt, List<String> audios) {
        var model = new ChatModelVo();
        model.setModelName(modelName);
        return VideoContext.builder().chatModelVo(model).prompt(prompt)
            .referenceAudios(audios).generateAudio(true).resolution("720p").build();
    }

    @Test void productionRoleDirectionUsesProviderTokensWithoutReorderingVoicesOrChangingSavedText() {
        String direction = "阿青问：‘现在是哪一年？’\n【角色音色】\n阿青对应@音频1；阿白对应@音频2。仅参考音色，按本镜对白发声，不朗读样音；画外声不驱动画内口型。";
        var context = context("bytedance/seedance-2.5/reference-to-video", direction,
            List.of("https://example.com/qing.mp3", "https://example.com/bai.mp3"));
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context);
        assertEquals(direction.replace("@音频1", "@Audio1").replace("@音频2", "@Audio2"),
            payload.path("prompt").asText());
        assertEquals("https://example.com/qing.mp3", payload.path("reference_audios").get(0).asText());
        assertEquals("https://example.com/bai.mp3", payload.path("reference_audios").get(1).asText());
        assertTrue(payload.path("generate_audio").asBoolean());
        assertEquals(direction, context.getPrompt());
        assertFalse(payload.has("voice_id"));
        assertFalse(payload.has("seed"));
    }

    @Test void audioOnlyReferenceSelectsReferenceModeWithoutRequiringAnImage() {
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(context(
            "bytedance/seedance-2.5/reference-to-video", "依照@audio1对白安排口型。",
            List.of("https://example.com/dialogue.wav")));
        assertEquals("reference", payload.path("omni_reference_task_type").asText());
        assertEquals("依照@Audio1对白安排口型。", payload.path("prompt").asText());
        assertFalse(payload.has("reference_images"));
        assertFalse(payload.has("image_url"));
        assertFalse(payload.has("duration"));
    }

    @Test void textAndFirstFrameModelsRejectUnsupportedAudioBeforeSubmitting() {
        for (String name : List.of("bytedance/seedance-2.5/text-to-video", "bytedance/seedance-2.5/image-to-video")) {
            var request = context(name, "阿青对应@音频1", List.of("https://example.com/voice.mp3"));
            var error = assertThrows(IllegalArgumentException.class,
                () -> AtlasVideoGenerationServiceImpl.buildPayload(request));
            assertTrue(error.getMessage().contains("reference-to-video"));
        }
    }

    @Test void legacyAndMixedCaseReferencesKeepTheirOriginalIndices() {
        var request = context("bytedance/seedance-2.5/reference-to-video",
            "@图片1、@IMAGE2、@image3；@音频10、@AUDIO2、@Audio1；@视频1、@video2。普通对白保留。",
            java.util.Collections.nCopies(10, "https://example.com/voice.mp3"));
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(request);
        assertEquals("@Image1、@Image2、@Image3；@Audio10、@Audio2、@Audio1；@Video1、@Video2。普通对白保留。",
            payload.path("prompt").asText());
        assertEquals(10, payload.path("reference_audios").size());
        request.setReferenceAudios(java.util.Collections.nCopies(11, "https://example.com/voice.mp3"));
        assertThrows(IllegalArgumentException.class, () -> AtlasVideoGenerationServiceImpl.buildPayload(request));
    }

    @Test void otherModelsKeepTheirExistingReferenceSyntax() {
        var request = context("bytedance/seedance-2.0-mini/reference-to-video",
            "阿青对应@音频1，@audio1。", List.of("https://example.com/voice.mp3"));
        request.setReferenceImages(List.of("https://example.com/frame.png"));
        var payload = AtlasVideoGenerationServiceImpl.buildPayload(request);
        assertEquals(request.getPrompt(), payload.path("prompt").asText());
        assertFalse(payload.has("omni_reference_task_type"));
    }
}
