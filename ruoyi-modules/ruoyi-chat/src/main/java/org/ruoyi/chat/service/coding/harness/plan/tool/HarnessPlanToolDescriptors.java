package org.ruoyi.chat.service.coding.harness.plan.tool;

import org.ruoyi.chat.service.coding.harness.tool.ToolCapability;
import org.ruoyi.chat.service.coding.harness.tool.ToolDescriptor;

import java.util.List;
import java.util.Set;

public final class HarnessPlanToolDescriptors {

    private static final Set<ToolCapability> CONTROL = Set.of(ToolCapability.CONTROL);

    private HarnessPlanToolDescriptors() { }

    public static List<ToolDescriptor> all() {
        return List.of(
            descriptor("plan_create", 128 * 1024),
            descriptor("plan_step", 32 * 1024),
            descriptor("plan_record_tool_evidence", 16 * 1024),
            descriptor("plan_verify", 32 * 1024));
    }

    private static ToolDescriptor descriptor(String name, int inputBytes) {
        return new ToolDescriptor(name, CONTROL, false, 10_000, inputBytes, 256 * 1024,
            true, "Durable plan control-plane mutation; never a workspace write");
    }
}
