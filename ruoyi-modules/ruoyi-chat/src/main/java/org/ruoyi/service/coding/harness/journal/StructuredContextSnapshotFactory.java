package org.ruoyi.service.coding.harness.journal;

import org.ruoyi.service.coding.harness.model.HarnessContextCheckpoint;
import org.ruoyi.service.coding.harness.model.HarnessModelEffect;
import org.ruoyi.service.coding.harness.model.HarnessModelEffectStatus;
import org.ruoyi.service.coding.harness.model.HarnessPlanStep;
import org.ruoyi.service.coding.harness.model.HarnessPlanStepStatus;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessRunStatus;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;
import org.ruoyi.service.coding.harness.model.HarnessToolEffectStatus;
import org.ruoyi.service.coding.harness.model.WorkspaceManifestProject;
import org.ruoyi.service.coding.harness.plan.AcceptanceCriterion;
import org.ruoyi.service.coding.harness.plan.ExecutionEvidence;
import org.ruoyi.service.coding.harness.plan.PlanAggregate;
import org.ruoyi.service.coding.harness.plan.PlanTaskStep;
import org.ruoyi.service.coding.harness.plan.PlanTaskStepStatus;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a bounded operator projection exclusively from already-durable Harness state.
 *
 * <p>The factory deliberately leaves decisions, resolved errors, repository revisions and file
 * content hashes empty unless an authoritative durable field exists. In particular, an evidence
 * digest is provenance for a verifier result and is never mislabeled as a repository or file
 * revision.</p>
 */
public final class StructuredContextSnapshotFactory {

    private static final String PROCESS_PREFIX = AcceptanceCriterion.PROCESS_CANONICAL_KEY_PREFIX;
    private static final String FILE_PREFIX = AcceptanceCriterion.FILE_CANONICAL_KEY_PREFIX;
    private static final Pattern NUMERIC_EXIT = Pattern.compile("exitCode=(-?\\d+)");
    private static final String TRUNCATED = "…[truncated]";

    public StructuredContextSnapshot create(HarnessRunState run, HarnessSessionState session) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(session, "session");
        validateOwnership(run, session);
        HarnessContextCheckpoint checkpoint = run.contextCheckpoint();
        if (checkpoint == null || checkpoint.isEmpty() || checkpoint.checkpointId() == null) {
            throw new IllegalArgumentException(
                "A durable, identified context checkpoint is required for journal projection");
        }

        String objective = bounded(run.originalRequirement(),
            StructuredContextSnapshot.MAX_TEXT_LENGTH);
        String checkpointSummary = boundedOptional(checkpoint.summary(),
            StructuredContextSnapshot.MAX_TEXT_LENGTH);
        List<String> checkpointArtifactIds = boundedTexts(checkpoint.artifactIds(),
            StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
        List<String> criteria = acceptanceCriteria(run.executionPlan());
        List<String> constraints = constraints(run.executionPlan(), checkpoint);
        List<StructuredContextSnapshot.PlanStatus> plan = plan(run);
        List<StructuredContextSnapshot.Repository> repositories = repositories(session);
        List<StructuredContextSnapshot.ChangedFile> changedFiles = changedFiles(
            run.executionPlan(), session);
        List<StructuredContextSnapshot.CommandEvidence> commands = commands(run.executionPlan());
        List<String> blockers = blockers(run);
        List<String> unknownEffects = unknownEffects(run);
        String nextAction = nextAction(run);
        Map<String, String> metadata = metadata(run, session, checkpoint);
        long capturedAt = checkpoint.createdAt() > 0 ? checkpoint.createdAt() : run.updatedAt();

        String contextHash = contextHash(checkpoint, checkpointSummary, checkpointArtifactIds);
        String snapshotHash = snapshotHash(run, session, checkpoint, capturedAt, objective,
            checkpointSummary, checkpointArtifactIds, criteria, constraints, plan, repositories,
            changedFiles, commands, blockers, unknownEffects, nextAction, contextHash, metadata);

        return new StructuredContextSnapshot(run.runId(), checkpoint.checkpointId(),
            checkpoint.toSequence(), capturedAt, objective, checkpointSummary,
            checkpointArtifactIds, criteria, constraints, plan,
            // No durable decision or resolved-error aggregate exists yet. Empty is truthful.
            List.of(), repositories, changedFiles, commands, List.of(), blockers, unknownEffects,
            nextAction,
            // Checkpoints currently retain artifact IDs, but not handles/media/size. Do not invent them.
            List.of(), snapshotHash, contextHash, metadata);
    }

    private static void validateOwnership(HarnessRunState run, HarnessSessionState session) {
        if (!run.sessionId().equals(session.sessionId())
            || !run.tenantId().equals(session.tenantId())
            || !run.userId().equals(session.userId())) {
            throw new IllegalArgumentException("Run and session do not share the same owner and identity");
        }
    }

    private static List<String> acceptanceCriteria(PlanAggregate executionPlan) {
        if (executionPlan == null) {
            return List.of();
        }
        return executionPlan.contract().criteria().stream()
            .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
            .map(criterion -> {
                String evidence = criterion.evidenceKey() == null ? ""
                    : " (evidence=" + criterion.evidenceKey() + ")";
                return bounded(criterion.id() + " [" + criterion.type() + "]: "
                        + criterion.expected() + evidence,
                    StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
            })
            .toList();
    }

    private static List<String> constraints(PlanAggregate executionPlan,
                                            HarnessContextCheckpoint checkpoint) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String constraint : checkpoint.securityConstraints()) {
            addBounded(values, constraint, StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
        }
        if (executionPlan != null) {
            for (String root : executionPlan.contract().allowedMutationRoots()) {
                addBounded(values, "Allowed mutation root: " + root,
                    StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
            }
            for (String operation : executionPlan.contract().forbiddenOperations()) {
                addBounded(values, "Forbidden operation: " + operation,
                    StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
            }
        }
        return values.stream().limit(StructuredContextSnapshot.MAX_LIST_ITEMS).toList();
    }

    private static List<StructuredContextSnapshot.PlanStatus> plan(HarnessRunState run) {
        if (run.executionPlan() != null) {
            Map<String, ExecutionEvidence> evidence = new LinkedHashMap<>();
            run.executionPlan().evidence().forEach(item -> evidence.put(item.evidenceId(), item));
            return run.executionPlan().steps().stream()
                .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
                .map(step -> new StructuredContextSnapshot.PlanStatus(
                    bounded(step.stepId(), 128), step.status().name(), planSummary(step),
                    evidenceSummary(step.completionEvidenceIds(), evidence)))
                .toList();
        }
        return run.plan().steps().stream()
            .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
            .map(step -> new StructuredContextSnapshot.PlanStatus(
                bounded(step.stepId(), 128), step.status().name(), legacyPlanSummary(step),
                boundedOptional(String.join("; ", step.evidence()),
                    StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH)))
            .toList();
    }

    private static String planSummary(PlanTaskStep step) {
        List<String> facts = new ArrayList<>();
        facts.add(step.title());
        if (!step.instructions().isBlank()) {
            facts.add(step.instructions());
        }
        if (step.statusReason() != null) {
            facts.add("Status reason: " + step.statusReason());
        }
        return bounded(String.join(" — ", facts), StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
    }

    private static String legacyPlanSummary(HarnessPlanStep step) {
        List<String> facts = new ArrayList<>();
        facts.add(step.title());
        if (step.description() != null && !step.description().isBlank()) {
            facts.add(step.description());
        }
        if (step.failureReason() != null && !step.failureReason().isBlank()) {
            facts.add("Failure: " + step.failureReason());
        }
        if (step.replanReason() != null && !step.replanReason().isBlank()) {
            facts.add("Replan: " + step.replanReason());
        }
        return bounded(String.join(" — ", facts), StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
    }

    private static String evidenceSummary(List<String> evidenceIds,
                                          Map<String, ExecutionEvidence> evidence) {
        List<String> summaries = new ArrayList<>();
        for (String evidenceId : evidenceIds) {
            ExecutionEvidence item = evidence.get(evidenceId);
            if (item != null) {
                summaries.add(item.evidenceId() + ": " + item.summary());
            }
        }
        return boundedOptional(String.join("; ", summaries),
            StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
    }

    private static List<StructuredContextSnapshot.Repository> repositories(
        HarnessSessionState session) {
        return session.workspaceManifest().projects().stream()
            .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
            .map(project -> new StructuredContextSnapshot.Repository(project.projectId(),
                project.relativePath(), "", ""))
            .toList();
    }

    private static List<StructuredContextSnapshot.ChangedFile> changedFiles(
        PlanAggregate executionPlan, HarnessSessionState session) {
        if (executionPlan == null) {
            return List.of();
        }
        LinkedHashMap<String, StructuredContextSnapshot.ChangedFile> changed =
            new LinkedHashMap<>();
        for (ExecutionEvidence evidence : executionPlan.evidence()) {
            if (!evidence.successful()
                || !AcceptanceCriterion.FILE_MUTATION_TYPE.equals(evidence.type())
                || !evidence.canonicalKey().startsWith(FILE_PREFIX)) {
                continue;
            }
            String workspacePath = evidence.canonicalKey().substring(FILE_PREFIX.length());
            ResolvedProjectFile resolved = resolveProjectFile(
                session.workspaceManifest().projects(), workspacePath);
            if (resolved == null) {
                continue;
            }
            StructuredContextSnapshot.ChangedFile projection =
                new StructuredContextSnapshot.ChangedFile(resolved.projectId(),
                    resolved.relativePath(), List.of(), "",
                    bounded(evidence.summary(), StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH));
            changed.put(resolved.projectId() + "\u0000" + resolved.relativePath(), projection);
            if (changed.size() >= StructuredContextSnapshot.MAX_LIST_ITEMS) {
                break;
            }
        }
        return List.copyOf(changed.values());
    }

    private static ResolvedProjectFile resolveProjectFile(
        List<WorkspaceManifestProject> projects, String workspacePath) {
        List<WorkspaceManifestProject> candidates = projects.stream()
            .sorted(Comparator.comparingInt((WorkspaceManifestProject project) ->
                ".".equals(project.relativePath()) ? 0 : project.relativePath().length())
                .reversed())
            .toList();
        for (WorkspaceManifestProject project : candidates) {
            if (".".equals(project.relativePath())) {
                return new ResolvedProjectFile(project.projectId(), workspacePath);
            }
            String prefix = project.relativePath() + "/";
            if (workspacePath.startsWith(prefix) && workspacePath.length() > prefix.length()) {
                return new ResolvedProjectFile(project.projectId(),
                    workspacePath.substring(prefix.length()));
            }
        }
        return null;
    }

    private static List<StructuredContextSnapshot.CommandEvidence> commands(
        PlanAggregate executionPlan) {
        if (executionPlan == null) {
            return List.of();
        }
        return executionPlan.evidence().stream()
            .filter(evidence -> AcceptanceCriterion.PROCESS_EXIT_TYPE.equals(evidence.type()))
            .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
            .map(evidence -> new StructuredContextSnapshot.CommandEvidence(
                bounded(stripProcessPrefix(evidence.canonicalKey()),
                    StructuredContextSnapshot.MAX_TEXT_LENGTH),
                exitCode(evidence),
                bounded(evidence.summary(), StructuredContextSnapshot.MAX_TEXT_LENGTH),
                null))
            .toList();
    }

    private static String stripProcessPrefix(String value) {
        return value.startsWith(PROCESS_PREFIX) ? value.substring(PROCESS_PREFIX.length()) : value;
    }

    private static int exitCode(ExecutionEvidence evidence) {
        String actual = evidence.attributes().get(AcceptanceCriterion.ACTUAL_OUTCOME_ATTRIBUTE);
        if (actual == null) {
            return -1;
        }
        Matcher matcher = NUMERIC_EXIT.matcher(actual);
        if (!matcher.matches()) {
            return -1;
        }
        try {
            int parsed = Integer.parseInt(matcher.group(1));
            return parsed >= 0 && parsed <= 255 ? parsed : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static List<String> blockers(HarnessRunState run) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        PlanAggregate executionPlan = run.executionPlan();
        if (executionPlan != null) {
            addBounded(blockers, executionPlan.blockedReason(),
                StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
            addBounded(blockers, executionPlan.failureReason(),
                StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
        }
        if ((run.status() == HarnessRunStatus.SUSPENDED
            || run.status() == HarnessRunStatus.FAILED) && run.error() != null) {
            addBounded(blockers, run.error(), StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH);
        }
        return blockers.stream().limit(StructuredContextSnapshot.MAX_LIST_ITEMS).toList();
    }

    private static List<String> unknownEffects(HarnessRunState run) {
        List<String> unknown = new ArrayList<>();
        run.toolEffects().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .filter(entry -> entry.getValue().status() == HarnessToolEffectStatus.PENDING)
            .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
            .forEach(entry -> unknown.add(bounded("Pending tool effect: "
                    + entry.getValue().toolName() + " (call " + entry.getKey() + ")",
                StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH)));
        HarnessModelEffect modelEffect = run.modelEffect();
        if (modelEffect != null && modelEffect.status() == HarnessModelEffectStatus.PENDING
            && unknown.size() < StructuredContextSnapshot.MAX_LIST_ITEMS) {
            unknown.add(bounded("Pending model effect: iteration " + modelEffect.iteration(),
                StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH));
        }
        return List.copyOf(unknown);
    }

    private static String nextAction(HarnessRunState run) {
        if (run.executionPlan() != null) {
            return run.executionPlan().steps().stream()
                .filter(step -> step.status() == PlanTaskStepStatus.IN_PROGRESS
                    || step.status() == PlanTaskStepStatus.PENDING)
                .findFirst()
                .map(StructuredContextSnapshotFactory::planSummary)
                .orElse("");
        }
        return run.plan().steps().stream()
            .filter(step -> step.status() == HarnessPlanStepStatus.IN_PROGRESS
                || step.status() == HarnessPlanStepStatus.PENDING)
            .findFirst()
            .map(StructuredContextSnapshotFactory::legacyPlanSummary)
            .orElse("");
    }

    private static Map<String, String> metadata(HarnessRunState run,
                                                HarnessSessionState session,
                                                HarnessContextCheckpoint checkpoint) {
        TreeMap<String, String> values = new TreeMap<>();
        values.put("run.status", run.status().name());
        values.put("run.iteration", Integer.toString(run.iteration()));
        values.put("run.toolCallCount", Integer.toString(run.toolCallCount()));
        values.put("run.revision", Long.toString(run.revision()));
        values.put("session.revision", Long.toString(session.revision()));
        values.put("session.permissionMode", session.permissionMode().name());
        values.put("session.approvalPolicy", session.approvalPolicy().name());
        values.put("checkpoint.inputTokensBefore", Long.toString(checkpoint.inputTokensBefore()));
        values.put("checkpoint.inputTokensAfter", Long.toString(checkpoint.inputTokensAfter()));
        values.put("checkpoint.lineageDepth", Integer.toString(checkpoint.lineage().size()));
        if (checkpoint.modelIdentity() != null) {
            values.put("checkpoint.model", bounded(checkpoint.modelIdentity(),
                StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH));
        }
        if (run.modelRoute() != null) {
            values.put("model.selected", bounded(run.modelRoute().selectedModel(),
                StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH));
            values.put("model.taskClass", run.modelRoute().taskClass().name());
            values.put("model.thinkingEnabled", Boolean.toString(run.modelRoute().thinkingEnabled()));
            values.put("model.routeSource", run.modelRoute().source().name());
        }
        if (run.executionPlan() != null) {
            values.put("plan.revision", Long.toString(run.executionPlan().revision()));
            values.put("plan.mode", run.executionPlan().mode().name());
            values.put("plan.reviewState", run.executionPlan().reviewState().name());
        } else {
            values.put("plan.revision", Long.toString(run.plan().revision()));
        }
        EnumMap<HarnessToolEffectStatus, Integer> counts =
            new EnumMap<>(HarnessToolEffectStatus.class);
        for (HarnessToolEffect effect : run.toolEffects().values()) {
            counts.merge(effect.status(), 1, Integer::sum);
        }
        counts.forEach((status, count) -> values.put(
            "effects.tool." + status.name().toLowerCase(), Integer.toString(count)));
        if (run.modelEffect() != null) {
            values.put("effects.model.status", run.modelEffect().status().name());
        }
        return Map.copyOf(values);
    }

    private static String contextHash(HarnessContextCheckpoint checkpoint,
                                      String checkpointSummary,
                                      List<String> checkpointArtifactIds) {
        DigestBuilder digest = new DigestBuilder("harness-journal-context-v1");
        digest.add(checkpoint.checkpointId());
        checkpoint.lineage().forEach(digest::add);
        digest.add(checkpoint.fromSequence());
        digest.add(checkpoint.toSequence());
        digest.add(checkpoint.compactedThroughMessageSequence());
        digest.add(checkpointSummary);
        checkpointArtifactIds.forEach(digest::add);
        digest.add(checkpoint.inputTokensBefore());
        digest.add(checkpoint.inputTokensAfter());
        digest.add(checkpoint.modelIdentity());
        digest.add(checkpoint.sourceUsageTimestamp());
        checkpoint.securityConstraints().stream()
            .map(value -> bounded(value, StructuredContextSnapshot.MAX_SHORT_TEXT_LENGTH))
            .forEach(digest::add);
        digest.add(checkpoint.createdAt());
        return digest.hex();
    }

    private static String snapshotHash(HarnessRunState run, HarnessSessionState session,
                                       HarnessContextCheckpoint checkpoint, long capturedAt,
                                       String objective, String checkpointSummary,
                                       List<String> checkpointArtifactIds,
                                       List<String> criteria, List<String> constraints,
                                       List<StructuredContextSnapshot.PlanStatus> plan,
                                       List<StructuredContextSnapshot.Repository> repositories,
                                       List<StructuredContextSnapshot.ChangedFile> changedFiles,
                                       List<StructuredContextSnapshot.CommandEvidence> commands,
                                       List<String> blockers, List<String> unknownEffects,
                                       String nextAction, String contextHash,
                                       Map<String, String> metadata) {
        DigestBuilder digest = new DigestBuilder("harness-journal-snapshot-v1");
        digest.add(run.runId());
        digest.add(session.sessionId());
        digest.add(checkpoint.checkpointId());
        digest.add(checkpoint.toSequence());
        digest.add(capturedAt);
        digest.add(objective);
        digest.add(checkpointSummary);
        checkpointArtifactIds.forEach(digest::add);
        criteria.forEach(digest::add);
        constraints.forEach(digest::add);
        plan.forEach(value -> {
            digest.add(value.stepId());
            digest.add(value.status());
            digest.add(value.summary());
            digest.add(value.evidence());
        });
        repositories.forEach(value -> {
            digest.add(value.repositoryId());
            digest.add(value.relativePath());
            digest.add(value.revision());
            digest.add(value.stateHash());
        });
        changedFiles.forEach(value -> {
            digest.add(value.repositoryId());
            digest.add(value.relativePath());
            value.symbols().forEach(digest::add);
            digest.add(value.contentHash());
            digest.add(value.changeSummary());
        });
        commands.forEach(value -> {
            digest.add(value.command());
            digest.add(value.exitCode());
            digest.add(value.evidence());
            digest.add(Objects.toString(value.test(), ""));
        });
        blockers.forEach(digest::add);
        unknownEffects.forEach(digest::add);
        digest.add(nextAction);
        digest.add(contextHash);
        new TreeMap<>(metadata).forEach((key, value) -> {
            digest.add(key);
            digest.add(value);
        });
        return digest.hex();
    }

    private static List<String> boundedTexts(List<String> values, int maxLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).filter(value -> !value.isBlank())
            .limit(StructuredContextSnapshot.MAX_LIST_ITEMS)
            .map(value -> bounded(value, maxLength)).toList();
    }

    private static void addBounded(LinkedHashSet<String> values, String value, int maxLength) {
        if (value != null && !value.isBlank()
            && values.size() < StructuredContextSnapshot.MAX_LIST_ITEMS) {
            values.add(bounded(value, maxLength));
        }
    }

    private static String boundedOptional(String value, int maxLength) {
        return value == null || value.isBlank() ? "" : bounded(value, maxLength);
    }

    private static String bounded(String value, int maxLength) {
        String normalized = JournalSecretRedactor.redact(Objects.requireNonNull(value, "value").strip());
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - TRUNCATED.length()) + TRUNCATED;
    }

    private record ResolvedProjectFile(String projectId, String relativePath) {
    }

    /** Length-prefixes every value so the digest cannot be changed by delimiter ambiguity. */
    private static final class DigestBuilder {
        private final MessageDigest digest;

        private DigestBuilder(String schema) {
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("SHA-256 is unavailable", impossible);
            }
            add(schema);
        }

        private void add(long value) {
            add(Long.toString(value));
        }

        private void add(int value) {
            add(Integer.toString(value));
        }

        private void add(boolean value) {
            add(Boolean.toString(value));
        }

        private void add(String value) {
            byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            digest.update(bytes);
        }

        private String hex() {
            return java.util.HexFormat.of().formatHex(digest.digest());
        }
    }
}
