package org.ruoyi.service.coding.harness.plan.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.service.coding.harness.event.HarnessEventHub;
import org.ruoyi.service.coding.harness.loop.HarnessTranscriptReader;
import org.ruoyi.service.coding.harness.loop.tool.HarnessToolInvocationContext;
import org.ruoyi.service.coding.harness.model.HarnessEvent;
import org.ruoyi.service.coding.harness.model.HarnessApprovalPolicy;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessOwner;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessToolCall;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;
import org.ruoyi.service.coding.harness.model.HarnessToolEffectStatus;
import org.ruoyi.service.coding.harness.plan.AcceptanceCriterion;
import org.ruoyi.service.coding.harness.plan.ExecutionEvidence;
import org.ruoyi.service.coding.harness.plan.ExecutionMode;
import org.ruoyi.service.coding.harness.plan.PlanAggregate;
import org.ruoyi.service.coding.harness.plan.PlanApprovalCommand;
import org.ruoyi.service.coding.harness.plan.PlanReviewState;
import org.ruoyi.service.coding.harness.plan.PlanTaskStep;
import org.ruoyi.service.coding.harness.plan.StalePlanRevisionException;
import org.ruoyi.service.coding.harness.plan.TaskContract;
import org.ruoyi.service.coding.harness.runtime.HarnessSessionGate;
import org.ruoyi.service.coding.harness.store.HarnessStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Durable plan control plane used by bound model tools. All mutations are owner scoped, session
 * serialized, optimistic inside {@link PlanAggregate}, and saved before their UI event.
 */
@Service
@Slf4j
public class HarnessPlanCommandService {

    private static final int MAX_BOUNDED_PLAN_STEPS = 3;
    private static final int ITERATIONS_RESERVED_FOR_VERIFICATION = 3;
    private static final int MINIMUM_ITERATIONS_PER_PLAN_STEP = 4;

    private static final ObjectMapper EVIDENCE_JSON = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final int EVIDENCE_SCAN_PAGE_SIZE = 1_000;
    private static final String FINAL_REVIEW_BOUNDARY_KIND = "PLAN_VERIFY_BOUNDARY";
    private static final Set<String> PROCESS_ARGUMENTS = Set.of(
        "executable", "argv", "cwd", "timeoutMs", "stdin");
    private static final Set<String> LONG_RUNNING_EXECUTABLES = Set.of(
        "http-server", "serve", "vite", "nodemon");
    private static final Set<String> LONG_RUNNING_SCRIPT_NAMES = Set.of(
        "dev", "start", "serve", "watch");
    private static final Map<String, String> PROCESS_EXECUTABLE_ALIASES = Map.ofEntries(
        Map.entry("git.exe", "git"), Map.entry("java.exe", "java"),
        Map.entry("javac.exe", "javac"), Map.entry("mvn.cmd", "mvn"),
        Map.entry("mvn.exe", "mvn"), Map.entry("gradle.bat", "gradle"),
        Map.entry("node.exe", "node"), Map.entry("npm.cmd", "npm"),
        Map.entry("npx.cmd", "npx"), Map.entry("python.exe", "python"),
        Map.entry("python3.exe", "python3"), Map.entry("rg.exe", "rg"));
    private static final Set<String> FINAL_REVIEW_TOOLS = Set.of(
        "git_diff", "read_file", "read_source", "search_text");
    private static final Pattern REVIEW_DEFECT_CLAIM = Pattern.compile(
        "(?i)(?:remaining|real|blocking|product|implementation|production|unresolved|known)\\s+"
            + "(?:defect|defects|bug|bugs|issue|issues|gap|gaps|risk|risks)"
            + "|(?:requirement|contract|obligation)\\s+(?:is|was|remains?)\\s+"
            + "(?:not|unmet|unsatisfied|violated|broken)"
            + "|(?:is|are|remains?)\\s+(?:not\\s+met|unmet|unsatisfied|broken)"
            + "|counterevidence|must\\s+(?:fix|repair)|cannot\\s+(?:complete|pass)"
            + "|not\\s+blocking|non[- ]blocking");
    private static final Pattern REQUIRED_PROCESS_COMMAND = Pattern.compile(
        "(?im)^\\s*required\\s+(?:test|check|verification)\\s+command\\s*:\\s*`([^`\\r\\n]+)`\\s*$");
    private static final Pattern HTML_ID = Pattern.compile(
        "(?i)\\bid\\s*=\\s*[\"']([^\"']+)[\"']");
    private static final Pattern JS_ID_SELECTOR = Pattern.compile(
        "(?:\\$|querySelector(?:All)?)\\s*\\(\\s*[\"']#([A-Za-z][A-Za-z0-9_.:-]*)[\"']\\s*\\)");
    private static final Pattern JS_GET_BY_ID = Pattern.compile(
        "getElementById\\s*\\(\\s*[\"']([A-Za-z][A-Za-z0-9_.:-]*)[\"']\\s*\\)");
    private static final Pattern JS_DOLLAR_GET_BY_ID_ALIAS = Pattern.compile(
        "(?s)(?:const|let|var)\\s+\\$\\s*=.*?getElementById\\s*\\(");
    private static final Pattern JS_DOLLAR_BARE_ID = Pattern.compile(
        "\\$\\s*\\(\\s*[\"']([A-Za-z][A-Za-z0-9_.:-]*)[\"']\\s*\\)");
    private static final long MAX_FRONTEND_CONTRACT_BYTES = 2L * 1024 * 1024;

    private final HarnessStore store;
    private final HarnessEventHub eventHub;
    private final HarnessSessionGate sessionGate;
    private final Clock clock;

    @Autowired
    public HarnessPlanCommandService(HarnessStore store, HarnessEventHub eventHub,
                                     HarnessSessionGate sessionGate,
                                     HarnessTranscriptReader transcriptReader) {
        this(store, eventHub, sessionGate, transcriptReader, Clock.systemUTC());
    }

    HarnessPlanCommandService(HarnessStore store, HarnessEventHub eventHub,
                              HarnessSessionGate sessionGate,
                              HarnessTranscriptReader transcriptReader, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.eventHub = Objects.requireNonNull(eventHub, "eventHub");
        this.sessionGate = Objects.requireNonNull(sessionGate, "sessionGate");
        Objects.requireNonNull(transcriptReader, "transcriptReader");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public HarnessPlanTools bind(HarnessOwner owner, String sessionId, String runId) {
        return new HarnessPlanTools(this, owner, sessionId, runId);
    }

    PlanToolResult createPlan(HarnessOwner owner, String sessionId, String runId,
                              PlanDraftInput draft) {
        Objects.requireNonNull(draft, "draft");
        List<AcceptanceCriterion> criteria = draft.criteria().stream()
            .map(value -> new AcceptanceCriterion(value.id(), value.type(), value.expected(),
                normalizeCriterionEvidenceKey(value)))
            .toList();
        criteria.stream().filter(criterion -> !criterion.mechanicallyVerifiable()).findFirst()
            .ifPresent(criterion -> {
                throw new IllegalArgumentException("Acceptance criterion " + criterion.id()
                    + " is not mechanically verifiable; supported contract is type="
                    + AcceptanceCriterion.PROCESS_EXIT_TYPE + " and expected="
                    + AcceptanceCriterion.PROCESS_EXIT_ZERO
                    + " with a required canonical execute_process evidenceKey, or type="
                    + AcceptanceCriterion.FILE_MUTATION_TYPE + " and expected="
                    + AcceptanceCriterion.FILE_MUTATION_SUCCESS
                    + " with a required canonical workspace_file evidenceKey");
            });
        criteria.forEach(this::requireCanonicalCriterionKey);
        criteria.forEach(this::requireFiniteProcessCriterion);
        Set<String> roots = draft.allowedMutationRoots().isEmpty()
            ? Set.of(".") : Set.copyOf(draft.allowedMutationRoots());
        requireFileCriteriaWithinMutationRoots(criteria, roots);
        List<PlanTaskStep> steps = draft.steps().stream()
            .map(value -> PlanTaskStep.pending(value.stepId(), value.title(), value.instructions(),
                value.dependencyIds(), value.acceptanceCriterionIds()))
            .toList();
        requireEveryCriterionBound(criteria, steps);
        requireNoCriterionReuseAcrossDependencyChain(steps);
        Set<String> forbidden = Set.copyOf(draft.forbiddenOperations());

        return mutate(owner, sessionId, runId, "plan.created", run -> {
            int feasibleSteps = maxFeasiblePlanSteps(run.budget().maxIterations(),
                run.iteration());
            if (steps.size() > feasibleSteps) {
                throw new IllegalArgumentException("Plan has " + steps.size()
                    + " steps but the bounded run can complete at most " + feasibleSteps
                    + "; consolidate related files and checks into coarse end-to-end steps");
            }
            requirePinnedProcessCommands(run.originalRequirement(), criteria);
            PlanAggregate current = run.executionPlan();
            if (current == null) {
                TaskContract contract = TaskContract.create(draft.kind(), run.originalRequirement(),
                    criteria, roots, forbidden);
                PlanAggregate created = PlanAggregate.create(run.originalRequirement(), contract,
                    now()).replacePlan(draft.planMarkdown(), steps, now());
                return run.withExecutionPlan(applyApprovalPolicy(owner, sessionId, created), now());
            }
            requirePlanPhase(current, ExecutionMode.PLAN, "replace a plan draft");
            requireSameContract(current.contract(), draft.kind(), criteria, roots, forbidden);
            if (sameDraft(current, draft.planMarkdown(), steps)) {
                PlanAggregate policyApplied = applyApprovalPolicy(owner, sessionId, current);
                return policyApplied == current ? run : run.withExecutionPlan(policyApplied, now());
            }
            PlanAggregate replaced = current.replacePlan(draft.planMarkdown(), steps, now());
            return run.withExecutionPlan(applyApprovalPolicy(owner, sessionId, replaced), now());
        }, "Plan draft was recorded according to the session approval policy");
    }

    /**
     * Real provider traces show that a coarse step needs an action turn plus enough room for one
     * truncation/recovery or diagnose/recheck cycle. Reserve three further turns for VERIFY and
     * final verdict only for legacy runs with an explicit iteration budget. Unbounded tasks
     * must not inherit this estimate as a hidden plan-size limit.
     */
    static int maxFeasiblePlanSteps(int maxIterations, int usedIterations) {
        if (maxIterations == 0) {
            return Integer.MAX_VALUE;
        }
        int remaining = Math.max(0, maxIterations - usedIterations
            - ITERATIONS_RESERVED_FOR_VERIFICATION);
        int byBudget = Math.max(1, remaining / MINIMUM_ITERATIONS_PER_PLAN_STEP);
        return Math.min(MAX_BOUNDED_PLAN_STEPS, byBudget);
    }

    private PlanAggregate applyApprovalPolicy(HarnessOwner owner, String sessionId,
                                              PlanAggregate plan) {
        HarnessApprovalPolicy approvalPolicy = store.findSession(owner, sessionId)
            .orElseThrow(() -> new IllegalStateException("Harness session no longer exists"))
            .approvalPolicy();
        if (approvalPolicy != HarnessApprovalPolicy.NEVER
            || plan.reviewState() != PlanReviewState.AWAITING_APPROVAL) {
            return plan;
        }
        String idempotencyKey = "automatic-plan-approval-" + plan.taskId() + "-" + plan.revision();
        PlanAggregate approved = plan.approveFromControlPlane(new PlanApprovalCommand(plan.taskId(),
            plan.revision(), plan.canonicalHash(), idempotencyKey), now());
        // NEVER means the control plane already supplied approval. Starting the first ready step
        // in the same durable mutation removes a model-only transition turn without weakening the
        // approval boundary or allowing any workspace mutation before approval.
        return startFirstReadyApprovedStep(approved, now());
    }

    static PlanAggregate startFirstReadyApprovedStep(PlanAggregate approved, long now) {
        if (approved == null || approved.mode() != ExecutionMode.BUILD
            || approved.reviewState() != PlanReviewState.APPROVED
            || approved.inProgressStep().isPresent()) {
            return approved;
        }
        PlanTaskStep firstReady = approved.readySteps().stream().findFirst().orElse(null);
        return firstReady == null ? approved
            : approved.startStep(firstReady.stepId(), approved.revision(), now);
    }

    private String normalizeCriterionEvidenceKey(PlanCriterionInput input) {
        String key = input.evidenceKey();
        if (AcceptanceCriterion.PROCESS_EXIT_TYPE.equals(input.type())
            && key != null && key.strip().startsWith("{")) {
            // Structured-output models regularly preserve the exact JSON payload but omit the
            // literal evidence namespace shown in the prose description. The namespace is not
            // security data, so recover it server-side while still parsing, validating and
            // canonicalizing every executable/argv/cwd field fail-closed.
            return canonicalProcessKey(key.strip());
        }
        return key;
    }

    private void requireNoCriterionReuseAcrossDependencyChain(List<PlanTaskStep> steps) {
        Map<String, PlanTaskStep> byId = steps.stream().collect(
            java.util.stream.Collectors.toMap(PlanTaskStep::stepId, Function.identity()));
        for (int leftIndex = 0; leftIndex < steps.size(); leftIndex++) {
            PlanTaskStep left = steps.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < steps.size(); rightIndex++) {
                PlanTaskStep right = steps.get(rightIndex);
                Set<String> shared = new java.util.LinkedHashSet<>(
                    left.acceptanceCriterionIds());
                shared.retainAll(right.acceptanceCriterionIds());
                if (shared.isEmpty()) {
                    continue;
                }
                if (dependsTransitively(left, right.stepId(), byId, new java.util.HashSet<>())
                    || dependsTransitively(right, left.stepId(), byId,
                        new java.util.HashSet<>())) {
                    throw new IllegalArgumentException("Acceptance criteria " + shared
                        + " are bound to dependent steps " + left.stepId() + " and "
                        + right.stepId() + "; this creates a completion deadlock. Bind an "
                        + "aggregate test/check only to the last dependent step; intermediate "
                        + "steps may have no acceptance criteria");
                }
            }
        }
    }

    private void requireEveryCriterionBound(List<AcceptanceCriterion> criteria,
                                             List<PlanTaskStep> steps) {
        Set<String> bound = steps.stream()
            .flatMap(step -> step.acceptanceCriterionIds().stream())
            .collect(java.util.stream.Collectors.toSet());
        List<String> unbound = criteria.stream()
            .map(AcceptanceCriterion::id)
            .filter(id -> !bound.contains(id))
            .toList();
        if (!unbound.isEmpty()) {
            throw new IllegalArgumentException("Every acceptance criterion must be bound to at "
                + "least one plan step; unbound criteria: " + unbound);
        }
    }

    private boolean dependsTransitively(PlanTaskStep step, String target,
                                        Map<String, PlanTaskStep> byId,
                                        Set<String> visited) {
        if (!visited.add(step.stepId())) {
            return false;
        }
        if (step.dependencyIds().contains(target)) {
            return true;
        }
        return step.dependencyIds().stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .anyMatch(dependency -> dependsTransitively(dependency, target, byId, visited));
    }

    PlanToolResult updateStep(HarnessOwner owner, String sessionId, String runId,
                              PlanStepCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.action() == null) {
            throw new IllegalArgumentException("Plan step action is required");
        }
        return mutate(owner, sessionId, runId, "plan.step.updated", run -> {
            PlanAggregate plan = requirePlan(run);
            PlanAggregate next;
            if (command.action() == PlanStepAction.START) {
                next = plan.startStep(command.stepId(), command.expectedRevision(), now());
            } else if (command.action() == PlanStepAction.COMPLETE) {
                next = completeStepWithDiagnostics(plan, command);
            } else if (command.action() == PlanStepAction.BLOCK) {
                requireFailedCriterionEvidence(plan, command);
                next = plan.blockStep(command.stepId(), command.reason(),
                    command.expectedRevision(), now());
            } else if (command.action() == PlanStepAction.FAIL) {
                requireFailedCriterionEvidence(plan, command);
                next = plan.failStep(command.stepId(), command.reason(),
                    command.expectedRevision(), now());
            } else if (command.action() == PlanStepAction.RETRY) {
                next = plan.retryStep(command.stepId(), command.expectedRevision(), now());
            } else {
                next = plan.skipStep(command.stepId(), command.reason(),
                    command.expectedRevision(), now());
            }
            return next == plan ? run : run.withExecutionPlan(next, now());
        }, command.action() + " step " + command.stepId());
    }

    static void requireFailedCriterionEvidence(PlanAggregate plan, PlanStepCommand command) {
        PlanTaskStep step = plan.steps().stream()
            .filter(candidate -> candidate.stepId().equals(command.stepId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown plan step: " + command.stepId()));
        Set<String> referencedIds = Set.copyOf(command.evidenceIds());
        Set<String> criterionIds = Set.copyOf(step.acceptanceCriterionIds());
        Set<String> expectedKeys = plan.contract().criteria().stream()
            .filter(criterion -> criterionIds.contains(criterion.id()))
            .map(criterion -> criterion.type() + "\u0000" + criterion.evidenceKey())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        boolean supported = plan.evidence().stream()
            .filter(evidence -> referencedIds.contains(evidence.evidenceId()))
            .filter(evidence -> !evidence.successful())
            .anyMatch(evidence -> expectedKeys.contains(
                evidence.type() + "\u0000" + evidence.canonicalKey()));
        if (!supported) {
            throw new IllegalArgumentException(command.action()
                + " requires referenced failed mechanical evidence matching this step; "
                + "model prose is not blocker authority");
        }
    }

    private PlanAggregate completeStepWithDiagnostics(PlanAggregate plan,
                                                      PlanStepCommand command) {
        try {
            return plan.completeStep(command.stepId(), command.evidenceIds(),
                command.expectedRevision(), now());
        } catch (IllegalArgumentException failure) {
            if (!Objects.toString(failure.getMessage(), "")
                .startsWith("Step completion has unmet acceptance criteria:")) {
                throw failure;
            }
            PlanTaskStep step = plan.steps().stream()
                .filter(candidate -> candidate.stepId().equals(command.stepId()))
                .findFirst()
                .orElseThrow(() -> failure);
            List<AcceptanceCriterion> expected = plan.contract().criteria().stream()
                .filter(criterion -> step.acceptanceCriterionIds().contains(criterion.id()))
                .toList();
            Set<String> referencedIds = Set.copyOf(command.evidenceIds());
            List<ExecutionEvidence> referenced = plan.evidence().stream()
                .filter(evidence -> referencedIds.contains(evidence.evidenceId()))
                .filter(ExecutionEvidence::successful)
                .toList();
            String expectedKeys = expected.stream()
                .map(criterion -> criterion.id() + "=" + criterion.evidenceKey())
                .toList().toString();
            String suppliedKeys = referenced.stream()
                .map(evidence -> evidence.evidenceId() + "=" + evidence.canonicalKey())
                .toList().toString();
            throw new IllegalArgumentException(failure.getMessage()
                + "; expected canonical evidence: " + expectedKeys
                + "; referenced successful evidence: " + suppliedKeys
                + ". Reuse a matching evidenceId or execute the exact expected argv once; "
                + "do not rerun a successful non-matching command.", failure);
        }
    }

    public PlanToolResult recordToolEvidence(HarnessOwner owner, String sessionId, String runId,
                                             PlanEvidenceCommand command) {
        Objects.requireNonNull(command, "command");
        List<HarnessMessage> transcript = scanEvidenceTranscript(owner, sessionId, runId,
            command.toolCallId());
        HarnessMessage result = uniqueToolResult(transcript, command.toolCallId());
        EvidenceSource source = uniqueEvidenceSource(transcript, result, command.toolCallId());
        if (!Objects.equals(source.call().toolName(), result.toolName())) {
            throw new IllegalArgumentException("Durable tool result name does not match its "
                + "assistant tool call");
        }
        if (!Set.of("execute_process", "write_file", "replace_text")
            .contains(source.call().toolName())) {
            throw new IllegalArgumentException("Tool " + source.call().toolName()
                + " cannot produce mechanical PROCESS_EXIT evidence or FILE_MUTATION evidence");
        }

        String evidenceType;
        String canonicalKey;
        ProcessOutcome outcome;
        if ("execute_process".equals(source.call().toolName())) {
            evidenceType = AcceptanceCriterion.PROCESS_EXIT_TYPE;
            canonicalKey = canonicalProcessKey(source.call());
            outcome = processOutcome(result);
        } else {
            evidenceType = AcceptanceCriterion.FILE_MUTATION_TYPE;
            canonicalKey = canonicalWorkspaceFileKey(owner, sessionId, source.call());
            outcome = fileMutationOutcome(source.call(), result, canonicalKey);
        }
        String digest = sha256(source.assistant().messageId(), source.call().toolName(),
            source.call().arguments(), result.messageId(), result.toolName(),
            Objects.toString(result.content(), ""), Boolean.toString(result.toolError()),
            outcome.code(), outcome.actual());
        String evidenceId = "evidence-" + sha256(runId, source.assistant().messageId(),
            result.messageId(), digest).substring(0, 32);
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("toolCallId", result.toolCallId());
        attributes.put("toolName", result.toolName());
        attributes.put(AcceptanceCriterion.SOURCE_ASSISTANT_MESSAGE_ATTRIBUTE,
            source.assistant().messageId());
        attributes.put(AcceptanceCriterion.RESULT_MESSAGE_ATTRIBUTE, result.messageId());
        attributes.put(AcceptanceCriterion.ACTUAL_OUTCOME_ATTRIBUTE, outcome.actual());
        attributes.put("sourceArgumentsDigest", sha256(source.call().arguments()));
        attributes.put("code", outcome.code());
        ExecutionEvidence evidence = new ExecutionEvidence(evidenceId,
            evidenceType, canonicalKey, digest, outcome.successful(),
            evidenceSummary(result), attributes, result.timestamp());

        return mutate(owner, sessionId, runId, "plan.evidence.recorded", run -> {
            PlanAggregate plan = requirePlan(run);
            if (plan.mode() == ExecutionMode.VERIFY) {
                // BUILD criteria are already satisfied before VERIFY can begin. Final-review
                // probes belong to the review transcript, not the acceptance aggregate: adding
                // each exploratory command as evidence would increment the plan revision, create
                // a new review boundary, and hide the result from the independent reviewer.
                return run;
            }
            if (plan.evidence().stream().anyMatch(existing ->
                semanticallyEquivalent(existing, evidence))) {
                // Re-running the same verifier during final review must not create a new plan
                // revision and therefore move the review boundary past the result that just ran.
                // The full call/result remains in the transcript for audit; one semantic proof is
                // sufficient in the authoritative aggregate.
                return run;
            }
            PlanAggregate next = plan.recordEvidence(List.of(evidence),
                command.expectedRevision(), now());
            // Reconcile the evidence while its identity is still explicit. Comparing evidence
            // timestamps with plan.updatedAt is subtly wrong: recording any evidence advances
            // updatedAt, so a later background reconciliation can classify the evidence that just
            // repaired a review-failed step as stale forever. A FAILED step may reopen only when
            // this exact newly persisted evidence participates in satisfying its bound criteria.
            next = repairFailedStepWithNewEvidence(next, evidence.evidenceId());
            return applyPlanTransition(run, plan, next);
        }, "Recorded evidence from durable tool result " + command.toolCallId());
    }

    private boolean semanticallyEquivalent(ExecutionEvidence existing,
                                           ExecutionEvidence candidate) {
        return existing.type().equals(candidate.type())
            && existing.canonicalKey().equals(candidate.canonicalKey())
            && existing.successful() == candidate.successful()
            && Objects.equals(existing.attributes().get(
                AcceptanceCriterion.ACTUAL_OUTCOME_ATTRIBUTE), candidate.attributes().get(
                AcceptanceCriterion.ACTUAL_OUTCOME_ATTRIBUTE));
    }

    /**
     * Scans to the physical end of a shared session ledger without the generic transcript
     * reader's working-set/message-count cap. Only four matching records are retained: two source
     * or result records are already sufficient to reject ambiguous provenance. Unrelated history
     * therefore has constant heap cost even when the target run occurs after millions of records.
     */
    private List<HarnessMessage> scanEvidenceTranscript(HarnessOwner owner, String sessionId,
                                                        String runId, String toolCallId) {
        List<HarnessMessage> matches = new ArrayList<>(4);
        long cursor = 0;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                return List.copyOf(matches);
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness evidence scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (!runId.equals(message.runId())) {
                    continue;
                }
                boolean matchingResult = message.role() == HarnessMessageRole.TOOL
                    && toolCallId.equals(message.toolCallId());
                boolean matchingSource = message.role() == HarnessMessageRole.ASSISTANT
                    && message.toolCalls().stream().anyMatch(call ->
                        toolCallId.equals(call.toolCallId()));
                if ((matchingResult || matchingSource) && matches.size() < 4) {
                    matches.add(message);
                }
            }
        }
    }

    PlanToolResult verify(HarnessOwner owner, String sessionId, String runId,
                          PlanVerificationCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.action() == null) {
            throw new IllegalArgumentException("Verification action is required");
        }
        return mutate(owner, sessionId, runId, "plan.verification.updated", run -> {
            PlanAggregate plan = requirePlan(run);
            requireRevision(plan, command.expectedRevision());
            // BEGIN and FAIL are control decisions, not evidence claims. Models commonly echo a
            // tool-call id or a stale evidence id after a counterexample fails; parsing it here can
            // trap a correct reviewer in VERIFY forever. Only COMPLETE may claim persisted success
            // evidence and therefore receives strict id validation.
            List<ExecutionEvidence> selected = command.action() == PlanVerificationAction.COMPLETE
                ? selectEvidence(plan, command.evidenceIds()) : List.of();
            if (command.action() == PlanVerificationAction.BEGIN
                && !plan.allAcceptanceCriteriaMet()) {
                throw new IllegalStateException("Verification cannot begin; satisfy every "
                    + "acceptance criterion in BUILD first. Unmet criteria: "
                    + plan.contract().unmetCriteria(plan.evidence()).stream()
                        .map(AcceptanceCriterion::id).toList());
            }
            if (command.action() == PlanVerificationAction.COMPLETE) {
                requireFreshFinalReview(owner, sessionId, runId, plan);
            }
            PlanAggregate next;
            if (command.action() == PlanVerificationAction.BEGIN) {
                next = plan.beginVerification(now());
            } else if (command.action() == PlanVerificationAction.COMPLETE) {
                next = plan.completeVerification(selected, now());
            } else {
                next = plan.verificationFailed(selected, now());
            }
            return applyPlanTransition(run, plan, next);
        }, command.action() + " verification");
    }

    /** Runtime-authored FAIL used when VERIFY produces executable counterevidence. */
    public PlanToolResult failVerificationFromRejectedMutation(HarnessOwner owner,
                                                                String sessionId,
                                                                String runId) {
        return mutate(owner, sessionId, runId, "plan.verification.updated", run -> {
            PlanAggregate plan = requirePlan(run);
            if (plan.mode() != ExecutionMode.VERIFY) {
                return run;
            }
            return applyPlanTransition(run, plan,
                plan.verificationFailed(List.of(), now()));
        }, "verification failed after executable counterevidence");
    }

    /**
     * Advances mechanically satisfied build work. Entering VERIFY deliberately creates a new model
     * boundary: a green command or successful mutation proves the configured criterion, but it does
     * not prove that the final patch still matches every requirement. Completion therefore also
     * requires a successful repository inspection performed after VERIFY began. The model must
     * then explicitly choose COMPLETE or FAIL after receiving that inspection result; merely
     * invoking a read tool is not proof that the result was reviewed.
     */
    public PlanToolResult advanceMechanicallySatisfiedPlan(HarnessOwner owner, String sessionId,
                                                           String runId) {
        return mutate(owner, sessionId, runId, "plan.verification.updated", run -> {
            PlanAggregate plan = requirePlan(run);
            PlanAggregate next = advanceMechanicallySatisfiedPlan(plan, Set.of());
            return applyPlanTransition(run, plan, next);
        }, "mechanically satisfied plan work advanced");
    }

    private HarnessRunState applyPlanTransition(HarnessRunState run, PlanAggregate before,
                                                PlanAggregate after) {
        if (after == before) {
            return run;
        }
        long timestamp = now();
        HarnessRunState updated = run.withExecutionPlan(after, timestamp);
        boolean independentPhaseBoundary = before.mode() == ExecutionMode.BUILD
            && after.mode() == ExecutionMode.VERIFY
            || before.mode() == ExecutionMode.VERIFY
            && after.mode() == ExecutionMode.BUILD;
        return independentPhaseBoundary
            ? updated.withInspectionLedger(
                run.inspectionLedger().beginIndependentPhase(), timestamp)
            : updated;
    }

    /**
     * True only when the next model turn must decide COMPLETE or FAIL without more exploration.
     * This is deliberately not automatic completion: repository inspection and a green probe are
     * evidence, while the reviewer remains responsible for the verdict over the full contract.
     */
    public boolean verificationDecisionReady(HarnessOwner owner, String sessionId, String runId,
                                             PlanAggregate plan) {
        return plan != null && plan.mode() == ExecutionMode.VERIFY
            && plan.allAcceptanceCriteriaMet()
            && plan.steps().stream().allMatch(step -> step.status().isTerminal())
            && hasSuccessfulFinalProbeAfterReview(owner, sessionId, runId, plan)
            && !hasFailedVerificationCommand(owner, sessionId, runId, plan);
    }

    private PlanAggregate advanceMechanicallySatisfiedPlan(PlanAggregate plan,
                                                            Set<String> newlyRecordedEvidenceIds) {
        PlanAggregate next = plan;
        if (next.mode() != ExecutionMode.BUILD) {
            return next;
        }
        boolean advanced;
        do {
            advanced = false;
            PlanTaskStep candidate = next.inProgressStep().orElse(null);
            if (candidate == null) {
                PlanAggregate observed = next;
                candidate = observed.steps().stream()
                    .filter(step -> step.status()
                        == org.ruoyi.service.coding.harness.plan.PlanTaskStepStatus.FAILED)
                    .filter(step -> newlyRecordedEvidenceParticipates(observed, step,
                        newlyRecordedEvidenceIds))
                    .findFirst().orElse(null);
                if (candidate != null) {
                    next = next.retryStep(candidate.stepId(), next.revision(), now());
                } else {
                    candidate = observed.readySteps().stream()
                        .filter(step -> !satisfyingEvidenceIds(observed, step).isEmpty())
                        .findFirst().orElse(null);
                }
                if (candidate != null) {
                    next = next.startStep(candidate.stepId(), next.revision(), now());
                    candidate = next.inProgressStep().orElseThrow();
                }
            }
            if (candidate != null) {
                List<String> evidenceIds = satisfyingEvidenceIds(next, candidate);
                if (!evidenceIds.isEmpty()) {
                    next = next.completeStep(candidate.stepId(), evidenceIds,
                        next.revision(), now());
                    advanced = true;
                }
            }
        } while (advanced);

        boolean everyStepTerminal = next.steps().stream()
            .allMatch(step -> step.status().isTerminal());
        if (everyStepTerminal && next.contract().allCriteriaSatisfiedBy(next.evidence())) {
            next = next.beginVerification(now());
        }
        return next;
    }

    private PlanAggregate repairFailedStepWithNewEvidence(PlanAggregate plan,
                                                           String newlyRecordedEvidenceId) {
        if (plan.mode() != ExecutionMode.BUILD) {
            return plan;
        }
        PlanTaskStep failed = plan.steps().stream()
            .filter(step -> step.status()
                == org.ruoyi.service.coding.harness.plan.PlanTaskStepStatus.FAILED)
            .filter(step -> newlyRecordedEvidenceParticipates(plan, step,
                Set.of(newlyRecordedEvidenceId)))
            .findFirst().orElse(null);
        if (failed == null) {
            return plan;
        }
        PlanAggregate next = plan.retryStep(failed.stepId(), plan.revision(), now());
        next = next.startStep(failed.stepId(), next.revision(), now());
        List<String> evidenceIds = satisfyingEvidenceIds(next,
            next.inProgressStep().orElseThrow());
        next = next.completeStep(failed.stepId(), evidenceIds, next.revision(), now());
        return advanceMechanicallySatisfiedPlan(next, Set.of());
    }

    private void requireFreshFinalReview(HarnessOwner owner, String sessionId, String runId,
                                         PlanAggregate plan) {
        if (plan.mode() != ExecutionMode.VERIFY) {
            return;
        }
        if (hasFailedVerificationCommand(owner, sessionId, runId, plan)) {
            throw new IllegalStateException("A post-boundary verification command failed. "
                + "Call plan_verify FAIL to return to BUILD, repair the cause, and start a new "
                + "verification revision; a later narrative cannot turn failed executable "
                + "counterevidence into success");
        }
        String declaredDefect = latestReviewerDefectClaim(owner, sessionId, runId, plan);
        if (declaredDefect != null) {
            throw new IllegalStateException("The independent reviewer declared an unresolved "
                + "defect or unmet obligation after the current VERIFY boundary. Call plan_verify "
                + "FAIL and repair it before completion: " + declaredDefect);
        }
        requireFrontendDomIdContract(owner, sessionId);
        if (hasOnlySatisfiedProcessExitCriteria(plan)) {
            // PROCESS_EXIT evidence is already a first-party, finite command result. Requiring a
            // repository read after VERIFY begins is unrelated to this contract and deadlocks with
            // the phase registry, which deliberately exposes only plan_verify once that evidence
            // is conclusive.
            return;
        }
        if (!hasFreshFinalReview(owner, sessionId, runId, plan)) {
            throw new IllegalStateException("Complete verification only after a fresh successful "
                + "git_diff, read_source, read_file, or search_text result produced after VERIFY began");
        }
    }

    private boolean hasOnlySatisfiedProcessExitCriteria(PlanAggregate plan) {
        return !plan.contract().criteria().isEmpty()
            && plan.contract().criteria().stream().allMatch(criterion ->
                AcceptanceCriterion.PROCESS_EXIT_TYPE.equals(criterion.type()))
            && plan.contract().allCriteriaSatisfiedBy(plan.evidence());
    }

    /**
     * Cross-file DOM ids are cheap to validate mechanically and too consequential to leave to a
     * prose review. This bounded check intentionally covers only literal ids/selectors; dynamic
     * selectors remain the independent reviewer's responsibility.
     */
    private void requireFrontendDomIdContract(HarnessOwner owner, String sessionId) {
        String workspace = store.findSession(owner, sessionId)
            .orElseThrow(() -> new IllegalStateException("Harness session not found"))
            .workspace();
        Path root = Path.of(workspace).toAbsolutePath().normalize();
        Path htmlPath = root.resolve("public/index.html").normalize();
        Path scriptPath = root.resolve("public/app.js").normalize();
        if (!htmlPath.startsWith(root) || !scriptPath.startsWith(root)
            || !Files.isRegularFile(htmlPath) || !Files.isRegularFile(scriptPath)) {
            return;
        }
        try {
            if (Files.size(htmlPath) > MAX_FRONTEND_CONTRACT_BYTES
                || Files.size(scriptPath) > MAX_FRONTEND_CONTRACT_BYTES) {
                throw new IllegalStateException("Frontend DOM contract files exceed the bounded "
                    + "mechanical review size; call plan_verify FAIL and split or reduce them");
            }
            String html = Files.readString(htmlPath, StandardCharsets.UTF_8);
            String script = Files.readString(scriptPath, StandardCharsets.UTF_8);
            Set<String> ids = new java.util.LinkedHashSet<>();
            Set<String> duplicates = new java.util.LinkedHashSet<>();
            var htmlMatcher = HTML_ID.matcher(html);
            while (htmlMatcher.find()) {
                if (!ids.add(htmlMatcher.group(1))) {
                    duplicates.add(htmlMatcher.group(1));
                }
            }
            if (!duplicates.isEmpty()) {
                throw new IllegalStateException("Frontend DOM contract has duplicate HTML ids "
                    + duplicates + "; call plan_verify FAIL and repair the markup");
            }
            Set<String> referenced = new java.util.LinkedHashSet<>();
            var selectorMatcher = JS_ID_SELECTOR.matcher(script);
            while (selectorMatcher.find()) {
                referenced.add(selectorMatcher.group(1));
            }
            var byIdMatcher = JS_GET_BY_ID.matcher(script);
            while (byIdMatcher.find()) {
                referenced.add(byIdMatcher.group(1));
            }
            if (JS_DOLLAR_GET_BY_ID_ALIAS.matcher(script).find()) {
                var aliasMatcher = JS_DOLLAR_BARE_ID.matcher(script);
                while (aliasMatcher.find()) {
                    referenced.add(aliasMatcher.group(1));
                }
            }
            referenced.removeAll(ids);
            if (!referenced.isEmpty()) {
                throw new IllegalStateException("Frontend DOM contract references missing HTML ids "
                    + referenced + "; call plan_verify FAIL and repair HTML/JavaScript together");
            }
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Unable to inspect the frontend DOM contract", error);
        }
    }

    /**
     * A model may produce a green probe and still explicitly diagnose a real product defect in the
     * following assistant turn. Completion must not accept a contradictory narrative just because
     * the tool protocol is mechanically green. This is deliberately conservative: reviewers should
     * omit speculative "non-blocking" issue lists from a COMPLETE turn and use FAIL for real gaps.
     */
    private String latestReviewerDefectClaim(HarnessOwner owner, String sessionId, String runId,
                                             PlanAggregate plan) {
        long cursor = 0;
        long boundarySequence = -1;
        String latestClaim = null;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                return latestClaim;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness reviewer-claim scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (isFinalReviewBoundary(message, runId, plan)) {
                    boundarySequence = message.sequence();
                    latestClaim = null;
                    continue;
                }
                if (boundarySequence >= 0 && message.sequence() > boundarySequence
                    && runId.equals(message.runId())
                    && message.role() == HarnessMessageRole.ASSISTANT
                    && message.content() != null
                    && reviewerDeclaresDefect(message.content())) {
                    String normalized = message.content().strip().replaceAll("\\s+", " ");
                    latestClaim = normalized.length() <= 240
                        ? normalized : normalized.substring(0, 237) + "...";
                }
            }
        }
    }

    private boolean reviewerDeclaresDefect(String content) {
        String withoutBenignNegations = content.replaceAll(
            "(?i)\\b(?:no|zero)\\s+(?:remaining|known|unresolved)?\\s*"
                + "(?:defect|defects|bug|bugs|issue|issues|gap|gaps|risk|risks)\\b", "");
        return REVIEW_DEFECT_CLAIM.matcher(withoutBenignNegations).find();
    }

    private boolean scanAfterBoundary(HarnessOwner owner, String sessionId, String runId,
                                      PlanAggregate plan,
                                      java.util.function.Predicate<HarnessMessage> predicate) {
        long cursor = 0;
        long boundarySequence = -1;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                return false;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness review scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (isFinalReviewBoundary(message, runId, plan)) {
                    boundarySequence = message.sequence();
                } else if (boundarySequence >= 0 && message.sequence() > boundarySequence
                    && runId.equals(message.runId()) && predicate.test(message)) {
                    return true;
                }
            }
        }
    }

    private boolean hasFailedVerificationCommand(HarnessOwner owner, String sessionId,
                                                 String runId, PlanAggregate plan) {
        long cursor = 0;
        long boundarySequence = -1;
        boolean latestInlineProbeFailed = false;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                return latestInlineProbeFailed;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness failed-review scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (isFinalReviewBoundary(message, runId, plan)) {
                    boundarySequence = message.sequence();
                    continue;
                }
                if (boundarySequence >= 0
                    && runId.equals(message.runId())
                    && message.role() == HarnessMessageRole.TOOL
                    && message.sequence() > boundarySequence
                    && "execute_process".equals(message.toolName())
                    && isFailedProcessResult(message)) {
                    return true;
                }
                if (boundarySequence >= 0
                    && runId.equals(message.runId())
                    && message.role() == HarnessMessageRole.TOOL
                    && message.sequence() > boundarySequence
                    && "run_inline_probe".equals(message.toolName())
                    && isProcessResult(message)) {
                    // Inline probes are code written by the reviewer, not a pre-bound acceptance
                    // command. A later corrected probe may supersede a bad import, bad fixture, or
                    // impossible assertion. The last executable inline verdict is authoritative;
                    // an execute_process failure remains irrevocable for this review revision.
                    latestInlineProbeFailed = isFailedProcessResult(message);
                }
            }
        }
    }

    private boolean isFailedProcessResult(HarnessMessage message) {
        String stableCode = Objects.toString(message.metadata().get("code"), "");
        if (Set.of("PROCESS_EXIT_NONZERO", "PROCESS_TIMEOUT",
            "INLINE_PROBE_COUNTEREVIDENCE", "INLINE_PROBE_TIMEOUT")
            .contains(stableCode)) {
            return true;
        }
        if (message.content() == null || message.content().isBlank()) {
            return false;
        }
        try {
            JsonNode result = EVIDENCE_JSON.readTree(message.content());
            return result != null && result.isObject()
                && ((result.has("exitCode") && result.get("exitCode").canConvertToInt()
                    && result.get("exitCode").intValue() != 0)
                    || result.path("timedOut").asBoolean(false));
        } catch (JsonProcessingException notAProcessResult) {
            return false;
        }
    }

    private boolean isProcessResult(HarnessMessage message) {
        String stableCode = Objects.toString(message.metadata().get("code"), "");
        if (Set.of("PROCESS_EXIT_ZERO", "PROCESS_EXIT_NONZERO", "PROCESS_TIMEOUT",
            "INLINE_PROBE_COUNTEREVIDENCE", "INLINE_PROBE_TIMEOUT")
            .contains(stableCode)) {
            // Oversized structured results are replaced by a durable artifact pointer; the stable
            // registry-authored code remains inline and is sufficient to classify the outcome.
            return true;
        }
        if (message.content() == null || message.content().isBlank()) {
            return false;
        }
        try {
            JsonNode result = EVIDENCE_JSON.readTree(message.content());
            return result != null && result.isObject()
                && result.has("exitCode") && result.get("exitCode").canConvertToInt()
                && result.has("timedOut") && result.get("timedOut").isBoolean();
        } catch (JsonProcessingException notAProcessResult) {
            return false;
        }
    }

    private boolean hasSuccessfulFinalProbeAfterReview(HarnessOwner owner, String sessionId,
                                                       String runId, PlanAggregate plan) {
        long cursor = 0;
        long boundarySequence = -1;
        boolean reviewed = false;
        Boolean latestProbeSuccessful = null;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                return Boolean.TRUE.equals(latestProbeSuccessful);
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness convergence scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (isFinalReviewBoundary(message, runId, plan)) {
                    boundarySequence = message.sequence();
                    reviewed = false;
                    latestProbeSuccessful = null;
                    continue;
                }
                if (boundarySequence < 0 || message.sequence() <= boundarySequence
                    || !runId.equals(message.runId())
                    || message.role() != HarnessMessageRole.TOOL) {
                    continue;
                }
                if (!message.toolError() && FINAL_REVIEW_TOOLS.contains(message.toolName())
                    && message.content() != null && !message.content().isBlank()) {
                    reviewed = true;
                    continue;
                }
                if (reviewed && "run_inline_probe".equals(message.toolName())
                    && isProcessResult(message)) {
                    latestProbeSuccessful = !isFailedProcessResult(message)
                        && !message.toolError();
                }
            }
        }
    }

    /**
     * Scans the durable ledger with constant heap use. Sequence ordering is authoritative here;
     * wall clocks can move backwards or come from separately injected clocks in tests. A result
     * before the exact plan-revision boundary cannot satisfy this gate.
     */
    private boolean hasFreshFinalReview(HarnessOwner owner, String sessionId, String runId,
                                        PlanAggregate plan) {
        long cursor = 0;
        long boundarySequence = -1;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, sessionId, cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                return false;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness final-review scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (isFinalReviewBoundary(message, runId, plan)) {
                    boundarySequence = message.sequence();
                    continue;
                }
                if (boundarySequence >= 0
                    && runId.equals(message.runId())
                    && message.role() == HarnessMessageRole.TOOL
                    && message.sequence() > boundarySequence
                    && !message.toolError()
                    && FINAL_REVIEW_TOOLS.contains(message.toolName())
                    && message.content() != null
                    && !message.content().isBlank()) {
                    return true;
                }
            }
        }
    }

    private boolean isFinalReviewBoundary(HarnessMessage message, String runId,
                                          PlanAggregate plan) {
        return runId.equals(message.runId())
            && message.role() == HarnessMessageRole.CONTROL
            && FINAL_REVIEW_BOUNDARY_KIND.equals(message.metadata().get("kind"))
            && plan.taskId().toString().equals(Objects.toString(
                message.metadata().get("taskId"), ""))
            && Long.toString(plan.revision()).equals(Objects.toString(
                message.metadata().get("revision"), ""));
    }

    private void ensureFinalReviewBoundary(HarnessOwner owner, HarnessRunState run) {
        PlanAggregate plan = run.executionPlan();
        if (plan == null || plan.mode() != ExecutionMode.VERIFY) {
            return;
        }
        long cursor = 0;
        while (true) {
            List<HarnessMessage> page = store.readMessages(owner, run.sessionId(), cursor,
                EVIDENCE_SCAN_PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            for (HarnessMessage message : page) {
                if (message.sequence() <= cursor) {
                    throw new IllegalStateException(
                        "Harness final-review boundary scan returned a non-monotonic message page");
                }
                cursor = message.sequence();
                if (isFinalReviewBoundary(message, run.runId(), plan)) {
                    return;
                }
            }
        }
        String identity = plan.taskId() + "\u0000" + plan.revision();
        HarnessMessage boundary = new HarnessMessage(HarnessMessage.CURRENT_SCHEMA_VERSION,
            "plan-verify-boundary-" + sha256(identity).substring(0, 40), run.sessionId(),
            run.runId(), 0, HarnessMessageRole.CONTROL,
            finalReviewBoundaryContent(plan), null,
            List.of(), null, null, false, null,
            Map.of("kind", FINAL_REVIEW_BOUNDARY_KIND, "taskId", plan.taskId().toString(),
                "revision", Long.toString(plan.revision())), plan.updatedAt());
        store.appendMessage(owner, boundary);
    }

    private String finalReviewBoundaryContent(PlanAggregate plan) {
        boolean simplifiedChinese = plan.originalRequest() != null
            && plan.originalRequest().codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
        if (simplifiedChinese) {
            return "计划修订 " + plan.revision()
                + " 需要进行最终仓库审查。验证阶段所有面向用户的说明必须继续使用简体中文。";
        }
        return "Final repository review required for plan revision " + plan.revision();
    }

    private List<String> satisfyingEvidenceIds(PlanAggregate plan, PlanTaskStep step) {
        if (step.acceptanceCriterionIds().isEmpty()) {
            return List.of();
        }
        List<AcceptanceCriterion> criteria = plan.contract().criteria().stream()
            .filter(criterion -> step.acceptanceCriterionIds().contains(criterion.id()))
            .toList();
        if (criteria.size() != step.acceptanceCriterionIds().size()
            || criteria.stream().anyMatch(criterion -> plan.evidence().stream()
                .noneMatch(criterion::isSatisfiedBy))) {
            return List.of();
        }
        return plan.evidence().stream()
            .filter(evidence -> criteria.stream().anyMatch(criterion ->
                criterion.isSatisfiedBy(evidence)))
            .map(ExecutionEvidence::evidenceId)
            .distinct()
            .toList();
    }

    private boolean newlyRecordedEvidenceParticipates(PlanAggregate plan, PlanTaskStep step,
                                                       Set<String> newlyRecordedEvidenceIds) {
        List<String> satisfying = satisfyingEvidenceIds(plan, step);
        return !satisfying.isEmpty() && satisfying.stream().anyMatch(
            newlyRecordedEvidenceIds::contains);
    }

    private PlanToolResult mutate(HarnessOwner owner, String sessionId, String runId,
                                  String eventType,
                                  Function<HarnessRunState, HarnessRunState> mutation,
                                  String detail) {
        return sessionGate.withSession(owner, sessionId, () -> {
            HarnessRunState run = store.findRun(owner, sessionId, runId)
                .orElseThrow(() -> new IllegalArgumentException("Harness run not found"));
            HarnessRunState mutated = mutation.apply(run);
            boolean planChanged = mutated != run;
            PlanToolResult toolResult = result(requirePlan(mutated), detail);
            HarnessEvent planEvent = planChanged
                ? durablePlanEvent(mutated, eventType) : null;
            HarnessRunState next = commitInvocationReceipt(mutated, toolResult, planEvent);
            HarnessRunState saved = next == run ? run
                : store.saveRun(owner, next, run.revision());
            ensureFinalReviewBoundary(owner, saved);
            if (planEvent != null) {
                try {
                    eventHub.publish(owner, planEvent);
                    markControlEventPublished(owner, saved, planEvent);
                } catch (RuntimeException publicationFailure) {
                    // The exact event draft was committed with the tool receipt. Returning the
                    // successful tool result is authoritative; the run processor replays this
                    // outbox by event id and never converts an event outage into a false tool error.
                    log.warn("Deferred durable plan event {} for run {} after publication failed",
                        planEvent.eventId(), runId, publicationFailure);
                }
            }
            return toolResult;
        });
    }

    /**
     * Records the happy-path acknowledgement without making event delivery part of the tool's
     * success transaction. If the event append or this follow-up snapshot write fails, the
     * COMMITTED receipt still contains the exact draft and remains replayable by event id.
     */
    private void markControlEventPublished(HarnessOwner owner, HarnessRunState saved,
                                           HarnessEvent publishedEvent) {
        var invocation = HarnessToolInvocationContext.current().orElse(null);
        if (invocation == null) {
            return;
        }
        HarnessToolEffect effect = saved.toolEffects().get(invocation.toolCallId());
        if (effect == null || !effect.hasPendingControlEvent()
            || !effect.controlEvent().eventId().equals(publishedEvent.eventId())) {
            throw new IllegalStateException(
                "Published plan event does not match its durable control receipt");
        }
        HarnessRunState acknowledged = saved.withToolEffect(
            effect.markControlEventPublished(), now());
        try {
            store.saveRun(owner, acknowledged, saved.revision());
        } catch (RuntimeException acknowledgementFailure) {
            // Publishing already succeeded. Do not turn the committed tool success into an error;
            // restart reconciliation finds the stable event id and only repairs this marker.
            log.warn("Deferred published marker for control event {} on run {}",
                publishedEvent.eventId(), saved.runId(), acknowledgementFailure);
        }
    }

    /**
     * A plan tool mutates the run itself, so replaying it after a crash can violate optimistic
     * revisions or repeat a transition. The processor writes a PENDING effect before invoking the
     * registry. Bind that non-forgeable registry identity to the plan result and persist both the
     * mutation and its success receipt in the same saveRun call.
     */
    private HarnessRunState commitInvocationReceipt(HarnessRunState run,
                                                     PlanToolResult toolResult,
                                                     HarnessEvent planEvent) {
        var invocation = HarnessToolInvocationContext.current().orElse(null);
        if (invocation == null || !invocation.toolName().startsWith("plan_")) {
            // Direct control-plane calls (HTTP/tests) are not model tool invocations.
            return run;
        }
        HarnessToolEffect effect = run.toolEffects().get(invocation.toolCallId());
        if (effect == null) {
            throw new IllegalStateException("Plan tool invocation has no durable PENDING effect");
        }
        if (!effect.toolName().equals(invocation.toolName())
            || !effect.argumentsSha256().equals(invocation.argumentsSha256())) {
            throw new IllegalStateException("Plan tool invocation identity does not match its "
                + "durable effect");
        }
        if (effect.status() != HarnessToolEffectStatus.PENDING) {
            throw new IllegalStateException("Plan tool effect cannot be committed from "
                + effect.status());
        }
        return run.withToolEffect(
            effect.commit(serializeResult(toolResult), planEvent, now()), now());
    }

    private HarnessEvent durablePlanEvent(HarnessRunState run, String eventType) {
        PlanAggregate plan = requirePlan(run);
        var invocation = HarnessToolInvocationContext.current().orElse(null);
        HarnessToolEffect effect = invocation == null ? null
            : run.toolEffects().get(invocation.toolCallId());
        String identity = effect == null
            ? "direct\u0000" + run.runId() + "\u0000" + eventType + "\u0000" + plan.revision()
            : effect.effectId() + "\u0000" + invocation.toolCallId() + "\u0000" + eventType;
        String eventId = "control-event-" + sha256(identity).substring(0, 40);
        Map<String, Object> data = Map.of("taskId", plan.taskId().toString(),
            "revision", plan.revision(), "hash", plan.canonicalHash(),
            "mode", plan.mode().name(), "reviewState", plan.reviewState().name());
        return new HarnessEvent(HarnessEvent.CURRENT_SCHEMA_VERSION, eventId,
            run.sessionId(), run.runId(), 0, now(), eventType, null,
            invocation == null ? null : invocation.toolCallId(), null, data);
    }

    private String serializeResult(PlanToolResult result) {
        try {
            return EVIDENCE_JSON.writeValueAsString(result);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Unable to serialize durable plan tool result",
                impossible);
        }
    }

    private PlanToolResult result(PlanAggregate plan, String detail) {
        return new PlanToolResult(plan.taskId().toString(), plan.revision(), plan.canonicalHash(),
            plan.mode(), plan.reviewState(),
            plan.readySteps().stream().map(PlanTaskStep::stepId).toList(),
            plan.inProgressStep().map(PlanTaskStep::stepId).orElse(null),
            plan.evidence().stream().map(ExecutionEvidence::evidenceId).toList(), detail);
    }

    private PlanAggregate requirePlan(HarnessRunState run) {
        if (run.executionPlan() == null) {
            throw new IllegalStateException("Create an authoritative plan first");
        }
        return run.executionPlan();
    }

    private void requirePlanPhase(PlanAggregate plan, ExecutionMode expected, String action) {
        if (plan.mode() != expected) {
            throw new IllegalStateException("Cannot " + action + " while plan mode is "
                + plan.mode());
        }
    }

    private void requireRevision(PlanAggregate plan, long expectedRevision) {
        if (plan.revision() != expectedRevision) {
            throw new StalePlanRevisionException(expectedRevision, plan.revision());
        }
    }

    private void requireSameContract(TaskContract contract, String kind,
                                     List<AcceptanceCriterion> criteria, Set<String> roots,
                                     Set<String> forbidden) {
        if (!contract.kind().equals(kind == null ? "" : kind.strip())
            || !contract.criteria().equals(criteria)
            || !contract.allowedMutationRoots().equals(roots)
            || !contract.forbiddenOperations().equals(forbidden)) {
            throw new IllegalStateException(
                "Acceptance and mutation contract is immutable after plan creation");
        }
    }

    private boolean sameDraft(PlanAggregate current, String markdown,
                              List<PlanTaskStep> steps) {
        return current.planMarkdown().equals(markdown == null ? "" : markdown.strip())
            && current.steps().equals(steps);
    }

    private List<ExecutionEvidence> selectEvidence(PlanAggregate plan, List<String> ids) {
        List<String> requested = ids == null ? List.of() : ids;
        if (requested.isEmpty()) {
            return plan.evidence();
        }
        Map<String, ExecutionEvidence> byId = new LinkedHashMap<>();
        plan.evidence().forEach(value -> byId.put(value.evidenceId(), value));
        List<ExecutionEvidence> selected = new ArrayList<>();
        for (String id : requested) {
            ExecutionEvidence evidence = byId.get(id);
            if (evidence == null) {
                throw new IllegalArgumentException("Unknown plan evidence: " + id);
            }
            selected.add(evidence);
        }
        return List.copyOf(selected);
    }

    private HarnessMessage uniqueToolResult(List<HarnessMessage> transcript, String toolCallId) {
        List<HarnessMessage> matches = transcript.stream()
            .filter(message -> message.role() == HarnessMessageRole.TOOL)
            .filter(message -> Objects.equals(toolCallId, message.toolCallId()))
            .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "No durable tool result exists for " + toolCallId);
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException(
                "Tool result provenance is ambiguous for " + toolCallId);
        }
        return matches.get(0);
    }

    private EvidenceSource uniqueEvidenceSource(List<HarnessMessage> transcript,
                                                HarnessMessage result, String toolCallId) {
        List<EvidenceSource> matches = new ArrayList<>();
        for (HarnessMessage message : transcript) {
            if (message.role() != HarnessMessageRole.ASSISTANT) {
                continue;
            }
            for (HarnessToolCall call : message.toolCalls()) {
                if (toolCallId.equals(call.toolCallId())) {
                    matches.add(new EvidenceSource(message, call));
                }
            }
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "No durable assistant tool call exists for " + toolCallId);
        }
        if (matches.size() != 1 || matches.get(0).assistant().sequence() >= result.sequence()) {
            throw new IllegalArgumentException(
                "Assistant tool-call provenance is ambiguous or out of order for " + toolCallId);
        }
        return matches.get(0);
    }

    private String canonicalProcessKey(HarnessToolCall call) {
        return canonicalProcessKey(call.arguments());
    }

    private void requireCanonicalCriterionKey(AcceptanceCriterion criterion) {
        String supplied = criterion.evidenceKey();
        if (AcceptanceCriterion.FILE_MUTATION_TYPE.equals(criterion.type())) {
            String canonical = canonicalWorkspaceFileKey(supplied.substring(
                AcceptanceCriterion.FILE_CANONICAL_KEY_PREFIX.length()));
            if (!supplied.equals(canonical)) {
                throw new IllegalArgumentException("Acceptance criterion " + criterion.id()
                    + " evidenceKey is not the canonical workspace file path");
            }
            return;
        }
        String arguments = supplied.substring(
            AcceptanceCriterion.PROCESS_CANONICAL_KEY_PREFIX.length());
        String canonical = canonicalProcessKey(arguments);
        if (!supplied.equals(canonical)) {
            throw new IllegalArgumentException("Acceptance criterion " + criterion.id()
                + " evidenceKey is not the canonical execute_process invocation");
        }
    }

    /** A server/watch process cannot ever satisfy a PROCESS_EXIT exitCode=0 contract. */
    private void requireFiniteProcessCriterion(AcceptanceCriterion criterion) {
        if (!AcceptanceCriterion.PROCESS_EXIT_TYPE.equals(criterion.type())) {
            return;
        }
        String arguments = criterion.evidenceKey().substring(
            AcceptanceCriterion.PROCESS_CANONICAL_KEY_PREFIX.length());
        JsonNode command = readJsonObject(arguments, "execute_process arguments");
        String executable = canonicalProcessExecutable(
            requiredText(command.get("executable"), "executable")).toLowerCase(Locale.ROOT);
        List<String> argv = new ArrayList<>();
        command.path("argv").forEach(value -> argv.add(value.asText().toLowerCase(Locale.ROOT)));
        if (isKnownLongRunningCommand(executable, argv)) {
            throw new IllegalArgumentException("Acceptance criterion " + criterion.id()
                + " binds a long-lived server/watch command that cannot satisfy PROCESS_EXIT "
                + "exitCode=0. Bind a finite syntax/test command instead; server behavior must "
                + "be checked by a bounded probe.");
        }
    }

    private boolean isKnownLongRunningCommand(String executable, List<String> argv) {
        if (LONG_RUNNING_EXECUTABLES.contains(executable)
            || argv.stream().anyMatch(argument -> Set.of("--watch", "--watchall", "-w")
                .contains(argument))) {
            return true;
        }
        if (Set.of("python", "python3", "py").contains(executable)) {
            return argv.size() >= 2 && "-m".equals(argv.get(0))
                && "http.server".equals(argv.get(1));
        }
        if ("npx".equals(executable) && !argv.isEmpty()) {
            String packageName = argv.get(0);
            return LONG_RUNNING_EXECUTABLES.contains(packageName)
                || "next".equals(packageName) && argv.size() >= 2
                    && "dev".equals(argv.get(1))
                || "webpack".equals(packageName) && argv.size() >= 2
                    && "serve".equals(argv.get(1));
        }
        if (Set.of("npm", "pnpm", "yarn", "bun").contains(executable)) {
            int scriptIndex = !argv.isEmpty() && "run".equals(argv.get(0)) ? 1 : 0;
            return argv.size() > scriptIndex
                && LONG_RUNNING_SCRIPT_NAMES.contains(argv.get(scriptIndex));
        }
        return "next".equals(executable) && argv.contains("dev")
            || "webpack".equals(executable) && argv.contains("serve");
    }

    /** A requirement-pinned executable check is immutable contract data, not a model choice. */
    private void requirePinnedProcessCommands(String requirement,
                                              List<AcceptanceCriterion> criteria) {
        var matcher = REQUIRED_PROCESS_COMMAND.matcher(Objects.toString(requirement, ""));
        while (matcher.find()) {
            String command = matcher.group(1).strip();
            List<String> tokens = splitRequiredCommand(command);
            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("Required test command must not be empty");
            }
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("executable", tokens.get(0));
            arguments.put("argv", tokens.subList(1, tokens.size()));
            arguments.put("cwd", ".");
            String requiredKey;
            try {
                requiredKey = canonicalProcessKey(EVIDENCE_JSON.writeValueAsString(arguments));
            } catch (JsonProcessingException impossible) {
                throw new IllegalStateException("Unable to canonicalize required test command",
                    impossible);
            }
            boolean present = criteria.stream().anyMatch(criterion ->
                AcceptanceCriterion.PROCESS_EXIT_TYPE.equals(criterion.type())
                    && requiredKey.equals(criterion.evidenceKey()));
            if (!present) {
                throw new IllegalArgumentException("Plan must bind the exact Required test command `"
                    + command + "` as PROCESS_EXIT evidence; do not substitute another script or "
                    + "package-manager command");
            }
        }
    }

    private List<String> splitRequiredCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < command.length(); index++) {
            char character = command.charAt(index);
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    current.append(character);
                }
            } else if (character == '\'' || character == '"') {
                quote = character;
            } else if (Character.isWhitespace(character)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("Required test command has an unclosed quote");
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return List.copyOf(tokens);
    }

    private void requireFileCriteriaWithinMutationRoots(List<AcceptanceCriterion> criteria,
                                                        Set<String> roots) {
        List<String> canonicalRoots = roots.stream()
            .map(this::canonicalMutationRoot)
            .toList();
        for (AcceptanceCriterion criterion : criteria) {
            if (!AcceptanceCriterion.FILE_MUTATION_TYPE.equals(criterion.type())) {
                continue;
            }
            String path = criterion.evidenceKey().substring(
                AcceptanceCriterion.FILE_CANONICAL_KEY_PREFIX.length());
            boolean allowed = canonicalRoots.stream().anyMatch(root -> ".".equals(root)
                || path.equals(root) || path.startsWith(root + "/"));
            if (!allowed) {
                throw new IllegalArgumentException("Acceptance criterion " + criterion.id()
                    + " requires mutation of " + path + " outside allowedMutationRoots "
                    + canonicalRoots + "; read-only tests and instructions must not be modeled "
                    + "as FILE_MUTATION evidence");
            }
        }
    }

    private String canonicalMutationRoot(String rawRoot) {
        String root = Objects.toString(rawRoot, "").strip().replace('\\', '/');
        while (root.startsWith("./")) {
            root = root.substring(2);
        }
        if (".".equals(root)) {
            return root;
        }
        String canonical = canonicalWorkspaceFileKey(root);
        return canonical.substring(AcceptanceCriterion.FILE_CANONICAL_KEY_PREFIX.length());
    }

    private String canonicalProcessKey(String argumentJson) {
        JsonNode arguments = readJsonObject(argumentJson, "execute_process arguments");
        arguments.fieldNames().forEachRemaining(name -> {
            if (!PROCESS_ARGUMENTS.contains(name)) {
                throw new IllegalArgumentException(
                    "Unknown execute_process evidence argument: " + name);
            }
        });
        String executable = canonicalProcessExecutable(
            requiredText(arguments.get("executable"), "executable"));
        JsonNode argvNode = arguments.get("argv");
        if (argvNode == null || !argvNode.isArray()) {
            throw new IllegalArgumentException("execute_process argv must be an array");
        }
        List<String> argv = new ArrayList<>();
        for (JsonNode argument : argvNode) {
            argv.add(requiredText(argument, "argv entry"));
        }
        JsonNode cwdNode = arguments.get("cwd");
        String cwd = cwdNode == null || cwdNode.isNull() || cwdNode.asText().isBlank()
            ? "." : requiredText(cwdNode, "cwd");

        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("executable", executable);
        canonical.put("argv", argv);
        canonical.put("cwd", cwd);
        JsonNode stdinNode = arguments.get("stdin");
        if (stdinNode != null && !stdinNode.isNull()) {
            if (!stdinNode.isTextual()) {
                throw new IllegalArgumentException("execute_process stdin must be a string");
            }
            canonical.put("stdin", stdinNode.textValue());
        }
        JsonNode timeoutNode = arguments.get("timeoutMs");
        if (timeoutNode != null && !timeoutNode.isNull()) {
            if (!timeoutNode.isIntegralNumber() || !timeoutNode.canConvertToLong()
                || timeoutNode.longValue() <= 0) {
                throw new IllegalArgumentException(
                    "execute_process timeoutMs must be a positive integer");
            }
            // timeoutMs is an execution safety deadline, not part of the verifier's semantic
            // identity. The same command may be run under a tighter runtime deadline without
            // invalidating an acceptance criterion that binds executable/argv/cwd.
        }
        try {
            return AcceptanceCriterion.PROCESS_CANONICAL_KEY_PREFIX
                + EVIDENCE_JSON.writeValueAsString(canonical);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Unable to canonicalize execute_process arguments",
                impossible);
        }
    }

    private String canonicalProcessExecutable(String executable) {
        if (executable.contains("/") || executable.contains("\\")) {
            return executable;
        }
        return PROCESS_EXECUTABLE_ALIASES.getOrDefault(
            executable.toLowerCase(Locale.ROOT), executable);
    }

    private String canonicalWorkspaceFileKey(HarnessOwner owner, String sessionId,
                                             HarnessToolCall call) {
        JsonNode arguments = readJsonObject(call.arguments(), call.toolName() + " arguments");
        String rawPath = requiredText(arguments.get("path"), "path");
        Path supplied = Path.of(rawPath);
        if (!supplied.isAbsolute()) {
            return canonicalWorkspaceFileKey(rawPath);
        }
        Path workspace = Path.of(store.findSession(owner, sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Harness session not found"))
            .workspace()).toAbsolutePath().normalize();
        Path absolute = supplied.normalize();
        if (!absolute.startsWith(workspace)) {
            throw new IllegalArgumentException(
                "workspace_file path cannot escape the session workspace");
        }
        return canonicalWorkspaceFileKey(workspace.relativize(absolute).toString());
    }

    private String canonicalWorkspaceFileKey(String rawPath) {
        String path = Objects.toString(rawPath, "").strip().replace('\\', '/');
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        if (path.isBlank() || path.startsWith("/") || path.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("workspace_file path must be relative");
        }
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/", -1)) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("workspace_file path cannot escape the workspace");
            }
            segments.add(segment);
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("workspace_file path must name a file");
        }
        return AcceptanceCriterion.FILE_CANONICAL_KEY_PREFIX + String.join("/", segments);
    }

    private ProcessOutcome fileMutationOutcome(HarnessToolCall call, HarnessMessage result,
                                                String sourceKey) {
        String code = requiredMetadataText(result, "code");
        if (result.toolError() || !"ok".equals(code)) {
            throw new IllegalArgumentException(
                "A failed file mutation cannot produce successful acceptance evidence");
        }
        JsonNode content = readJsonObject(result.content(), call.toolName() + " result");
        if (content.path("offloaded").asBoolean(false)) {
            throw new IllegalArgumentException("File mutation identity cannot be offloaded");
        }
        String resultKey = canonicalWorkspaceFileKey(requiredText(content.get("path"), "path"));
        String sha256 = requiredText(content.get("sha256"), "sha256");
        JsonNode size = content.get("sizeBytes");
        JsonNode created = content.get("created");
        if (!sourceKey.equals(resultKey) || !sha256.matches("[0-9a-f]{64}")
            || size == null || !size.isIntegralNumber() || !size.canConvertToLong()
            || size.longValue() < 0 || created == null || !created.isBoolean()) {
            throw new IllegalArgumentException(
                "File mutation result does not match its canonical source path or identity");
        }
        return new ProcessOutcome(true, AcceptanceCriterion.FILE_MUTATION_SUCCESS, code);
    }

    private ProcessOutcome processOutcome(HarnessMessage result) {
        String code = requiredMetadataText(result, "code");
        JsonNode content = readJsonObject(result.content(), "execute_process result");
        if (content.path("offloaded").asBoolean(false)) {
            return offloadedProcessOutcome(result, content, code);
        }
        JsonNode exitCodeNode = content.get("exitCode");
        JsonNode timedOutNode = content.get("timedOut");
        if (exitCodeNode == null || !exitCodeNode.isIntegralNumber()
            || !exitCodeNode.canConvertToInt() || timedOutNode == null
            || !timedOutNode.isBoolean()) {
            throw new IllegalArgumentException(
                "execute_process result lacks a mechanical exit outcome");
        }
        int exitCode = exitCodeNode.intValue();
        boolean timedOut = timedOutNode.booleanValue();
        boolean successful = !timedOut && exitCode == 0;
        String expectedCode = timedOut ? "PROCESS_TIMEOUT"
            : successful ? "PROCESS_EXIT_ZERO" : "PROCESS_EXIT_NONZERO";
        if (result.toolError() == successful || !expectedCode.equals(code)) {
            throw new IllegalArgumentException(
                "execute_process result error/code disagrees with its exit outcome");
        }
        String actual = timedOut ? "timedOut=true" : "exitCode=" + exitCode;
        return new ProcessOutcome(successful, actual, code);
    }

    private ProcessOutcome offloadedProcessOutcome(HarnessMessage result, JsonNode content,
                                                    String code) {
        String originalCode = requiredText(content.get("originalCode"), "originalCode");
        JsonNode originalError = content.get("originalError");
        String artifactId = requiredText(content.get("artifactId"), "artifactId");
        Object storedArtifactId = result.metadata().get("artifactId");
        if (originalError == null || !originalError.isBoolean()
            || originalError.booleanValue() != result.toolError()
            || !originalCode.equals(code) || storedArtifactId == null
            || !artifactId.equals(storedArtifactId.toString())) {
            throw new IllegalArgumentException(
                "Offloaded execute_process result has inconsistent provenance");
        }
        return switch (code) {
            case "PROCESS_EXIT_ZERO" -> {
                if (result.toolError()) {
                    throw new IllegalArgumentException(
                        "Successful execute_process result cannot be marked as an error");
                }
                yield new ProcessOutcome(true, AcceptanceCriterion.PROCESS_EXIT_ZERO, code);
            }
            case "PROCESS_EXIT_NONZERO" -> {
                if (!result.toolError()) {
                    throw new IllegalArgumentException(
                        "Non-zero execute_process result must be marked as an error");
                }
                yield new ProcessOutcome(false, "exitCode=nonzero", code);
            }
            case "PROCESS_TIMEOUT" -> {
                if (!result.toolError()) {
                    throw new IllegalArgumentException(
                        "Timed-out execute_process result must be marked as an error");
                }
                yield new ProcessOutcome(false, "timedOut=true", code);
            }
            default -> throw new IllegalArgumentException(
                "Offloaded result is not a completed execute_process outcome");
        };
    }

    private JsonNode readJsonObject(String json, String description) {
        try {
            JsonNode parsed = EVIDENCE_JSON.readTree(json);
            if (parsed == null || !parsed.isObject()) {
                throw new IllegalArgumentException(description + " must be one JSON object");
            }
            return parsed;
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException(description + " is not valid JSON", error);
        }
    }

    private String requiredMetadataText(HarnessMessage result, String key) {
        Object value = result.metadata().get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                "execute_process result metadata " + key + " is required");
        }
        return text;
    }

    private String requiredText(JsonNode value, String field) {
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException(field + " must be non-blank text");
        }
        return value.textValue();
    }

    private String evidenceSummary(HarnessMessage result) {
        String content = Objects.toString(result.content(), "").replaceAll("\\s+", " ").strip();
        if (content.length() > 500) {
            content = content.substring(0, 500) + "…";
        }
        return result.toolName() + (result.toolError() ? " failed: " : " succeeded: ")
            + (content.isBlank() ? "(no output)" : content);
    }

    private String sha256(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private long now() {
        return clock.millis();
    }

    private record EvidenceSource(HarnessMessage assistant, HarnessToolCall call) { }

    private record ProcessOutcome(boolean successful, String actual, String code) { }
}
