package org.ruoyi.chat.service.coding.harness.tool;

import org.ruoyi.chat.service.coding.harness.model.HarnessApprovalPolicy;
import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Plans every call independently so denied or approval-gated calls do not discard legal siblings. */
public final class ToolBatchPlanner {

    private final ToolPolicyEngine policyEngine;

    public ToolBatchPlanner(ToolPolicyEngine policyEngine) {
        if (policyEngine == null) {
            throw new IllegalArgumentException("Tool policy engine is required");
        }
        this.policyEngine = policyEngine;
    }

    public ToolBatchPlan plan(List<ToolInvocation> invocations,
                              HarnessPermissionMode permissionMode,
                              ToolPolicyContract contract) {
        return plan(invocations, permissionMode, HarnessApprovalPolicy.ON_REQUEST, contract);
    }

    public ToolBatchPlan plan(List<ToolInvocation> invocations,
                              HarnessPermissionMode permissionMode,
                              HarnessApprovalPolicy approvalPolicy,
                              ToolPolicyContract contract) {
        List<ToolInvocation> calls = invocations == null ? List.of() : List.copyOf(invocations);
        assertUniqueCallIds(calls);

        List<ToolBatchSlot> slots = new ArrayList<>(calls.size());
        for (ToolInvocation invocation : calls) {
            ToolPolicyEvaluation evaluation = policyEngine.evaluate(invocation, permissionMode,
                approvalPolicy, contract);
            ToolDescriptor descriptor = policyEngine.descriptor(invocation.toolName()).orElse(null);
            ToolOutcome outcome = switch (evaluation.decision()) {
                case ALLOW -> ToolOutcome.pending(invocation);
                case ASK -> ToolOutcome.approvalRequired(invocation, evaluation);
                case DENY -> ToolOutcome.denied(invocation, evaluation);
            };
            slots.add(new ToolBatchSlot(invocation, descriptor, evaluation, outcome));
        }
        return new ToolBatchPlan(slots, buildExecutionGroups(slots));
    }

    public ToolBatchPlan plan(List<ToolInvocation> invocations,
                              HarnessPermissionMode permissionMode) {
        return plan(invocations, permissionMode, null);
    }

    private static List<ToolExecutionGroup> buildExecutionGroups(List<ToolBatchSlot> slots) {
        List<ToolExecutionGroup> groups = new ArrayList<>();
        List<ToolInvocation> concurrent = new ArrayList<>();
        for (ToolBatchSlot slot : slots) {
            if (!slot.executable()) {
                continue;
            }
            if (slot.descriptor().concurrencySafe()) {
                concurrent.add(slot.invocation());
            } else {
                flushConcurrent(groups, concurrent);
                groups.add(new ToolExecutionGroup(false, List.of(slot.invocation())));
            }
        }
        flushConcurrent(groups, concurrent);
        return List.copyOf(groups);
    }

    private static void flushConcurrent(List<ToolExecutionGroup> groups,
                                        List<ToolInvocation> concurrent) {
        if (!concurrent.isEmpty()) {
            groups.add(new ToolExecutionGroup(true, List.copyOf(concurrent)));
            concurrent.clear();
        }
    }

    private static void assertUniqueCallIds(List<ToolInvocation> calls) {
        Set<String> callIds = new HashSet<>();
        for (ToolInvocation call : calls) {
            if (!callIds.add(call.callId())) {
                throw new IllegalArgumentException("Duplicate tool callId: " + call.callId());
            }
        }
    }
}
