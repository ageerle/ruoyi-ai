package org.ruoyi.service.coding.harness.modelruntime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class HarnessModelPolicyTest {
    @Test
    void fixedSeed21UsesDoubaoRoutingWithoutFuzzyAliases() {
        assertTrue(HarnessModelPolicy.isDoubao("bytedance/doubao-seed-2.1-pro-260628"));
        assertTrue(HarnessModelPolicy.isDoubao("doubao-seed-2.1-pro-260628"));
        assertFalse(HarnessModelPolicy.isAutomatic("bytedance/doubao-seed-2.1-pro-260628"));
        assertTrue(HarnessModelPolicy.supportsThinkingLevel("bytedance/doubao-seed-2.1-pro-260628"));
        assertFalse(HarnessModelPolicy.isDoubao("bytedance/doubao-seed-2.1-pro-unknown"));
        assertFalse(HarnessModelPolicy.isDoubao("deepseek-v4-flash"));
        assertTrue(HarnessModelPolicy.isDoubao("bytedance/doubao-seed-evolving"));
    }
}
