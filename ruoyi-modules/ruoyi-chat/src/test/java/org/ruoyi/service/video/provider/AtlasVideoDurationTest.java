package org.ruoyi.service.video.provider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev") class AtlasVideoDurationTest {
    @Test void shortEditorialShotsUseSupportedProviderDuration() {
        String model="bytedance/seedance-2.0-mini/reference-to-video";
        assertEquals(4,AtlasVideoGenerationServiceImpl.requestDuration(model,3));
        assertEquals(9,AtlasVideoGenerationServiceImpl.requestDuration(model,9));
        assertEquals(-1,AtlasVideoGenerationServiceImpl.requestDuration(model,-1));
        assertThrows(IllegalArgumentException.class,()->AtlasVideoGenerationServiceImpl.requestDuration(model,16));
        assertEquals(3,AtlasVideoGenerationServiceImpl.requestDuration("another-model",3));
    }
}
