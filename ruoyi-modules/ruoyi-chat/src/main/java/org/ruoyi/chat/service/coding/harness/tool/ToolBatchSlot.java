package org.ruoyi.chat.service.coding.harness.tool;

/** One policy disposition and exactly one result slot for an input tool call. */
public record ToolBatchSlot(
    ToolInvocation invocation,
    ToolDescriptor descriptor,
    ToolPolicyEvaluation policy,
    ToolOutcome outcome
) {

    public ToolBatchSlot {
        if (invocation == null || policy == null || outcome == null
            || !invocation.callId().equals(outcome.callId())
            || !invocation.toolName().equals(outcome.toolName())) {
            throw new IllegalArgumentException("Invalid tool batch slot");
        }
        if (policy.decision() == PolicyDecision.ALLOW && descriptor == null) {
            throw new IllegalArgumentException("Allowed tool slot requires a descriptor");
        }
        if (descriptor != null && !descriptor.toolName().equalsIgnoreCase(invocation.toolName())) {
            throw new IllegalArgumentException("Tool descriptor does not match invocation");
        }
        boolean outcomeMatchesDecision = switch (policy.decision()) {
            case ALLOW -> outcome.status() == ToolOutcomeStatus.PENDING && !outcome.synthetic();
            case ASK -> outcome.status() == ToolOutcomeStatus.APPROVAL_REQUIRED && outcome.synthetic();
            case DENY -> outcome.status() == ToolOutcomeStatus.DENIED && outcome.synthetic();
        };
        if (!outcomeMatchesDecision) {
            throw new IllegalArgumentException("Tool outcome does not match policy decision");
        }
    }

    public boolean executable() {
        return policy.decision() == PolicyDecision.ALLOW;
    }
}
