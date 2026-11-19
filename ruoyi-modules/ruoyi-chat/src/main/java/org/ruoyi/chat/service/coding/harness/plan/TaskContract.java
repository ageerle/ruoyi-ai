package org.ruoyi.chat.service.coding.harness.plan;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable task scope and acceptance contract. */
public record TaskContract(
    UUID contractId,
    String kind,
    String normalizedGoal,
    List<AcceptanceCriterion> criteria,
    Set<String> allowedMutationRoots,
    Set<String> forbiddenOperations
) {

    public TaskContract {
        if (contractId == null) {
            throw new IllegalArgumentException("contractId must not be null");
        }
        kind = requireText(kind, "kind");
        normalizedGoal = requireText(normalizedGoal, "normalizedGoal");
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("A coding task contract must define acceptance criteria");
        }
        if (criteria.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Acceptance criteria must not contain null values");
        }
        Set<String> criterionIds = new HashSet<>();
        for (AcceptanceCriterion criterion : criteria) {
            if (!criterionIds.add(criterion.id())) {
                throw new IllegalArgumentException("Duplicate acceptance criterion id: " + criterion.id());
            }
        }
        allowedMutationRoots = immutableSortedStrings(allowedMutationRoots, "allowedMutationRoots", false);
        forbiddenOperations = immutableSortedStrings(forbiddenOperations, "forbiddenOperations", true);
    }

    public static TaskContract create(String kind, String normalizedGoal,
                                      List<AcceptanceCriterion> criteria,
                                      Set<String> allowedMutationRoots,
                                      Set<String> forbiddenOperations) {
        return new TaskContract(UUID.randomUUID(), kind, normalizedGoal, criteria,
            allowedMutationRoots, forbiddenOperations);
    }

    public boolean allCriteriaSatisfiedBy(List<ExecutionEvidence> evidence) {
        List<ExecutionEvidence> available = evidence == null ? List.of() : evidence;
        return criteria.stream().allMatch(criterion -> available.stream().anyMatch(criterion::isSatisfiedBy));
    }

    public List<AcceptanceCriterion> unmetCriteria(List<ExecutionEvidence> evidence) {
        List<ExecutionEvidence> available = evidence == null ? List.of() : evidence;
        return criteria.stream()
            .filter(criterion -> available.stream().noneMatch(criterion::isSatisfiedBy))
            .toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Task contract " + field + " must not be blank");
        }
        return value.strip();
    }

    private static Set<String> immutableSortedStrings(Set<String> values, String field,
                                                       boolean emptyAllowed) {
        if (values == null) {
            values = Set.of();
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " must not contain blank values");
            }
            normalized.add(value.strip());
        }
        if (!emptyAllowed && normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return Collections.unmodifiableSet(normalized);
    }
}
