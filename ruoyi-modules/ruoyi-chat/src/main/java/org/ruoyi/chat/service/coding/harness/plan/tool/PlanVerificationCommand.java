package org.ruoyi.chat.service.coding.harness.plan.tool;

import java.util.List;

public record PlanVerificationCommand(
    PlanVerificationAction action,
    long expectedRevision,
    List<String> evidenceIds
) {
    public PlanVerificationCommand {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
