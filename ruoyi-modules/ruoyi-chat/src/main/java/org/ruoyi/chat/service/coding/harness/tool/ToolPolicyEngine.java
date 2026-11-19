package org.ruoyi.chat.service.coding.harness.tool;

import org.ruoyi.chat.service.coding.harness.model.HarnessApprovalPolicy;
import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Fail-closed policy evaluation based on declared effects, permission mode and task contract.
 * An {@link PolicyDecision#ALLOW} for EXECUTE is authorization only; process isolation and
 * resource/network sandboxing remain mandatory responsibilities of the execution runtime.
 */
public final class ToolPolicyEngine {

    private final Map<String, ToolDescriptor> descriptors;

    public ToolPolicyEngine(Collection<ToolDescriptor> descriptors) {
        if (descriptors == null) {
            throw new IllegalArgumentException("Tool descriptors are required");
        }
        Map<String, ToolDescriptor> indexed = new LinkedHashMap<>();
        for (ToolDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                throw new IllegalArgumentException("Tool descriptor cannot be null");
            }
            String key = key(descriptor.toolName());
            if (indexed.putIfAbsent(key, descriptor) != null) {
                throw new IllegalArgumentException("Duplicate tool descriptor: " + descriptor.toolName());
            }
        }
        this.descriptors = Map.copyOf(indexed);
    }

    public Optional<ToolDescriptor> descriptor(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(descriptors.get(key(toolName)));
    }

    public ToolPolicyEvaluation evaluate(ToolInvocation invocation,
                                         HarnessPermissionMode permissionMode,
                                         ToolPolicyContract contract) {
        return evaluate(invocation, permissionMode, HarnessApprovalPolicy.ON_REQUEST, contract);
    }

    public ToolPolicyEvaluation evaluate(ToolInvocation invocation,
                                         HarnessPermissionMode permissionMode,
                                         HarnessApprovalPolicy approvalPolicy,
                                         ToolPolicyContract contract) {
        if (invocation == null) {
            throw new IllegalArgumentException("Tool invocation is required");
        }
        ToolDescriptor descriptor = descriptor(invocation.toolName()).orElse(null);
        if (descriptor == null) {
            return deny("unknown_tool", "Unknown tools are denied by default: " + invocation.toolName());
        }
        if (permissionMode == null) {
            return deny("permission_mode_missing", "No permission mode was supplied");
        }
        HarnessApprovalPolicy effectiveApprovalPolicy = approvalPolicy == null
            ? HarnessApprovalPolicy.ON_REQUEST : approvalPolicy;
        if (contract != null && contract.forbids(invocation)) {
            return deny("operation_forbidden", "The task contract forbids operation: " + invocation.operation());
        }

        boolean workspaceMutation = descriptor.hasCapability(ToolCapability.WRITE)
            || descriptor.hasCapability(ToolCapability.DESTRUCTIVE);
        // CONTROL updates run/plan state. It is not a workspace mutation unless WRITE/DESTRUCTIVE
        // is also explicitly declared on the descriptor.
        if (workspaceMutation && contract != null
            && !contract.permitsAllMutationTargets(invocation.mutationTargets())) {
            return deny("write_root_not_allowed",
                "Mutation targets are missing or outside the task contract write roots");
        }

        return switch (permissionMode) {
            case READ_ONLY -> evaluateReadOnly(descriptor);
            case WORKSPACE_WRITE -> evaluateWorkspaceWrite(descriptor, effectiveApprovalPolicy);
            case FULL_ACCESS -> evaluateFullAccess(descriptor, effectiveApprovalPolicy);
        };
    }

    private ToolPolicyEvaluation evaluateReadOnly(ToolDescriptor descriptor) {
        if (descriptor.hasCapability(ToolCapability.WRITE)
            || descriptor.hasCapability(ToolCapability.EXECUTE)
            || descriptor.hasCapability(ToolCapability.NETWORK)
            || descriptor.hasCapability(ToolCapability.DESTRUCTIVE)) {
            return deny("read_only_violation", "The selected permission mode is read-only");
        }
        return allow();
    }

    private ToolPolicyEvaluation evaluateWorkspaceWrite(ToolDescriptor descriptor,
                                                         HarnessApprovalPolicy approvalPolicy) {
        if (descriptor.hasCapability(ToolCapability.DESTRUCTIVE)
            || descriptor.hasCapability(ToolCapability.EXECUTE)
            || descriptor.hasCapability(ToolCapability.NETWORK)) {
            return approvalPolicy == HarnessApprovalPolicy.NEVER ? allow() : ask(descriptor);
        }
        return allow();
    }

    private ToolPolicyEvaluation evaluateFullAccess(ToolDescriptor descriptor,
                                                    HarnessApprovalPolicy approvalPolicy) {
        if (descriptor.hasCapability(ToolCapability.DESTRUCTIVE)
            || descriptor.hasCapability(ToolCapability.EXECUTE)
            || descriptor.hasCapability(ToolCapability.NETWORK)) {
            return approvalPolicy == HarnessApprovalPolicy.NEVER ? allow() : ask(descriptor);
        }
        return allow();
    }

    private static ToolPolicyEvaluation allow() {
        return new ToolPolicyEvaluation(PolicyDecision.ALLOW, "allowed", "Tool call is allowed");
    }

    private static ToolPolicyEvaluation ask(ToolDescriptor descriptor) {
        return new ToolPolicyEvaluation(PolicyDecision.ASK, "approval_required",
            "Explicit approval is required: " + descriptor.riskSummary());
    }

    private static ToolPolicyEvaluation deny(String code, String reason) {
        return new ToolPolicyEvaluation(PolicyDecision.DENY, code, reason);
    }

    private static String key(String toolName) {
        return toolName.trim().toLowerCase(Locale.ROOT);
    }
}
