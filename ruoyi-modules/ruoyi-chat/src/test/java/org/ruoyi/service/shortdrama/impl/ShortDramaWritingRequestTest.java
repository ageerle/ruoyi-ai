package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaWritingRequestTest {
    @Test void shortIdeasAndScriptsDisableThinkingForBothSeedAliases() {
        for (String model : java.util.List.of("bytedance/doubao-seed-2.1-pro-260628", "doubao-seed-2-1-pro-260628")) {
            assertEquals("none", ShortDramaWritingRequest.forText(model, "古人乘竹布风筝翱翔").getReasoningEffort());
            assertEquals("none", ShortDramaWritingRequest.forText(model, "字".repeat(2000)).getReasoningEffort());
            assertEquals("low", ShortDramaWritingRequest.forText(model, "字".repeat(2001)).getReasoningEffort());
        }
    }
    @Test void unrelatedProvidersRetainTheirExistingDefaults() {
        assertNull(ShortDramaWritingRequest.forText("other-model", "故事").getReasoningEffort());
        assertNull(ShortDramaWritingRequest.forText(null, null).getReasoningEffort());
    }
}
