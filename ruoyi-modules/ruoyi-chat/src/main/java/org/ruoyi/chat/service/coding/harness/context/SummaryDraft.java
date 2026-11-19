package org.ruoyi.chat.service.coding.harness.context;

import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

import java.util.List;

/** Structured echo fields let the engine reject a summary that rewrites trusted pins. */
public record SummaryDraft(
    String summary,
    String originalRequirement,
    String currentPlan,
    HarnessPermissionMode permissionMode,
    List<String> securityConstraints,
    String sourceCheckpointId
) {

    public SummaryDraft {
        securityConstraints = securityConstraints == null
            ? List.of() : List.copyOf(securityConstraints);
        currentPlan = currentPlan == null ? "" : currentPlan;
        sourceCheckpointId = sourceCheckpointId == null || sourceCheckpointId.isBlank()
            ? null : sourceCheckpointId;
    }

    public static SummaryDraft preservingPins(SummaryRequest request, String summary) {
        ContextPins pins = request.pins();
        return new SummaryDraft(summary, pins.originalRequirement(), pins.currentPlan(),
            pins.permissionMode(), pins.securityConstraints(), request.previousCheckpointId());
    }
}
