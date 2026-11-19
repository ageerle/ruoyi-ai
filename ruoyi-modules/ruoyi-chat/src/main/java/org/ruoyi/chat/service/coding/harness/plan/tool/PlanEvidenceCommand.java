package org.ruoyi.chat.service.coding.harness.plan.tool;

public record PlanEvidenceCommand(
    String toolCallId,
    long expectedRevision
) {

    public PlanEvidenceCommand {
        if (toolCallId == null || toolCallId.isBlank()) {
            throw new IllegalArgumentException("Evidence toolCallId must not be blank");
        }
        toolCallId = toolCallId.strip();
        if (expectedRevision < 0) {
            throw new IllegalArgumentException("Evidence expectedRevision must not be negative");
        }
    }
}
