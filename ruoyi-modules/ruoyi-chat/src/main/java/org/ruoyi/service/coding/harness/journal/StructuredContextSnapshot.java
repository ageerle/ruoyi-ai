package org.ruoyi.service.coding.harness.journal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A bounded, immutable projection of the facts required to safely resume or audit a coding run.
 * The snapshot contains no mutable run state; a failed journal export therefore cannot affect a
 * caller's execution state.
 */
public record StructuredContextSnapshot(
    String runId,
    String checkpointId,
    long checkpointSequence,
    long capturedAtEpochMillis,
    String objective,
    String checkpointSummary,
    List<String> checkpointArtifactIds,
    List<String> acceptanceCriteria,
    List<String> constraints,
    List<PlanStatus> plan,
    List<Decision> decisions,
    List<Repository> repositories,
    List<ChangedFile> changedFiles,
    List<CommandEvidence> commands,
    List<ResolvedError> resolvedErrors,
    List<String> blockers,
    List<String> unknownEffects,
    String nextAction,
    List<ArtifactReference> artifacts,
    String snapshotHash,
    String contextHash,
    Map<String, String> metadata
) {

    public static final int MAX_LIST_ITEMS = 128;
    public static final int MAX_TEXT_LENGTH = 8_192;
    public static final int MAX_SHORT_TEXT_LENGTH = 1_024;
    public static final int MAX_METADATA_ENTRIES = 32;
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Pattern HASH = Pattern.compile("[A-Fa-f0-9]{8,128}");

    public StructuredContextSnapshot {
        runId = requireId(runId, "runId");
        checkpointId = requireId(checkpointId, "checkpointId");
        if (checkpointSequence < 0) {
            throw new IllegalArgumentException("checkpointSequence must not be negative");
        }
        if (capturedAtEpochMillis <= 0) {
            throw new IllegalArgumentException("capturedAtEpochMillis must be positive");
        }
        objective = requireText(objective, "objective", MAX_TEXT_LENGTH);
        checkpointSummary = optionalText(checkpointSummary, MAX_TEXT_LENGTH);
        checkpointArtifactIds = immutableTexts(checkpointArtifactIds,
            "checkpointArtifactIds", MAX_SHORT_TEXT_LENGTH);
        acceptanceCriteria = immutableTexts(acceptanceCriteria, "acceptanceCriteria", MAX_SHORT_TEXT_LENGTH);
        constraints = immutableTexts(constraints, "constraints", MAX_SHORT_TEXT_LENGTH);
        plan = immutableRecords(plan, "plan");
        decisions = immutableRecords(decisions, "decisions");
        repositories = immutableRecords(repositories, "repositories");
        changedFiles = immutableRecords(changedFiles, "changedFiles");
        commands = immutableRecords(commands, "commands");
        resolvedErrors = immutableRecords(resolvedErrors, "resolvedErrors");
        blockers = immutableTexts(blockers, "blockers", MAX_SHORT_TEXT_LENGTH);
        unknownEffects = immutableTexts(unknownEffects, "unknownEffects", MAX_SHORT_TEXT_LENGTH);
        nextAction = optionalText(nextAction, MAX_TEXT_LENGTH);
        artifacts = immutableRecords(artifacts, "artifacts");
        snapshotHash = requireHash(snapshotHash, "snapshotHash");
        contextHash = requireHash(contextHash, "contextHash");
        metadata = immutableMetadata(metadata);
    }

    public record PlanStatus(String stepId, String status, String summary, String evidence) {
        public PlanStatus {
            stepId = requireText(stepId, "plan stepId", 128);
            status = requireText(status, "plan status", 64);
            summary = requireText(summary, "plan summary", MAX_SHORT_TEXT_LENGTH);
            evidence = optionalText(evidence, MAX_SHORT_TEXT_LENGTH);
        }
    }

    public record Decision(String decision, String rationale) {
        public Decision {
            decision = requireText(decision, "decision", MAX_SHORT_TEXT_LENGTH);
            rationale = requireText(rationale, "rationale", MAX_TEXT_LENGTH);
        }
    }

    public record Repository(String repositoryId, String relativePath, String revision, String stateHash) {
        public Repository {
            repositoryId = requireId(repositoryId, "repositoryId");
            relativePath = requireRelativePath(relativePath, "repository relativePath");
            revision = optionalText(revision, 256);
            stateHash = optionalHash(stateHash, "repository stateHash");
        }
    }

    public record ChangedFile(String repositoryId, String relativePath, List<String> symbols,
                              String contentHash, String changeSummary) {
        public ChangedFile {
            repositoryId = requireId(repositoryId, "changed file repositoryId");
            relativePath = requireRelativePath(relativePath, "changed file relativePath");
            symbols = immutableTexts(symbols, "changed file symbols", 256);
            contentHash = optionalHash(contentHash, "changed file contentHash");
            changeSummary = requireText(changeSummary, "changed file changeSummary", MAX_SHORT_TEXT_LENGTH);
        }
    }

    /** {@code test == null} means the durable evidence did not classify the command. */
    public record CommandEvidence(String command, int exitCode, String evidence, Boolean test) {
        public CommandEvidence {
            command = requireText(command, "command", MAX_TEXT_LENGTH);
            if (exitCode < -1 || exitCode > 255) {
                throw new IllegalArgumentException("command exitCode must be between -1 and 255");
            }
            evidence = requireText(evidence, "command evidence", MAX_TEXT_LENGTH);
        }
    }

    public record ResolvedError(String error, String resolution, String evidence) {
        public ResolvedError {
            error = requireText(error, "resolved error", MAX_TEXT_LENGTH);
            resolution = requireText(resolution, "error resolution", MAX_TEXT_LENGTH);
            evidence = optionalText(evidence, MAX_TEXT_LENGTH);
        }
    }

    public record ArtifactReference(String artifactId, String relativeHandle, String contentHash,
                                    String mediaType, long byteSize) {
        public ArtifactReference {
            artifactId = requireId(artifactId, "artifactId");
            relativeHandle = requireRelativePath(relativeHandle, "artifact relativeHandle");
            contentHash = requireHash(contentHash, "artifact contentHash");
            mediaType = requireText(mediaType, "artifact mediaType", 128);
            if (byteSize < 0) {
                throw new IllegalArgumentException("artifact byteSize must not be negative");
            }
        }
    }

    private static String requireId(String value, String name) {
        if (value == null || !SAFE_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be a safe identifier");
        }
        return value;
    }

    private static String requireHash(String value, String name) {
        if (value == null || !HASH.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be a hexadecimal hash");
        }
        return value.toLowerCase();
    }

    private static String optionalHash(String value, String name) {
        return value == null || value.isBlank() ? "" : requireHash(value, name);
    }

    private static String requireRelativePath(String value, String name) {
        String path = requireText(value, name, MAX_SHORT_TEXT_LENGTH);
        // Schema-v1 sessions represent their single lease root as ".". It is a descriptive
        // manifest value, not a filesystem traversal segment, and must remain visible in an
        // operator snapshot without being rewritten to a made-up repository path.
        if (".".equals(path)) {
            return path;
        }
        if (path.startsWith("/") || path.startsWith("\\") || path.contains("\\") || path.contains(":")) {
            throw new IllegalArgumentException(name + " must be a safe relative path");
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")
                || segment.indexOf('\u0000') >= 0) {
                throw new IllegalArgumentException(name + " contains an unsafe path segment");
            }
        }
        return path;
    }

    private static String requireText(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return boundedRedacted(value.trim(), name, maxLength);
    }

    private static String optionalText(String value, int maxLength) {
        return value == null || value.isBlank() ? "" : boundedRedacted(value.trim(), "text", maxLength);
    }

    private static String boundedRedacted(String value, String name, int maxLength) {
        String safe = JournalSecretRedactor.redact(value);
        if (safe.length() > maxLength) {
            throw new IllegalArgumentException(name + " exceeds " + maxLength + " characters");
        }
        return safe;
    }

    private static List<String> immutableTexts(Collection<String> values, String name, int maxLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        requireListSize(values.size(), name);
        List<String> copy = new ArrayList<>(values.size());
        for (String value : values) {
            copy.add(requireText(value, name + " item", maxLength));
        }
        return List.copyOf(copy);
    }

    private static <T> List<T> immutableRecords(Collection<T> values, String name) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        requireListSize(values.size(), name);
        List<T> copy = new ArrayList<>(values.size());
        for (T value : values) {
            copy.add(Objects.requireNonNull(value, name + " contains null"));
        }
        return List.copyOf(copy);
    }

    private static void requireListSize(int size, String name) {
        if (size > MAX_LIST_ITEMS) {
            throw new IllegalArgumentException(name + " exceeds " + MAX_LIST_ITEMS + " items");
        }
    }

    private static Map<String, String> immutableMetadata(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        if (values.size() > MAX_METADATA_ENTRIES) {
            throw new IllegalArgumentException("metadata exceeds " + MAX_METADATA_ENTRIES + " entries");
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
            String key = requireText(entry.getKey(), "metadata key", 128);
            if (!key.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}")) {
                throw new IllegalArgumentException("metadata key must be a safe identifier");
            }
            copy.put(key, optionalText(entry.getValue(), MAX_SHORT_TEXT_LENGTH));
        });
        return Map.copyOf(copy);
    }
}
