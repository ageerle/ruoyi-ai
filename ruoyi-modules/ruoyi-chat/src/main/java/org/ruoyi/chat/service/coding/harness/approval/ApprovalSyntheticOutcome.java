package org.ruoyi.chat.service.coding.harness.approval;

/** Stable synthetic error result for a denied or expired invocation. */
public record ApprovalSyntheticOutcome(
    String toolCallId,
    String toolName,
    ApprovalState state,
    String reason,
    String message
) {

    public ApprovalSyntheticOutcome {
        if (toolCallId == null || toolCallId.isBlank() || toolName == null || toolName.isBlank()
            || state == null || reason == null || reason.isBlank()
            || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Invalid synthetic approval outcome");
        }
        if (state != ApprovalState.DENIED && state != ApprovalState.EXPIRED) {
            throw new IllegalArgumentException("Synthetic outcome requires DENIED or EXPIRED state");
        }
    }
}
