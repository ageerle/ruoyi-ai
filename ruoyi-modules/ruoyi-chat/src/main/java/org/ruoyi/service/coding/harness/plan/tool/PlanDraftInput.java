package org.ruoyi.service.coding.harness.plan.tool;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record PlanDraftInput(
    @Description("Concise implementation and verification plan")
    String planMarkdown,
    @Description("Task kind, for example FEATURE or FIX")
    String kind,
    @Description("Mechanical criteria using only the two exact supported criterion forms; every criterion id must be bound to at least one step")
    List<PlanCriterionInput> criteria,
    @Description("Concise executable steps sized to the task. Group related work, avoid one step per file, and collectively bind every criterion id. Unbounded runs have no fixed step-count limit")
    List<PlanStepInput> steps,
    @Description("Only relative production paths authorized to change; never include read-only tests unless requested")
    List<String> allowedMutationRoots,
    @Description("Operations and paths that must remain unchanged")
    List<String> forbiddenOperations
) {
    public PlanDraftInput {
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        steps = steps == null ? List.of() : List.copyOf(steps);
        allowedMutationRoots = allowedMutationRoots == null
            ? List.of() : List.copyOf(allowedMutationRoots);
        forbiddenOperations = forbiddenOperations == null
            ? List.of() : List.copyOf(forbiddenOperations);
    }
}
