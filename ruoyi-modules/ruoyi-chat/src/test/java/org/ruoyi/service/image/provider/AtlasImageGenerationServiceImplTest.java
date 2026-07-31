package org.ruoyi.service.image.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class AtlasImageGenerationServiceImplTest {

    @Test
    void shouldUseGptImageSupportedLandscapeSize() {
        String model = "openai/gpt-image-2/text-to-image";

        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "3:2"));
        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "1152*768"));
        assertEquals("1536x1024", AtlasImageGenerationServiceImpl.resolveSize(model, "16:9"));
    }

    @Test
    void shouldUseGptImageSupportedPortraitAndSquareSizes() {
        String model = "openai/gpt-image-2/edit";

        assertEquals("1024x1536", AtlasImageGenerationServiceImpl.resolveSize(model, "9:16"));
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