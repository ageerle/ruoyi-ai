package org.ruoyi.chat.service.coding.harness.plan.tool;

import org.ruoyi.chat.service.coding.harness.plan.ExecutionMode;
import org.ruoyi.chat.service.coding.harness.plan.PlanReviewState;

import java.util.List;

public record PlanToolResult(
    String taskId,
    long revision,
    String canonicalHash,
    ExecutionMode mode,
    PlanReviewState reviewState,
    List<String> readyStepIds,
    String inProgressStepId,
    List<String> evidenceIds,
    String detail
) {
    public PlanToolResult {
        readyStepIds = readyStepIds == null ? List.of() : List.copyOf(readyStepIds);
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
