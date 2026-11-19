package org.ruoyi.chat.service.coding.harness.plan;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/** Immutable, persistable unit of scheduling. PlanAggregate is the only transition authority. */
public record PlanTaskStep(
    String stepId,
    String title,
    String instructions,
    PlanTaskStepStatus status,
    List<String> dependencyIds,
    List<String> acceptanceCriterionIds,
    List<String> completionEvidenceIds,
    String statusReason,
    int attempt
) {

    public PlanTaskStep {
        stepId = requireText(stepId, "stepId");
        title = requireText(title, "title");
        instructions = instructions == null ? "" : instructions.strip();
        if (status == null || attempt < 0) {
            throw new IllegalArgumentException("Plan step status is required and attempt must not be negative");
        }
        dependencyIds = normalizeIds(dependencyIds, "dependencyIds", true);
        acceptanceCriterionIds = normalizeIds(acceptanceCriterionIds,
            "acceptanceCriterionIds", true);
        completionEvidenceIds = normalizeIds(completionEvidenceIds,
            "completionEvidenceIds", true);
        statusReason = normalizeOptional(statusReason);

        switch (status) {
            case PENDING -> requireState(statusReason == null && completionEvidenceIds.isEmpty(),
                "PENDING step cannot have a reason or completion evidence");
            case IN_PROGRESS -> {
                requireState(attempt > 0, "IN_PROGRESS step requires an attempt");
                requireState(statusReason == null && completionEvidenceIds.isEmpty(),
                    "IN_PROGRESS step cannot have a reason or completion evidence");
            }
            case BLOCKED, FAILED -> {
                requireState(attempt > 0, status + " step requires an attempt");
                requireState(statusReason != null && completionEvidenceIds.isEmpty(),
                    status + " step requires a reason and cannot have completion evidence");
            }
            case COMPLETED -> {
                requireState(attempt > 0, "COMPLETED step requires an attempt");
                requireState(statusReason == null && !completionEvidenceIds.isEmpty(),
                    "COMPLETED step requires evidence and cannot have a reason");
            }
            case SKIPPED -> requireState(statusReason != null && completionEvidenceIds.isEmpty(),
                "SKIPPED step requires a reason and cannot have completion evidence");
        }
    }

    public static PlanTaskStep pending(String stepId, String title, String instructions,
                                       Collection<String> dependencyIds,
                                       Collection<String> acceptanceCriterionIds) {
        return new PlanTaskStep(stepId, title, instructions, PlanTaskStepStatus.PENDING,
            copy(dependencyIds), copy(acceptanceCriterionIds), List.of(), null, 0);
    }

    public static PlanTaskStep pending(String stepId, String title,
                                       Collection<String> dependencyIds,
                                       Collection<String> acceptanceCriterionIds) {
        return pending(stepId, title, "", dependencyIds, acceptanceCriterionIds);
    }

    static PlanTaskStep legacyMarkdownCompatibilityStep() {
        return new PlanTaskStep("legacy-markdown-plan", "Execute approved markdown plan", "",
            PlanTaskStepStatus.SKIPPED, List.of(), List.of(), List.of(),
            "Compatibility step for the legacy markdown-only plan API", 0);
    }

    PlanTaskStep start() {
        return copy(PlanTaskStepStatus.IN_PROGRESS, List.of(), null, attempt + 1);
    }

    PlanTaskStep block(String reason) {
        return copy(PlanTaskStepStatus.BLOCKED, List.of(), requireText(reason, "blocked reason"), attempt);
    }

    PlanTaskStep fail(String reason) {
        return copy(PlanTaskStepStatus.FAILED, List.of(), requireText(reason, "failure reason"), attempt);
    }

    PlanTaskStep retry() {
        return copy(PlanTaskStepStatus.PENDING, List.of(), null, attempt);
    }

    PlanTaskStep skip(String reason) {
        return copy(PlanTaskStepStatus.SKIPPED, List.of(), requireText(reason, "skip reason"), attempt);
    }

    PlanTaskStep complete(Collection<String> evidenceIds) {
        return copy(PlanTaskStepStatus.COMPLETED, copy(evidenceIds), null, attempt);
    }

    private PlanTaskStep copy(PlanTaskStepStatus nextStatus, List<String> evidenceIds,
                              String reason, int nextAttempt) {
        return new PlanTaskStep(stepId, title, instructions, nextStatus, dependencyIds,
            acceptanceCriterionIds, evidenceIds, reason, nextAttempt);
    }

    private static List<String> normalizeIds(Collection<String> values, String field,
                                             boolean emptyAllowed) {
        TreeSet<String> normalized = new TreeSet<>();
        if (values != null) {
            for (String value : values) {
                normalized.add(requireText(value, field));
            }
        }
        if (!emptyAllowed && normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return List.copyOf(normalized);
    }

    private static List<String> copy(Collection<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Plan step " + field + " must not be blank");
        }
        return value.strip();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void requireState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
