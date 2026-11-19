package org.ruoyi.chat.service.coding.harness.plan.tool;

import java.util.List;

public record PlanStepInput(
    String stepId,
    String title,
    String instructions,
    List<String> dependencyIds,
    List<String> acceptanceCriterionIds
) {
    public PlanStepInput {
        dependencyIds = dependencyIds == null ? List.of() : List.copyOf(dependencyIds);
        acceptanceCriterionIds = acceptanceCriterionIds == null
            ? List.of() : List.copyOf(acceptanceCriterionIds);
    }
}
