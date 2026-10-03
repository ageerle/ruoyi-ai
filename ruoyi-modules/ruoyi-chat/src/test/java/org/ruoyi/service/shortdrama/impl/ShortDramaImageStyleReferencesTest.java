package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev") class ShortDramaImageStyleReferencesTest {
    @Test void keepsIdentityFirstAndStyleOrdered() {
        assertEquals(List.of("identity", "lighting", "texture"),
            ShortDramaImageStyleReferences.ordered("identity", List.of("lighting", "texture", "lighting")));
    }
    @Test void keepsLegacySingleReference() {
        assertEquals(List.of("identity"), ShortDramaImageStyleReferences.ordered("identity", List.of()));
        assertEquals(List.of(), ShortDramaImageStyleReferences.ordered(null, List.of()));
    }
    @Test void rejectsStyleWithoutIdentity() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaImageStyleReferences.ordered(null, List.of("style")));
    }
    @Test void rejectsAmbiguousDuplicateAndOversizedStyleSet() {
        assertThrows(IllegalArgumentException.class, () -> ShortDramaImageStyleReferences.ordered("same", List.of("same")));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaImageStyleReferences.ordered("id", List.of("a","b","c","d")));
    }
    @Test void distinguishesGeometryFromFaceIdentity() {
        assertTrue(ShortDramaImageStyleReferences.direction(true, 3).contains("门墙"));
        assertTrue(ShortDramaImageStyleReferences.direction(false, 3).contains("身份、年龄"));
        assertTrue(ShortDramaImageStyleReferences.direction(false, 3).contains("不复制后者的人脸"));
    }
}
