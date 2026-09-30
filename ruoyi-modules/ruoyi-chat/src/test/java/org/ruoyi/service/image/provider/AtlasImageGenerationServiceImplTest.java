package org.ruoyi.service.image.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class AtlasImageGenerationServiceImplTest {
    @Test void sendsAllIdentityAndLocationReferencesInOrder() {
        var model=new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();model.setModelName("openai/gpt-image-2/edit");
        var context=org.ruoyi.common.chat.entity.image.ImageContext.builder().chatModelVo(model).referenceImages(java.util.List.of("person1","person2","location","prop")).build();
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        AtlasImageGenerationServiceImpl.applyReferenceImages(payload,context);
        assertEquals(4,payload.path("images").size());assertEquals("location",payload.path("images").get(2).asText());
    }

    @Test void supportsGptImage25NativeWidescreenAndSixteenReferences() {
        var model=new org.ruoyi.common.chat.domain.vo.chat.ChatModelVo();model.setModelName("openai/gpt-image-2.5-flare/edit");
        var refs=java.util.stream.IntStream.range(0,16).mapToObj(i->"reference-"+i).toList();
        var context=org.ruoyi.common.chat.entity.image.ImageContext.builder().chatModelVo(model).referenceImages(refs).build();
        var payload=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        AtlasImageGenerationServiceImpl.applyReferenceImages(payload,context);
        assertEquals(16,payload.path("images").size());
        assertEquals("1536x864",AtlasImageGenerationServiceImpl.resolveSize(model.getModelName(),"16:9"));
    }

    @Test
    void shouldUseGptImageSupportedLandscapeSize() {
        String model = "openai/gpt-image-2/text-to-image";

        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "3:2"));
        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "1152*768"));
        assertEquals("1536x864", AtlasImageGenerationServiceImpl.resolveSize(model, "16:9"));
    }

    @Test
    void shouldUseGptImageSupportedPortraitAndSquareSizes() {
        String model = "openai/gpt-image-2/edit";

        assertEquals("864x1536", AtlasImageGenerationServiceImpl.resolveSize(model, "9:16"));
        assertEquals("1024x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "1:1"));
    }

    @Test
    void shouldKeepLegacyAtlasSizeFormatForOtherModels() {
        String model = "microsoft/mai-image-2.5-flash/text-to-image";

        assertEquals("1152*768", AtlasImageGenerationServiceImpl.resolveSize(model, "3:2"));
        assertEquals("1360*768", AtlasImageGenerationServiceImpl.resolveSize(model, "16:9"));
    }

    @Test
    void shouldDetectGptImageModels() {
        assertTrue(AtlasImageGenerationServiceImpl.isGptImageModel("openai/gpt-image-2/text-to-image"));
        assertTrue(AtlasImageGenerationServiceImpl.isGptImageModel("openai/gpt-image-2/edit"));
        assertFalse(AtlasImageGenerationServiceImpl.isGptImageModel("microsoft/mai-image-2.5/edit"));
    }
}
