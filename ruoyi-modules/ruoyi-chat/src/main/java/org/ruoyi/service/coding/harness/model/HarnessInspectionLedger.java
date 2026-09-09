package org.ruoyi.service.coding.harness.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Durable inspection history used to reject repeated repository reads across model turns,
 * compaction and process restarts.
 */
public record HarnessInspectionLedger(
    int schemaVersion,
    long mutationEpoch,
    Map<String, List<HarnessReadSpan>> readCoverage,
    Map<String, String> inspectionFingerprints,
    int duplicateAttempts,
    boolean synthesisRequired,
    boolean workspaceBoundaryReached
) {

    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int MAX_SPANS = 4_096;
    public static final int MAX_FINGERPRINTS = 2_048;

    public HarnessInspectionLedger {
        if (schemaVersion < 1 || mutationEpoch < 0 || duplicateAttempts < 0) {
            throw new IllegalArgumentException("Invalid Harness inspection ledger");
        }
        Map<String, List<HarnessReadSpan>> coverage = new LinkedHashMap<>();
        if (readCoverage != null) {
            readCoverage.forEach((path, spans) -> {
                if (path == null || path.isBlank() || spans == null) {
                    throw new IllegalArgumentException("Invalid Harness read coverage");
                }
                coverage.put(path, List.copyOf(spans));
            });
        }
        readCoverage = Map.copyOf(coverage);
        inspectionFingerprints = inspectionFingerprints == null
            ? Map.of() : Map.copyOf(inspectionFingerprints);
        if (spanCount(readCoverage) > MAX_SPANS
            || inspectionFingerprints.size() > MAX_FINGERPRINTS) {
            throw new IllegalArgumentException("Harness inspection ledger capacity exceeded");
        }
    }

    public static HarnessInspectionLedger empty() {
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, 0, Map.of(), Map.of(), 0,
            false, false);
    }

    public List<HarnessReadSpan> overlaps(String toolCallId, String path, int start, int end) {
        return readCoverage.getOrDefault(path, List.of()).stream()
            .filter(span -> !span.toolCallId().equals(toolCallId))
            .filter(span -> span.overlaps(start, end))
            .toList();
    }

    public boolean hasInspection(String toolCallId, String fingerprint) {
        String priorCall = inspectionFingerprints.get(fingerprint);
        return priorCall != null && !priorCall.equals(toolCallId);
    }

    public HarnessInspectionLedger recordRead(String path, HarnessReadSpan span) {
        if (readCoverage.values().stream().flatMap(List::stream)
            .anyMatch(existing -> existing.toolCallId().equals(span.toolCallId()))) {
            return this;
        }
        if (spanCount(readCoverage) >= MAX_SPANS) {
            throw new IllegalStateException("Harness read coverage capacity exceeded");
        }
        Map<String, List<HarnessReadSpan>> next = new LinkedHashMap<>(readCoverage);
        List<HarnessReadSpan> spans = new ArrayList<>(next.getOrDefault(path, List.of()));
        spans.add(span);
        next.put(path, List.copyOf(spans));
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, next,
            inspectionFingerprints, duplicateAttempts, synthesisRequired,
            workspaceBoundaryReached);
    }

    public HarnessInspectionLedger recordInspection(String toolCallId, String fingerprint) {
        String existing = inspectionFingerprints.get(fingerprint);
        if (toolCallId.equals(existing)) {
            return this;
        }
        if (inspectionFingerprints.size() >= MAX_FINGERPRINTS) {
            throw new IllegalStateException("Harness inspection fingerprint capacity exceeded");
        }
        Map<String, String> next = new LinkedHashMap<>(inspectionFingerprints);
        next.put(fingerprint, toolCallId);
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, readCoverage,
            next, duplicateAttempts, synthesisRequired, workspaceBoundaryReached);
    }

    public HarnessInspectionLedger recordDuplicate(boolean requireSynthesis) {
        int nextAttempts = duplicateAttempts == Integer.MAX_VALUE
            ? Integer.MAX_VALUE : duplicateAttempts + 1;
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, readCoverage,
            inspectionFingerprints, nextAttempts, synthesisRequired || requireSynthesis,
            workspaceBoundaryReached);
    }

    public HarnessInspectionLedger requireSynthesis() {
        if (synthesisRequired) {
            return this;
        }
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, readCoverage,
            inspectionFingerprints, duplicateAttempts, true, workspaceBoundaryReached);
    }

    /**
     * An independent evidence audit may inspect a decisive layer that the first pass never
     * visited. Preserve every fingerprint and covered range so it cannot repeat work, while
     * reopening only the unused portion of the bounded inspection budget.
     */
    public HarnessInspectionLedger beginEvidenceAudit() {
        if (!synthesisRequired) {
            return this;
        }
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, readCoverage,
            inspectionFingerprints, duplicateAttempts, false, workspaceBoundaryReached);
    }

    /**
     * BUILD and VERIFY are deliberately independent phases. Each phase must be able to inspect
     * the current workspace once even when the preceding phase already read the same files or ran
     * the same search. Advancing the epoch keeps the boundary durable across restarts while
     * resetting the convergence counters that are meaningful only within one phase.
     */
    public HarnessInspectionLedger beginIndependentPhase() {
        long nextEpoch = mutationEpoch == Long.MAX_VALUE ? Long.MAX_VALUE : mutationEpoch + 1;
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, nextEpoch, Map.of(), Map.of(),
            0, false, workspaceBoundaryReached);
    }

    /**
     * A successful first-party file mutation invalidates byte/range coverage because the
     * workspace contents changed. It must not erase the durable inspection-call history: doing
     * so lets alternating read/write turns reset the convergence budget forever.
     */
    public HarnessInspectionLedger invalidate() {
        long nextEpoch = mutationEpoch == Long.MAX_VALUE ? Long.MAX_VALUE : mutationEpoch + 1;
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, nextEpoch, Map.of(),
            inspectionFingerprints, 0, false, workspaceBoundaryReached);
    }

    /** Persists a fail-closed workspace escape observation so hot paths never rescan the ledger. */
    public HarnessInspectionLedger recordWorkspaceBoundaryReached() {
        if (workspaceBoundaryReached && schemaVersion >= CURRENT_SCHEMA_VERSION) {
            return this;
        }
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, readCoverage,
            inspectionFingerprints, duplicateAttempts, synthesisRequired, true);
    }

    /** Completes the one-time v1 ledger migration even when no escape was observed. */
    public HarnessInspectionLedger completeWorkspaceBoundaryMigration() {
        if (schemaVersion >= CURRENT_SCHEMA_VERSION) {
            return this;
        }
        return new HarnessInspectionLedger(CURRENT_SCHEMA_VERSION, mutationEpoch, readCoverage,
            inspectionFingerprints, duplicateAttempts, synthesisRequired,
            workspaceBoundaryReached);
    }

    private static int spanCount(Map<String, List<HarnessReadSpan>> coverage) {
        return coverage.values().stream().mapToInt(List::size).sum();
    }
}
