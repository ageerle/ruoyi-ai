package org.ruoyi.chat.service.coding.harness.model;

import java.util.List;

public record HarnessPlanStep(
    String stepId,
    String title,
    String description,
    HarnessPlanStepStatus status,
    List<String> dependencies,
    List<String> affectedPaths,
    List<String> evidence,
    int attempts,
    String failureReason,
    String replanReason,
    long createdAt,
    long updatedAt
) {

    public HarnessPlanStep {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        affectedPaths = affectedPaths == null ? List.of() : List.copyOf(affectedPaths);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        if (stepId == null || stepId.isBlank() || title == null || title.isBlank() || status == null) {
            throw new IllegalArgumentException("Invalid plan step");
        }
    }
}
