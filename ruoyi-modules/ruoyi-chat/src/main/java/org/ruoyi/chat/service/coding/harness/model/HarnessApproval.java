package org.ruoyi.chat.service.coding.harness.model;

import java.util.Map;

public record HarnessApproval(
    String approvalId,
    String toolCallId,
    String toolName,
    String capability,
    String summary,
    Map<String, Object> argumentsPreview,
    HarnessApprovalStatus status,
    long createdAt,
    long resolvedAt,
    Long resolvedBy,
    String resolutionNote
) {

    public HarnessApproval {
        argumentsPreview = argumentsPreview == null ? Map.of() : Map.copyOf(argumentsPreview);
        if (approvalId == null || approvalId.isBlank() || toolCallId == null || toolCallId.isBlank()
            || toolName == null || toolName.isBlank() || status == null) {
            throw new IllegalArgumentException("Invalid approval");
        }
    }
}
