package org.ruoyi.chat.service.coding.harness.artifact;

import org.ruoyi.chat.service.coding.harness.tool.ToolCapability;
import org.ruoyi.chat.service.coding.harness.tool.ToolDescriptor;

import java.util.List;
import java.util.Set;

public final class HarnessArtifactToolDescriptors {

    private HarnessArtifactToolDescriptors() { }

    public static List<ToolDescriptor> all() {
        return List.of(
            new ToolDescriptor("read_artifact", Set.of(ToolCapability.READ), true,
                10_000, 4 * 1024, 512 * 1024, true,
                "Bounded read of a run-owned content-addressed artifact"),
            new ToolDescriptor("list_artifacts", Set.of(ToolCapability.READ), true,
                10_000, 4 * 1024, 256 * 1024, true,
                "Bounded owner/session artifact manifest discovery")
        );
    }
}
