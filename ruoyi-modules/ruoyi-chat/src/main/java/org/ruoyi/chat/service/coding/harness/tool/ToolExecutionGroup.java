package org.ruoyi.chat.service.coding.harness.tool;

import java.util.List;

/** Ordered execution wave; concurrent groups may execute their calls in parallel. */
public record ToolExecutionGroup(
    boolean concurrent,
    List<ToolInvocation> invocations
) {

    public ToolExecutionGroup {
        invocations = invocations == null ? List.of() : List.copyOf(invocations);
        if (invocations.isEmpty() || (!concurrent && invocations.size() != 1)) {
            throw new IllegalArgumentException("Invalid tool execution group");
        }
    }
}
