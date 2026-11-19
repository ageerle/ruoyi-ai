package org.ruoyi.chat.service.coding.harness.plan.tool;

import java.util.List;

public record PlanStepCommand(
    PlanStepAction action,
    String stepId,
    long expectedRevision,
    List<String> evidenceIds,
    String reason
) {
    public PlanStepCommand {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
