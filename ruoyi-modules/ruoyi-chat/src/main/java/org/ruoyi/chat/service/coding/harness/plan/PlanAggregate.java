package org.ruoyi.chat.service.coding.harness.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Persistable source of truth for planning, scheduling and verification.
 *
 * <p>Plan approval is intentionally a control-plane domain command. Step completion accepts only
 * references to persisted {@link ExecutionEvidence}; model prose is never a completion input.</p>
 */
public record PlanAggregate(
    int schemaVersion,
    UUID taskId,
    long revision,
    ExecutionMode mode,
    PlanReviewState reviewState,
    TaskContract contract,
    String originalRequest,
    String planMarkdown,
    List<PlanTaskStep> steps,
    List<PlanFeedback> feedbackHistory,
    List<ExecutionEvidence> evidence,
    Map<String, PlanApprovalReceipt> approvalReceipts,
    ExecutionMode blockedFromMode,
    String blockedReason,
    String failureReason,
    long createdAt,
    long updatedAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 2;

    public PlanAggregate {
        if (schemaVersion < 1 || taskId == null || revision < 0 || mode == null
            || reviewState == null || contract == null) {
            throw new IllegalArgumentException("Invalid plan aggregate identity or state");
        }
        originalRequest = requirePinnedText(originalRequest, "originalRequest");
        planMarkdown = planMarkdown == null ? "" : planMarkdown;
        steps = normalizeSteps(steps);
        feedbackHistory = normalizeFeedback(feedbackHistory);
        evidence = normalizeEvidence(evidence);
        approvalReceipts = normalizeReceipts(approvalReceipts);
        blockedReason = normalizeOptional(blockedReason);
        failureReason = normalizeOptional(failureReason);
        if (createdAt <= 0 || updatedAt < createdAt) {
            throw new IllegalArgumentException("Invalid plan aggregate timestamps");
        }
        validateState(schemaVersion, mode, reviewState, planMarkdown, contract, steps, evidence,
            blockedFromMode, blockedReason, failureReason);
    }

    public static PlanAggregate create(UUID taskId, String originalRequest,
                                       TaskContract contract, long now) {
        return new PlanAggregate(CURRENT_SCHEMA_VERSION, taskId, 0, ExecutionMode.PLAN,
            PlanReviewState.DRAFT, contract, originalRequest, "", List.of(), List.of(), List.of(),
            Map.of(), null, null, null, now, now);
    }

    public static PlanAggregate create(String originalRequest, TaskContract contract, long now) {
        return create(UUID.randomUUID(), originalRequest, contract, now);
    }

    /** Hashes the complete semantic aggregate using a schema-versioned canonical encoding. */
    @JsonProperty(value = "canonicalHash", access = JsonProperty.Access.READ_ONLY)
    public String canonicalHash() {
        return CanonicalPlanHasher.hash(this);
    }

    /**
     * Compatibility overload for existing callers. New integrations should supply explicit steps.
     * The synthetic skipped step keeps legacy markdown plans schedulable without pretending that a
     * model-authored sentence was verifier-completed.
     */
    public PlanAggregate replacePlan(String markdown, long now) {
        return replacePlanInternal(markdown,
            List.of(PlanTaskStep.legacyMarkdownCompatibilityStep()), true, now);
    }

    /** Replaces the reviewed plan and its complete, pending dependency DAG. */
    public PlanAggregate replacePlan(String markdown, List<PlanTaskStep> replacementSteps,
                                     long now) {
        return replacePlanInternal(markdown, replacementSteps, false, now);
    }

    private PlanAggregate replacePlanInternal(String markdown, List<PlanTaskStep> replacementSteps,
                                              boolean legacyCompatibility, long now) {
        requireMode(ExecutionMode.PLAN, "replace the plan");
        requireMonotonicTime(now);
        String nextPlan = requireText(markdown, "planMarkdown");
        List<PlanTaskStep> normalized = normalizeSteps(replacementSteps);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("A reviewed plan must define at least one step");
        }
        if (!legacyCompatibility && normalized.stream()
            .anyMatch(step -> step.status() != PlanTaskStepStatus.PENDING)) {
            throw new IllegalArgumentException("Replacement plan steps must begin in PENDING state");
        }
        return new PlanAggregate(CURRENT_SCHEMA_VERSION, taskId, revision + 1, mode,
            PlanReviewState.AWAITING_APPROVAL, contract, originalRequest, nextPlan, normalized,
            feedbackHistory, evidence, approvalReceipts, null, null, null, createdAt, now);
    }

    /** Records feedback without changing task identity or the immutable original request. */
    public PlanAggregate requestRevision(String feedbackId, String content, long now) {
        requireMode(ExecutionMode.PLAN, "request a plan revision");
        PlanFeedback candidate = new PlanFeedback(feedbackId, content, now);
        for (PlanFeedback existing : feedbackHistory) {
            if (existing.feedbackId().equals(candidate.feedbackId())) {
                if (existing.content().equals(candidate.content())) {
                    return this;
                }
                throw new PlanIdempotencyConflictException(candidate.feedbackId());
            }
        }
        if (reviewState != PlanReviewState.AWAITING_APPROVAL) {
            throw new IllegalStateException("Plan feedback requires an awaiting-approval draft");
        }
        List<PlanFeedback> nextFeedback = new ArrayList<>(feedbackHistory);
        nextFeedback.add(candidate);
        return next(revision + 1, mode, PlanReviewState.REVISION_REQUESTED, planMarkdown,
            steps, nextFeedback, evidence, approvalReceipts, null, null, null, now);
    }

    /**
     * Atomically validates task identity, aggregate revision and canonical hash before entering
     * BUILD. Reusing the same idempotency key with the same command is a no-op.
     */
    public PlanAggregate approveFromControlPlane(PlanApprovalCommand command, long now) {
        Objects.requireNonNull(command, "command");
        PlanApprovalReceipt existing = approvalReceipts.get(command.idempotencyKey());
        if (existing != null) {
            if (existing.matches(command)) {
                return this;
            }
            throw new PlanIdempotencyConflictException(command.idempotencyKey());
        }

        requireMode(ExecutionMode.PLAN, "approve the plan");
        if (reviewState != PlanReviewState.AWAITING_APPROVAL) {
            throw new IllegalStateException("Only an awaiting-approval plan can be approved");
        }
        if (!taskId.equals(command.taskId())) {
            throw new StalePlanApprovalException("taskId", command.taskId(), taskId);
        }
        if (revision != command.expectedRevision()) {
            throw new StalePlanApprovalException("revision", command.expectedRevision(), revision);
        }
        String actualHash = canonicalHash();
        if (!constantTimeEquals(command.expectedHash(), actualHash)) {
            throw new StalePlanApprovalException("hash", command.expectedHash(), actualHash);
        }

        long approvedRevision = revision + 1;
        PlanApprovalReceipt receipt = new PlanApprovalReceipt(command.idempotencyKey(), taskId,
            command.expectedRevision(), command.expectedHash(), approvedRevision, now);
        Map<String, PlanApprovalReceipt> nextReceipts = new LinkedHashMap<>(approvalReceipts);
        nextReceipts.put(receipt.idempotencyKey(), receipt);
        return next(approvedRevision, ExecutionMode.BUILD, PlanReviewState.APPROVED,
            planMarkdown, steps, feedbackHistory, evidence, nextReceipts, null, null, null, now);
    }

    public PlanAggregate recordEvidence(Collection<ExecutionEvidence> additionalEvidence, long now) {
        return recordEvidence(additionalEvidence, revision, now);
    }

    /** Optimistic, idempotent evidence append. Distinct failed attempts are never collapsed. */
    public PlanAggregate recordEvidence(Collection<ExecutionEvidence> additionalEvidence,
                                        long expectedRevision, long now) {
        requireAnyMode(List.of(ExecutionMode.BUILD, ExecutionMode.VERIFY), "record evidence");
        List<ExecutionEvidence> merged = mergeEvidence(additionalEvidence);
        if (merged.equals(evidence)) {
            return this;
        }
        requireRevision(expectedRevision);
        return next(revision + 1, mode, reviewState, planMarkdown, steps, feedbackHistory,
            merged, approvalReceipts, null, null, null, now);
    }

    /** Pending steps whose dependencies are completed or skipped, in authoritative plan order. */
    public List<PlanTaskStep> readySteps() {
        Map<String, PlanTaskStep> byId = stepsById(steps);
        return steps.stream()
            .filter(step -> step.status() == PlanTaskStepStatus.PENDING)
            .filter(step -> dependenciesSatisfied(step, byId))
            .toList();
    }

    public Optional<PlanTaskStep> inProgressStep() {
        return steps.stream().filter(step -> step.status() == PlanTaskStepStatus.IN_PROGRESS)
            .findFirst();
    }

    public PlanAggregate startStep(String stepId, long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "start a plan step");
        PlanTaskStep step = requireStep(stepId);
        if (step.status() == PlanTaskStepStatus.IN_PROGRESS) {
            return this;
        }
        if (step.status() != PlanTaskStepStatus.PENDING) {
            throw stepTransition(step, "start");
        }
        requireRevision(expectedRevision);
        if (inProgressStep().isPresent()) {
            throw new IllegalStateException("Only one plan step may be IN_PROGRESS");
        }
        if (!dependenciesSatisfied(step, stepsById(steps))) {
            throw new IllegalStateException("Cannot start step " + step.stepId()
                + "; dependencies are not completed or skipped: " + step.dependencyIds());
        }
        return updateStep(step.start(), now);
    }

    public PlanAggregate blockStep(String stepId, String reason, long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "block a plan step");
        PlanTaskStep step = requireStep(stepId);
        String normalizedReason = requireText(reason, "step blocked reason");
        if (step.status() == PlanTaskStepStatus.BLOCKED) {
            if (normalizedReason.equals(step.statusReason())) {
                return this;
            }
            throw new PlanIdempotencyConflictException("step:" + stepId + ":blocked");
        }
        if (step.status() != PlanTaskStepStatus.IN_PROGRESS) {
            throw stepTransition(step, "block");
        }
        requireRevision(expectedRevision);
        return updateStep(step.block(normalizedReason), now);
    }

    public PlanAggregate failStep(String stepId, String reason, long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "fail a plan step");
        PlanTaskStep step = requireStep(stepId);
        String normalizedReason = requireText(reason, "step failure reason");
        if (step.status() == PlanTaskStepStatus.FAILED) {
            if (normalizedReason.equals(step.statusReason())) {
                return this;
            }
            throw new PlanIdempotencyConflictException("step:" + stepId + ":failed");
        }
        if (step.status() != PlanTaskStepStatus.IN_PROGRESS) {
            throw stepTransition(step, "fail");
        }
        requireRevision(expectedRevision);
        return updateStep(step.fail(normalizedReason), now);
    }

    public PlanAggregate retryStep(String stepId, long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "retry a plan step");
        PlanTaskStep step = requireStep(stepId);
        if (step.status() == PlanTaskStepStatus.PENDING && step.attempt() > 0) {
            return this;
        }
        if (step.status() != PlanTaskStepStatus.BLOCKED
            && step.status() != PlanTaskStepStatus.FAILED) {
            throw stepTransition(step, "retry");
        }
        requireRevision(expectedRevision);
        return updateStep(step.retry(), now);
    }

    public PlanAggregate skipStep(String stepId, String reason, long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "skip a plan step");
        PlanTaskStep step = requireStep(stepId);
        String normalizedReason = requireText(reason, "step skip reason");
        if (step.status() == PlanTaskStepStatus.SKIPPED) {
            if (normalizedReason.equals(step.statusReason())) {
                return this;
            }
            throw new PlanIdempotencyConflictException("step:" + stepId + ":skipped");
        }
        if (step.status() == PlanTaskStepStatus.IN_PROGRESS
            || step.status() == PlanTaskStepStatus.COMPLETED) {
            throw stepTransition(step, "skip");
        }
        requireRevision(expectedRevision);
        return updateStep(step.skip(normalizedReason), now);
    }

    /**
     * Completes an active step only through successful persisted verifier evidence. Every bound
     * acceptance criterion must be satisfied by one of the referenced evidence records.
     */
    public PlanAggregate completeStep(String stepId, Collection<String> completionEvidenceIds,
                                      long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "complete a plan step");
        PlanTaskStep step = requireStep(stepId);
        List<String> normalizedIds = normalizeIds(completionEvidenceIds, "completionEvidenceIds");
        if (step.status() == PlanTaskStepStatus.COMPLETED) {
            if (step.completionEvidenceIds().equals(normalizedIds)) {
                return this;
            }
            throw new PlanIdempotencyConflictException("step:" + stepId + ":completed");
        }
        if (step.status() != PlanTaskStepStatus.IN_PROGRESS) {
            throw stepTransition(step, "complete");
        }
        requireRevision(expectedRevision);
        validateCompletionEvidence(step, normalizedIds, contract, evidence);
        return updateStep(step.complete(normalizedIds), now);
    }

    /** Reorders the complete step set without changing dependency or execution semantics. */
    public PlanAggregate reorderSteps(List<String> orderedStepIds, long expectedRevision, long now) {
        requireMode(ExecutionMode.BUILD, "reorder plan steps");
        List<String> requested = orderedStepIds == null ? List.of() : List.copyOf(orderedStepIds);
        List<String> current = steps.stream().map(PlanTaskStep::stepId).toList();
        if (current.equals(requested)) {
            return this;
        }
        if (inProgressStep().isPresent()) {
            throw new IllegalStateException("Cannot reorder steps while one is IN_PROGRESS");
        }
        if (requested.size() != current.size() || new HashSet<>(requested).size() != requested.size()
            || !new HashSet<>(requested).equals(new HashSet<>(current))) {
            throw new IllegalArgumentException("Reordered step ids must contain every plan step exactly once");
        }
        requireRevision(expectedRevision);
        Map<String, PlanTaskStep> byId = stepsById(steps);
        List<PlanTaskStep> reordered = requested.stream().map(byId::get).toList();
        return next(revision + 1, mode, reviewState, planMarkdown, reordered,
            feedbackHistory, evidence, approvalReceipts, null, null, null, now);
    }

    public PlanAggregate beginVerification(long now) {
        requireMode(ExecutionMode.BUILD, "begin verification");
        if (schemaVersion >= 2 && steps.stream().anyMatch(step -> !step.status().isTerminal())) {
            throw new IllegalStateException("Verification requires every plan step to be completed or skipped");
        }
        return next(revision + 1, ExecutionMode.VERIFY, reviewState, planMarkdown, steps,
            feedbackHistory, evidence, approvalReceipts, null, null, null, now);
    }

    /**
     * A failed final review returns structured work to an actionable BUILD state. Successful
     * executable evidence predates the discovered defect and cannot be reused to auto-complete the
     * repair. A successful FILE_MUTATION remains a durable fact that the bound file was changed;
     * the repaired step must still produce fresh PROCESS_EXIT evidence before it can close. Failed
     * evidence remains as diagnostics. For a structured plan, the last completed step is
     * reopened as FAILED so the model must explicitly retry it before mutating again. Every
     * completed step that references invalidated evidence must reopen; otherwise constructing the
     * next aggregate would retain a dangling completion-evidence id and trap VERIFY forever. If
     * every completed step is backed only by retained file-mutation facts, reopen the last one to
     * provide an actionable repair step. A legacy compatibility step remains skipped but can no
     * longer pass without fresh evidence.
     */
    public PlanAggregate verificationFailed(Collection<ExecutionEvidence> verificationEvidence,
                                            long now) {
        requireMode(ExecutionMode.VERIFY, "fail verification");
        List<ExecutionEvidence> merged = mergeEvidence(verificationEvidence);
        List<PlanTaskStep> nextSteps = steps;
        List<ExecutionEvidence> retainedEvidence = merged;
        if (schemaVersion >= 2) {
            retainedEvidence = merged.stream()
                .filter(evidence -> !evidence.successful()
                    || AcceptanceCriterion.FILE_MUTATION_TYPE.equals(evidence.type()))
                .toList();
            Set<String> retainedEvidenceIds = retainedEvidence.stream()
                .map(ExecutionEvidence::evidenceId).collect(java.util.stream.Collectors.toSet());
            Set<String> reopenedIds = steps.stream()
                .filter(step -> step.status() == PlanTaskStepStatus.COMPLETED)
                .filter(step -> !retainedEvidenceIds.containsAll(step.completionEvidenceIds()))
                .map(PlanTaskStep::stepId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            if (reopenedIds.isEmpty()) {
                for (int index = steps.size() - 1; index >= 0; index--) {
                    if (steps.get(index).status() == PlanTaskStepStatus.COMPLETED) {
                        reopenedIds.add(steps.get(index).stepId());
                        break;
                    }
                }
            }
            // A completed descendant cannot remain completed after one of its prerequisites is
            // reopened. Close the transitive dependency set before constructing the immutable
            // aggregate; otherwise validation observes a FAILED parent under a started child and
            // makes plan_verify FAIL itself impossible.
            boolean expanded;
            do {
                expanded = false;
                for (PlanTaskStep step : steps) {
                    if (step.status() == PlanTaskStepStatus.COMPLETED
                        && !reopenedIds.contains(step.stepId())
                        && step.dependencyIds().stream().anyMatch(reopenedIds::contains)) {
                        reopenedIds.add(step.stepId());
                        expanded = true;
                    }
                }
            } while (expanded);
            List<PlanTaskStep> reopened = new ArrayList<>(steps.size());
            for (PlanTaskStep step : steps) {
                if (!reopenedIds.contains(step.stepId())) {
                    reopened.add(step);
                    continue;
                }
                PlanTaskStep failed = step.fail("Final review rejected the implementation");
                boolean dependencyReopened = step.dependencyIds().stream()
                    .anyMatch(reopenedIds::contains);
                reopened.add(dependencyReopened ? failed.retry() : failed);
            }
            nextSteps = List.copyOf(reopened);
        }
        return next(revision + 1, ExecutionMode.BUILD, reviewState, planMarkdown, nextSteps,
            feedbackHistory, retainedEvidence, approvalReceipts, null, null, null, now);
    }

    /** Completes only when successful verifier evidence satisfies every contract criterion. */
    public PlanAggregate completeVerification(Collection<ExecutionEvidence> verificationEvidence,
                                              long now) {
        requireMode(ExecutionMode.VERIFY, "complete verification");
        List<ExecutionEvidence> merged = mergeEvidence(verificationEvidence);
        List<AcceptanceCriterion> unmet = contract.unmetCriteria(merged);
        if (!unmet.isEmpty()) {
            throw new IllegalStateException("Verification cannot complete; unmet criteria: "
                + unmet.stream().map(AcceptanceCriterion::id).toList());
        }
        return next(revision + 1, ExecutionMode.COMPLETED, reviewState, planMarkdown, steps,
            feedbackHistory, merged, approvalReceipts, null, null, null, now);
    }

    public PlanAggregate markBlocked(String reason, long now) {
        if (mode == ExecutionMode.BLOCKED) {
            if (Objects.equals(blockedReason, requireText(reason, "blockedReason"))) {
                return this;
            }
            throw new InvalidPlanTransitionException(mode, "replace the blocking reason");
        }
        requireAnyMode(List.of(ExecutionMode.PLAN, ExecutionMode.BUILD, ExecutionMode.VERIFY),
            "mark the task blocked");
        return next(revision + 1, ExecutionMode.BLOCKED, reviewState, planMarkdown, steps,
            feedbackHistory, evidence, approvalReceipts, mode, requireText(reason, "blockedReason"),
            null, now);
    }

    public PlanAggregate resumeFromBlocked(long now) {
        requireMode(ExecutionMode.BLOCKED, "resume the blocked task");
        return next(revision + 1, blockedFromMode, reviewState, planMarkdown, steps,
            feedbackHistory, evidence, approvalReceipts, null, null, null, now);
    }

    public PlanAggregate markFailed(String reason, long now) {
        String normalized = requireText(reason, "failureReason");
        if (mode == ExecutionMode.FAILED) {
            if (normalized.equals(failureReason)) {
                return this;
            }
            throw new InvalidPlanTransitionException(mode, "replace the failure reason");
        }
        if (mode == ExecutionMode.COMPLETED) {
            throw new InvalidPlanTransitionException(mode, "fail the task");
        }
        return next(revision + 1, ExecutionMode.FAILED, reviewState, planMarkdown, steps,
            feedbackHistory, evidence, approvalReceipts, null, null, normalized, now);
    }

    public boolean allAcceptanceCriteriaMet() {
        return contract.allCriteriaSatisfiedBy(evidence);
    }

    private PlanAggregate updateStep(PlanTaskStep replacement, long now) {
        List<PlanTaskStep> updated = new ArrayList<>(steps);
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).stepId().equals(replacement.stepId())) {
                updated.set(index, replacement);
                return next(revision + 1, mode, reviewState, planMarkdown, updated,
                    feedbackHistory, evidence, approvalReceipts, null, null, null, now);
            }
        }
        throw new IllegalArgumentException("Unknown plan step: " + replacement.stepId());
    }

    private PlanAggregate next(long nextRevision, ExecutionMode nextMode,
                               PlanReviewState nextReviewState, String nextPlan,
                               List<PlanTaskStep> nextSteps,
                               List<PlanFeedback> nextFeedback,
                               List<ExecutionEvidence> nextEvidence,
                               Map<String, PlanApprovalReceipt> nextReceipts,
                               ExecutionMode nextBlockedFrom, String nextBlockedReason,
                               String nextFailureReason, long now) {
        requireMonotonicTime(now);
        return new PlanAggregate(schemaVersion, taskId, nextRevision, nextMode, nextReviewState,
            contract, originalRequest, nextPlan, nextSteps, nextFeedback, nextEvidence, nextReceipts,
            nextBlockedFrom, nextBlockedReason, nextFailureReason, createdAt, now);
    }

    private List<ExecutionEvidence> mergeEvidence(Collection<ExecutionEvidence> additions) {
        LinkedHashMap<String, ExecutionEvidence> merged = new LinkedHashMap<>();
        for (ExecutionEvidence item : evidence) {
            merged.put(item.evidenceId(), item);
        }
        if (additions != null) {
            for (ExecutionEvidence item : additions) {
                if (item == null) {
                    throw new IllegalArgumentException("Evidence collection must not contain null values");
                }
                ExecutionEvidence existing = merged.putIfAbsent(item.evidenceId(), item);
                if (existing != null && !existing.equals(item)) {
                    throw new PlanIdempotencyConflictException(item.evidenceId());
                }
            }
        }
        return List.copyOf(merged.values());
    }

    private PlanTaskStep requireStep(String stepId) {
        String normalized = requireText(stepId, "stepId");
        return steps.stream().filter(step -> step.stepId().equals(normalized)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown plan step: " + normalized));
    }

    private void requireMode(ExecutionMode expected, String action) {
        if (mode != expected) {
            throw new InvalidPlanTransitionException(mode, action);
        }
    }

    private void requireAnyMode(List<ExecutionMode> allowed, String action) {
        if (!allowed.contains(mode)) {
            throw new InvalidPlanTransitionException(mode, action);
        }
    }

    private void requireRevision(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new StalePlanRevisionException(expectedRevision, revision);
        }
    }

    private void requireMonotonicTime(long now) {
        if (now < updatedAt) {
            throw new IllegalArgumentException("Plan update time must be monotonic");
        }
    }

    private static IllegalStateException stepTransition(PlanTaskStep step, String action) {
        return new IllegalStateException("Cannot " + action + " plan step " + step.stepId()
            + " while it is " + step.status());
    }

    private static void validateState(int schemaVersion, ExecutionMode mode,
                                      PlanReviewState reviewState, String planMarkdown,
                                      TaskContract contract, List<PlanTaskStep> steps,
                                      List<ExecutionEvidence> evidence,
                                      ExecutionMode blockedFromMode, String blockedReason,
                                      String failureReason) {
        if (schemaVersion < 2 && !steps.isEmpty()) {
            throw new IllegalArgumentException("Schema v1 plans cannot contain scheduled steps");
        }
        if (mode == ExecutionMode.PLAN && reviewState == PlanReviewState.APPROVED) {
            throw new IllegalArgumentException("PLAN mode cannot have an approved review state");
        }
        if ((mode == ExecutionMode.BUILD || mode == ExecutionMode.VERIFY
            || mode == ExecutionMode.COMPLETED) && reviewState != PlanReviewState.APPROVED) {
            throw new IllegalArgumentException(mode + " mode requires an approved plan");
        }
        if (reviewState == PlanReviewState.AWAITING_APPROVAL || reviewState == PlanReviewState.APPROVED) {
            if (planMarkdown == null || planMarkdown.isBlank()) {
                throw new IllegalArgumentException("A reviewed plan must not be blank");
            }
            if (schemaVersion >= 2 && steps.isEmpty()) {
                throw new IllegalArgumentException("A reviewed plan must define at least one step");
            }
        }
        validateStepGraph(steps, contract, evidence);
        if (mode == ExecutionMode.COMPLETED) {
            if (!contract.allCriteriaSatisfiedBy(evidence)) {
                throw new IllegalArgumentException("COMPLETED mode requires all criteria to be satisfied");
            }
            if (schemaVersion >= 2 && steps.stream().anyMatch(step -> !step.status().isTerminal())) {
                throw new IllegalArgumentException("COMPLETED mode requires all plan steps to be terminal");
            }
        }
        if (mode == ExecutionMode.BLOCKED) {
            if (blockedFromMode == null || blockedFromMode == ExecutionMode.BLOCKED
                || blockedFromMode.isTerminal() || blockedReason == null) {
                throw new IllegalArgumentException("BLOCKED mode requires a resumable source and reason");
            }
        } else if (blockedFromMode != null || blockedReason != null) {
            throw new IllegalArgumentException("Blocking fields are only valid in BLOCKED mode");
        }
        if (mode == ExecutionMode.FAILED) {
            if (failureReason == null) {
                throw new IllegalArgumentException("FAILED mode requires a reason");
            }
        } else if (failureReason != null) {
            throw new IllegalArgumentException("failureReason is only valid in FAILED mode");
        }
    }

    private static void validateStepGraph(List<PlanTaskStep> steps, TaskContract contract,
                                          List<ExecutionEvidence> evidence) {
        Map<String, PlanTaskStep> byId = stepsById(steps);
        Set<String> criterionIds = contract.criteria().stream()
            .map(AcceptanceCriterion::id).collect(java.util.stream.Collectors.toSet());
        long inProgress = steps.stream()
            .filter(step -> step.status() == PlanTaskStepStatus.IN_PROGRESS).count();
        if (inProgress > 1) {
            throw new IllegalArgumentException("Only one plan step may be IN_PROGRESS");
        }
        for (PlanTaskStep step : steps) {
            if (step.dependencyIds().contains(step.stepId())) {
                throw new IllegalArgumentException("Plan step cannot depend on itself: " + step.stepId());
            }
            for (String dependency : step.dependencyIds()) {
                if (!byId.containsKey(dependency)) {
                    throw new IllegalArgumentException("Unknown dependency " + dependency
                        + " for step " + step.stepId());
                }
            }
            for (String criterionId : step.acceptanceCriterionIds()) {
                if (!criterionIds.contains(criterionId)) {
                    throw new IllegalArgumentException("Unknown acceptance criterion " + criterionId
                        + " for step " + step.stepId());
                }
            }
        }
        ensureAcyclic(byId);
        for (PlanTaskStep step : steps) {
            if (step.status() != PlanTaskStepStatus.PENDING
                && step.status() != PlanTaskStepStatus.SKIPPED
                && !dependenciesSatisfied(step, byId)) {
                throw new IllegalArgumentException("Started step has unsatisfied dependencies: "
                    + step.stepId());
            }
            if (step.status() == PlanTaskStepStatus.COMPLETED) {
                validateCompletionEvidence(step, step.completionEvidenceIds(), contract, evidence);
            }
        }
    }

    private static void ensureAcyclic(Map<String, PlanTaskStep> byId) {
        Map<String, Integer> colors = new HashMap<>();
        for (String stepId : byId.keySet()) {
            visit(stepId, byId, colors);
        }
    }

    private static void visit(String stepId, Map<String, PlanTaskStep> byId,
                              Map<String, Integer> colors) {
        int color = colors.getOrDefault(stepId, 0);
        if (color == 1) {
            throw new IllegalArgumentException("Plan step dependency cycle includes: " + stepId);
        }
        if (color == 2) {
            return;
        }
        colors.put(stepId, 1);
        for (String dependency : byId.get(stepId).dependencyIds()) {
            visit(dependency, byId, colors);
        }
        colors.put(stepId, 2);
    }

    private static void validateCompletionEvidence(PlanTaskStep step,
                                                     Collection<String> referencedIds,
                                                     TaskContract contract,
                                                     List<ExecutionEvidence> evidence) {
        List<String> ids = normalizeIds(referencedIds, "completionEvidenceIds");
        Map<String, ExecutionEvidence> byId = new HashMap<>();
        for (ExecutionEvidence item : evidence) {
            byId.put(item.evidenceId(), item);
        }
        List<ExecutionEvidence> referenced = new ArrayList<>();
        for (String evidenceId : ids) {
            ExecutionEvidence item = byId.get(evidenceId);
            if (item == null) {
                throw new IllegalArgumentException("Unknown completion evidence: " + evidenceId);
            }
            if (!item.successful()) {
                throw new IllegalArgumentException("Step completion evidence must be successful: "
                    + evidenceId);
            }
            referenced.add(item);
        }
        Map<String, AcceptanceCriterion> criteria = new HashMap<>();
        for (AcceptanceCriterion criterion : contract.criteria()) {
            criteria.put(criterion.id(), criterion);
        }
        List<String> unmet = step.acceptanceCriterionIds().stream()
            .filter(id -> referenced.stream().noneMatch(criteria.get(id)::isSatisfiedBy))
            .toList();
        if (!unmet.isEmpty()) {
            throw new IllegalArgumentException("Step completion has unmet acceptance criteria: " + unmet);
        }
    }

    private static boolean dependenciesSatisfied(PlanTaskStep step,
                                                 Map<String, PlanTaskStep> byId) {
        return step.dependencyIds().stream()
            .map(byId::get)
            .allMatch(dependency -> dependency != null && dependency.status().satisfiesDependency());
    }

    private static Map<String, PlanTaskStep> stepsById(List<PlanTaskStep> steps) {
        LinkedHashMap<String, PlanTaskStep> byId = new LinkedHashMap<>();
        for (PlanTaskStep step : steps) {
            PlanTaskStep old = byId.putIfAbsent(step.stepId(), step);
            if (old != null) {
                throw new IllegalArgumentException("Duplicate plan step id: " + step.stepId());
            }
        }
        return Collections.unmodifiableMap(byId);
    }

    private static List<PlanTaskStep> normalizeSteps(List<PlanTaskStep> steps) {
        if (steps == null) {
            return List.of();
        }
        if (steps.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Plan steps must not contain null values");
        }
        List<PlanTaskStep> copy = List.copyOf(steps);
        stepsById(copy);
        return copy;
    }

    private static List<PlanFeedback> normalizeFeedback(List<PlanFeedback> feedback) {
        LinkedHashMap<String, PlanFeedback> unique = new LinkedHashMap<>();
        if (feedback != null) {
            for (PlanFeedback item : feedback) {
                if (item == null) {
                    throw new IllegalArgumentException("Feedback history must not contain null values");
                }
                PlanFeedback old = unique.putIfAbsent(item.feedbackId(), item);
                if (old != null && !old.equals(item)) {
                    throw new IllegalArgumentException("Conflicting feedback id: " + item.feedbackId());
                }
            }
        }
        return List.copyOf(unique.values());
    }

    private static List<ExecutionEvidence> normalizeEvidence(List<ExecutionEvidence> evidence) {
        LinkedHashMap<String, ExecutionEvidence> unique = new LinkedHashMap<>();
        if (evidence != null) {
            for (ExecutionEvidence item : evidence) {
                if (item == null) {
                    throw new IllegalArgumentException("Evidence must not contain null values");
                }
                ExecutionEvidence old = unique.putIfAbsent(item.evidenceId(), item);
                if (old != null && !old.equals(item)) {
                    throw new IllegalArgumentException("Conflicting evidence id: " + item.evidenceId());
                }
            }
        }
        return List.copyOf(unique.values());
    }

    private static Map<String, PlanApprovalReceipt> normalizeReceipts(
        Map<String, PlanApprovalReceipt> receipts) {
        TreeMap<String, PlanApprovalReceipt> normalized = new TreeMap<>();
        if (receipts != null) {
            receipts.forEach((key, receipt) -> {
                if (key == null || receipt == null || !key.equals(receipt.idempotencyKey())) {
                    throw new IllegalArgumentException("Approval receipt key mismatch");
                }
                normalized.put(key, receipt);
            });
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static List<String> normalizeIds(Collection<String> values, String field) {
        TreeSet<String> normalized = new TreeSet<>();
        if (values != null) {
            for (String value : values) {
                normalized.add(requireText(value, field));
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        return List.copyOf(normalized);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
            actual.getBytes(StandardCharsets.US_ASCII));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    /**
     * The original request is a byte-significant security pin. It must be validated as non-blank
     * but never normalized: stripping a trailing line ending here makes every plan for a normal
     * text file differ from {@code HarnessRunState.originalRequirement()} and traps the agent in a
     * deterministic plan_create retry loop.
     */
    private static String requirePinnedText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
