package org.ruoyi.chat.service.coding.harness.tool;

/** Decision plus stable machine code and human-readable policy reason. */
public record ToolPolicyEvaluation(
    PolicyDecision decision,
    String code,
    String reason
) {

    public ToolPolicyEvaluation {
        if (decision == null || code == null || code.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Invalid tool policy evaluation");
        }
    }
}
