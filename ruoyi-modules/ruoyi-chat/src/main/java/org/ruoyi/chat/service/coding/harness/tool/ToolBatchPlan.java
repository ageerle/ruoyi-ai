package org.ruoyi.chat.service.coding.harness.tool;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable policy plan preserving the original input slot order. */
public record ToolBatchPlan(
    List<ToolBatchSlot> slots,
    List<ToolExecutionGroup> executionGroups
) {

    public ToolBatchPlan {
        slots = slots == null ? List.of() : List.copyOf(slots);
        executionGroups = executionGroups == null ? List.of() : List.copyOf(executionGroups);

        Set<String> slotCallIds = new HashSet<>();
        Set<String> executableCallIds = new HashSet<>();
        for (ToolBatchSlot slot : slots) {
            if (!slotCallIds.add(slot.invocation().callId())) {
                throw new IllegalArgumentException("Duplicate tool callId: " + slot.invocation().callId());
            }
            if (slot.executable()) {
                executableCallIds.add(slot.invocation().callId());
            }
        }

        Set<String> groupedCallIds = new HashSet<>();
        for (ToolExecutionGroup group : executionGroups) {
            for (ToolInvocation invocation : group.invocations()) {
                if (!groupedCallIds.add(invocation.callId())) {
                    throw new IllegalArgumentException("Tool call appears in multiple execution groups: "
                        + invocation.callId());
                }
            }
        }
        if (!groupedCallIds.equals(executableCallIds)) {
            throw new IllegalArgumentException("Execution groups must contain every allowed call exactly once");
        }
    }
}
