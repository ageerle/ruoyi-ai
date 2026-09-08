package org.ruoyi.service.coding.harness.context;

import org.ruoyi.service.coding.harness.model.HarnessContextCheckpoint;
import org.ruoyi.service.coding.harness.model.HarnessMessage;
import org.ruoyi.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.service.coding.harness.model.HarnessToolCall;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure context-domain service. It chooses a token-based safe prefix and delegates only the
 * summary text generation; persistence and provider calls remain outside this package.
 */
public final class ContextEngine {

    static final int MAX_ARTIFACT_HANDLES = 256;
    static final int MAX_ARTIFACT_HANDLE_BYTES = 32 * 1_024;

    private final Summarizer summarizer;
    private final TokenEstimator tokenEstimator;
    private final ContextEnginePolicy policy;

    public ContextEngine(Summarizer summarizer, TokenEstimator tokenEstimator) {
        this(summarizer, tokenEstimator, ContextEnginePolicy.defaults());
    }

    public ContextEngine(Summarizer summarizer, TokenEstimator tokenEstimator,
                         ContextEnginePolicy policy) {
        this.summarizer = Objects.requireNonNull(summarizer, "summarizer");
        this.tokenEstimator = Objects.requireNonNull(tokenEstimator, "tokenEstimator");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public ContextWindow project(ContextState state, ContextTokenBudget budget) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(budget, "budget");
        // Pins are integrity metadata for summary validation and checkpoint identity. They are not
        // rendered as a separate provider message: the objective is present in the USER ledger (or
        // its summary), while the current plan/permission/security contract is already rendered by
        // the caller's system prompt and charged through ContextTokenBudget.systemPromptTokens.
        // Counting the pins here charged the same provider input twice and could make a late repair
        // turn fail compaction even though the actual request still fit.
        long pinTokens = 0;
        long summaryTokens = add(estimateText(state.checkpoint().summary()),
            estimateArtifactHandles(state.checkpoint().artifactIds()));
        long messageTokens = estimateMessages(state.workingMessages());
        long inputTokens = add(add(pinTokens, summaryTokens), messageTokens);
        return new ContextWindow(budget.usableInputTokens(), budget.reservedTokens(),
            pinTokens, summaryTokens, messageTokens, inputTokens,
            inputTokens > budget.usableInputTokens());
    }

    public ContextCompactionResult compact(ContextState state, ContextTokenBudget budget,
                                           CompactionRequest request) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(request, "request");

        ContextWindow before = project(state, budget);
        if (!request.emergency() && !before.overBudget()) {
            return result(ContextCompactionStatus.NOT_NEEDED, state, List.of(), before,
                "working set fits usable input capacity");
        }
        if (state.compactionControl().circuitOpen()) {
            return result(ContextCompactionStatus.CIRCUIT_OPEN, state, List.of(), before,
                "three consecutive compaction failures opened the circuit");
        }
        if (request.emergency()
            && state.compactionControl().emergencyAttempted(request.overflowId())) {
            return result(ContextCompactionStatus.EMERGENCY_ALREADY_ATTEMPTED, state,
                List.of(), before, "overflow already received its one emergency attempt");
        }
        if (!state.checkpoint().isEmpty()
            && request.sourceUsageTimestamp() < state.checkpoint().sourceUsageTimestamp()) {
            return failed(ContextCompactionStatus.VALIDATION_FAILED, state, budget, request,
                "source usage is older than the current checkpoint");
        }

        Grouping grouping = groupMessages(state.workingMessages());
        if (!grouping.valid()) {
            return failed(ContextCompactionStatus.NO_SAFE_CUTOFF, state, budget, request,
                grouping.detail());
        }
        Cut cut = chooseCut(grouping.groups(), grouping.pinnedGroupIndex(), state, budget,
            request.emergency());
        if (cut == null) {
            return failed(ContextCompactionStatus.NO_SAFE_CUTOFF, state, budget, request,
                "no complete prefix can be removed while retaining a valid tail");
        }

        String parentCheckpointId = effectiveCheckpointId(state.checkpoint());
        List<String> artifactHandles = collectArtifactHandles(state.checkpoint(), cut.archived());
        long artifactTokens = estimateArtifactHandles(artifactHandles);
        long targetSummaryTokens = budget.usableInputTokens() - before.pinTokens()
            - estimateMessages(cut.retained()) - artifactTokens;
        if (targetSummaryTokens < 0) {
            return failed(ContextCompactionStatus.NO_SAFE_CUTOFF, state, budget, request,
                "pinned context and retained tail exceed usable input capacity");
        }
        long preferredSummaryTokens = policy.preferredInputTokens(budget.usableInputTokens())
            - before.pinTokens() - estimateMessages(cut.retained()) - artifactTokens;
        if (!request.emergency() && preferredSummaryTokens >= Math.max(32, policy.summaryHeadroomTokens())) {
            targetSummaryTokens = Math.min(targetSummaryTokens, preferredSummaryTokens);
        }
        targetSummaryTokens = Math.min(targetSummaryTokens,
            policy.maximumSummaryTokens(budget.usableInputTokens()));
        SummaryRequest summaryRequest = new SummaryRequest(state.pins(), parentCheckpointId,
            state.checkpoint().summary(), cut.archived(), cut.fromSequence(), cut.toSequence(),
            targetSummaryTokens, request.modelIdentity(), request.sourceUsageTimestamp(),
            request.emergency());

        SummaryDraft draft;
        try {
            draft = summarizer.summarize(summaryRequest);
        } catch (Exception exception) {
            return failed(ContextCompactionStatus.SUMMARY_FAILED, state, budget, request,
                "summarizer failed: " + exception.getClass().getSimpleName());
        }

        String validationError = validateDraft(draft, summaryRequest);
        if (validationError != null) {
            return failed(ContextCompactionStatus.VALIDATION_FAILED, state, budget, request,
                validationError);
        }

        long summaryTokens = estimateText(draft.summary());
        long retainedTokens = estimateMessages(cut.retained());
        long inputAfter = add(add(add(before.pinTokens(), summaryTokens), artifactTokens),
            retainedTokens);
        if (summaryTokens > targetSummaryTokens || inputAfter > budget.usableInputTokens()) {
            return failed(ContextCompactionStatus.VALIDATION_FAILED, state, budget, request,
                "summary does not fit its token target");
        }

        HarnessContextCheckpoint checkpoint = checkpoint(state, request, draft.summary(),
            artifactHandles,
            cut.fromSequence(), cut.toSequence(), before.inputTokens(), inputAfter);
        CompactionControl nextControl = state.compactionControl().succeeded(
            request.emergency() ? request.overflowId() : null);
        ContextState compacted = state.afterCompaction(cut.retained(), checkpoint, nextControl);
        return result(ContextCompactionStatus.COMPACTED, compacted, cut.archived(),
            project(compacted, budget), "compacted a structurally complete prefix");
    }

    private Cut chooseCut(List<MessageGroup> groups, int pinnedGroupIndex, ContextState state,
                          ContextTokenBudget budget, boolean emergency) {
        if (groups.size() < 2) {
            return null;
        }
        // The latest tool-bearing assistant has not been consumed until a later provider
        // assistant exists. Steering/plan-feedback USER records appended after its results do not
        // change that fact. No compaction cut may archive that protocol group or anything after it.
        int maximumCutGroupCount = pinnedGroupIndex < 0
            ? groups.size() - 1 : pinnedGroupIndex;
        if (maximumCutGroupCount < 1) {
            return null;
        }
        int cutGroupCount;
        if (emergency) {
            cutGroupCount = maximumCutGroupCount;
        } else {
            long previousSummaryTokens = estimateText(state.checkpoint().summary());
            long minimumFinalSummaryTokens = Math.max(Math.max(32, policy.summaryHeadroomTokens()),
                Math.min(previousSummaryTokens, policy.maximumSummaryTokens(budget.usableInputTokens())));
            cutGroupCount = -1;
            for (int candidate = 1; candidate <= maximumCutGroupCount; candidate++) {
                List<HarnessMessage> archived = flatten(groups.subList(0, candidate));
                List<HarnessMessage> retained = flatten(groups.subList(candidate, groups.size()));
                long retainedTokens = estimateMessages(retained);
                if (retainedTokens < policy.minimumRetainedTokens()) {
                    break;
                }
                // Artifact handles are part of the final provider projection. A shallow cut can
                // introduce enough newly archived handles to consume all summary capacity even
                // though its retained message tail appears to fit. Evaluate the complete projected
                // checkpoint and continue toward a deeper safe cut when it does not fit.
                long artifactTokens = estimateArtifactHandles(
                    collectArtifactHandles(state.checkpoint(), archived));
                long anticipatedInput = add(add(minimumFinalSummaryTokens, artifactTokens),
                    retainedTokens);
                if (anticipatedInput <= budget.usableInputTokens()) {
                    // Keep the deepest feasible fallback when a protected tail prevents the
                    // preferred target. Never discard an unconsumed tool protocol group.
                    cutGroupCount = candidate;
                    if (anticipatedInput <= policy.preferredInputTokens(budget.usableInputTokens())) {
                        break;
                    }
                }
            }
            if (cutGroupCount < 0) {
                return null;
            }
        }

        List<HarnessMessage> archived = flatten(groups.subList(0, cutGroupCount));
        List<HarnessMessage> retained = flatten(groups.subList(cutGroupCount, groups.size()));
        return new Cut(archived, retained, archived.get(0).sequence(),
            archived.get(archived.size() - 1).sequence());
    }

    private Grouping groupMessages(List<HarnessMessage> messages) {
        List<MessageGroup> groups = new ArrayList<>();
        int index = 0;
        while (index < messages.size()) {
            HarnessMessage message = messages.get(index);
            if (message.role() == HarnessMessageRole.TOOL) {
                return Grouping.invalid("orphan tool result at sequence " + message.sequence());
            }
            if (message.role() == HarnessMessageRole.CONTROL && !groups.isEmpty()) {
                // A control-plane boundary is metadata for the immediately preceding durable
                // action. Retaining it as a one-message tail lets emergency compaction archive the
                // assistant/tool evidence it describes, leaving the next model request without the
                // just-completed results. Keep both sides atomic across every compaction cut.
                MessageGroup previous = groups.remove(groups.size() - 1);
                List<HarnessMessage> combined = new ArrayList<>(previous.messages());
                combined.add(message);
                groups.add(new MessageGroup(List.copyOf(combined)));
                index++;
                continue;
            }
            if (message.role() != HarnessMessageRole.ASSISTANT || message.toolCalls().isEmpty()) {
                groups.add(new MessageGroup(List.of(message)));
                index++;
                continue;
            }

            Set<String> expected = new HashSet<>();
            for (HarnessToolCall call : message.toolCalls()) {
                if (!expected.add(call.toolCallId())) {
                    return Grouping.invalid("duplicate assistant tool call id " + call.toolCallId());
                }
            }
            Set<String> resolved = new HashSet<>();
            int endExclusive = index + 1;
            while (endExclusive < messages.size()
                && messages.get(endExclusive).role() == HarnessMessageRole.TOOL) {
                HarnessMessage toolResult = messages.get(endExclusive);
                if (!expected.contains(toolResult.toolCallId())
                    || !resolved.add(toolResult.toolCallId())) {
                    return Grouping.invalid("tool result does not match exactly one assistant call");
                }
                endExclusive++;
                if (resolved.size() == expected.size()) {
                    break;
                }
            }
            if (!resolved.equals(expected)) {
                return Grouping.invalid("assistant tool calls do not have a complete result group");
            }
            groups.add(new MessageGroup(messages.subList(index, endExclusive)));
            index = endExclusive;
        }
        int pinnedGroupIndex = -1;
        HarnessMessage latestAssistant = null;
        for (int messageIndex = messages.size() - 1; messageIndex >= 0; messageIndex--) {
            HarnessMessage candidate = messages.get(messageIndex);
            if (candidate.role() == HarnessMessageRole.ASSISTANT) {
                latestAssistant = candidate;
                break;
            }
        }
        if (latestAssistant != null && !latestAssistant.toolCalls().isEmpty()) {
            for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
                for (HarnessMessage grouped : groups.get(groupIndex).messages()) {
                    if (grouped == latestAssistant) {
                        pinnedGroupIndex = groupIndex;
                        break;
                    }
                }
                if (pinnedGroupIndex >= 0) {
                    break;
                }
            }
            if (pinnedGroupIndex < 0) {
                return Grouping.invalid(
                    "latest unconsumed tool protocol group could not be pinned");
            }
        }
        return Grouping.valid(groups, pinnedGroupIndex);
    }

    private String validateDraft(SummaryDraft draft, SummaryRequest request) {
        if (draft == null || draft.summary() == null || draft.summary().isBlank()) {
            return "summary is blank";
        }
        ContextPins pins = request.pins();
        if (!pins.originalRequirement().equals(draft.originalRequirement())
            || !pins.currentPlan().equals(draft.currentPlan())
            || pins.permissionMode() != draft.permissionMode()
            || !pins.securityConstraints().equals(draft.securityConstraints())) {
            return "summarizer attempted to rewrite immutable context pins";
        }
        if (!Objects.equals(request.previousCheckpointId(), draft.sourceCheckpointId())) {
            return "summary does not acknowledge the previous checkpoint lineage";
        }
        return null;
    }

    private HarnessContextCheckpoint checkpoint(ContextState state, CompactionRequest request,
                                                 String summary, List<String> artifactHandles,
                                                 long fromSequence,
                                                 long toSequence, long inputBefore,
                                                 long inputAfter) {
        HarnessContextCheckpoint previous = state.checkpoint();
        String parentId = effectiveCheckpointId(previous);
        String checkpointId = checkpointId(parentId, fromSequence, toSequence, summary,
            artifactHandles, request, state.pins());
        List<String> lineage = new ArrayList<>();
        if (!previous.lineage().isEmpty()) {
            lineage.addAll(previous.lineage());
        } else if (parentId != null) {
            lineage.add(parentId);
        }
        lineage.add(checkpointId);
        return new HarnessContextCheckpoint(checkpointId, lineage, fromSequence, toSequence,
            toSequence, summary, artifactHandles, inputBefore, inputAfter,
            request.modelIdentity(), request.sourceUsageTimestamp(),
            state.pins().securityConstraints(), request.now());
    }

    private List<String> collectArtifactHandles(HarnessContextCheckpoint previous,
                                                List<HarnessMessage> archived) {
        java.util.LinkedHashSet<String> handles = new java.util.LinkedHashSet<>();
        for (String handle : previous.artifactIds()) {
            addArtifactHandle(handles, handle);
        }
        for (HarnessMessage message : archived) {
            Object artifactId = message.metadata().get("artifactId");
            if (artifactId instanceof String hash && !hash.isBlank()) {
                addArtifactHandle(handles, message.runId() + ":" + hash.strip());
            }
        }
        return List.copyOf(handles);
    }

    private void addArtifactHandle(java.util.LinkedHashSet<String> handles, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        String handle = candidate.strip();
        if (utf8Length(handle) > MAX_ARTIFACT_HANDLE_BYTES) {
            return;
        }
        // A repeated handle becomes the newest reference. When a bounded checkpoint rolls over,
        // retain recent live handles instead of allowing an ever-growing provider-side preamble.
        handles.remove(handle);
        handles.add(handle);
        while (handles.size() > MAX_ARTIFACT_HANDLES
            || artifactHandleBytes(handles) > MAX_ARTIFACT_HANDLE_BYTES) {
            var iterator = handles.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private long estimateArtifactHandles(List<String> handles) {
        long tokens = 0;
        for (String handle : handles) {
            tokens = add(tokens, estimateText(handle));
            tokens = add(tokens, estimateText("\n"));
        }
        return tokens;
    }

    private int artifactHandleBytes(Iterable<String> handles) {
        long bytes = 0;
        for (String handle : handles) {
            bytes += utf8Length(handle) + 1L;
            if (bytes > MAX_ARTIFACT_HANDLE_BYTES) {
                return MAX_ARTIFACT_HANDLE_BYTES + 1;
            }
        }
        return (int) bytes;
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private String effectiveCheckpointId(HarnessContextCheckpoint checkpoint) {
        if (checkpoint.checkpointId() != null) {
            return checkpoint.checkpointId();
        }
        if (checkpoint.isEmpty()) {
            return null;
        }
        return hash("legacy", Long.toString(checkpoint.fromSequence()),
            Long.toString(checkpoint.toSequence()), Objects.toString(checkpoint.summary(), ""),
            Long.toString(checkpoint.createdAt()));
    }

    private String checkpointId(String parentId, long fromSequence, long toSequence,
                                String summary, List<String> artifactHandles,
                                CompactionRequest request, ContextPins pins) {
        List<String> parts = new ArrayList<>();
        parts.add(Objects.toString(parentId, "root"));
        parts.add(Long.toString(fromSequence));
        parts.add(Long.toString(toSequence));
        parts.add(summary);
        parts.add(request.modelIdentity());
        parts.add(Long.toString(request.sourceUsageTimestamp()));
        parts.add(pins.originalRequirement());
        parts.add(pins.currentPlan());
        parts.add(pins.permissionMode().name());
        parts.addAll(pins.securityConstraints());
        parts.add("artifact-handles");
        parts.addAll(artifactHandles);
        return hash(parts.toArray(String[]::new));
    }

    private String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private ContextCompactionResult failed(ContextCompactionStatus status, ContextState state,
                                           ContextTokenBudget budget, CompactionRequest request,
                                           String detail) {
        CompactionControl nextControl = state.compactionControl().failed(
            request.emergency() ? request.overflowId() : null);
        ContextState failedState = state.withControl(nextControl);
        return result(status, failedState, List.of(), project(failedState, budget), detail);
    }

    private ContextCompactionResult result(ContextCompactionStatus status, ContextState state,
                                           List<HarnessMessage> archived, ContextWindow window,
                                           String detail) {
        return new ContextCompactionResult(status, state, archived, window, detail);
    }

    private long estimateMessages(List<HarnessMessage> messages) {
        long tokens = 0;
        for (HarnessMessage message : messages) {
            long estimate = tokenEstimator.estimateMessage(message);
            if (estimate < 0) {
                throw new IllegalArgumentException("Token estimator returned a negative value");
            }
            tokens = add(tokens, estimate);
        }
        return tokens;
    }

    private long estimateText(String text) {
        long estimate = tokenEstimator.estimateText(text);
        if (estimate < 0) {
            throw new IllegalArgumentException("Token estimator returned a negative value");
        }
        return estimate;
    }

    private long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Token accounting overflow", exception);
        }
    }

    private List<HarnessMessage> flatten(List<MessageGroup> groups) {
        List<HarnessMessage> messages = new ArrayList<>();
        for (MessageGroup group : groups) {
            messages.addAll(group.messages());
        }
        return List.copyOf(messages);
    }

    private record MessageGroup(List<HarnessMessage> messages) {
        private MessageGroup {
            messages = List.copyOf(messages);
        }
    }

    private record Grouping(boolean valid, List<MessageGroup> groups, int pinnedGroupIndex,
                            String detail) {
        private static Grouping valid(List<MessageGroup> groups, int pinnedGroupIndex) {
            return new Grouping(true, List.copyOf(groups), pinnedGroupIndex, null);
        }

        private static Grouping invalid(String detail) {
            return new Grouping(false, List.of(), -1, detail);
        }
    }

    private record Cut(List<HarnessMessage> archived, List<HarnessMessage> retained,
                       long fromSequence, long toSequence) {
    }
}
