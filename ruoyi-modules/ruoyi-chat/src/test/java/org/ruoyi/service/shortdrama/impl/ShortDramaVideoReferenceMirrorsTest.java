package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaVideoReferenceMirrorsTest {
    @Test void transportCopiesPreserveOriginalOrderingAndLeaveUnmappedAssets() {
        var original = List.of("https://source.test/actor.png", "https://source.test/set.png");
        var transported = ShortDramaVideoReferenceMirrors.apply("{\"video_reference_mirrors\":{\"https://source.test/actor.png\":\"https://provider.test/copy.png\"}}", original);
        assertEquals(List.of("https://provider.test/copy.png", original.get(1)), transported);
        assertEquals("https://source.test/actor.png", original.get(0));
        assertSame(original, ShortDramaVideoReferenceMirrors.apply("{}", original));
    }
    @Test void staleMappingsAndNonHttpTargetsCannotChangeAssetIdentity() {
        var original = List.of("https://source.test/current.png");
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceMirrors.apply(
            "{\"video_reference_mirrors\":{\"https://source.test/old.png\":\"https://provider.test/copy.png\"}}", original));
        assertThrows(IllegalArgumentException.class, () -> ShortDramaVideoReferenceMirrors.apply(
            "{\"video_reference_mirrors\":{\"https://source.test/current.png\":\"file:///secret.png\"}}", original));
    }
}
