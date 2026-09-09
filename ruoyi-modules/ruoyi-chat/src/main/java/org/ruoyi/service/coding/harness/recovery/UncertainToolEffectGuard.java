package org.ruoyi.service.coding.harness.recovery;

import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessRunStatus;
import org.ruoyi.service.coding.harness.model.HarnessToolEffect;

import java.util.Comparator;
import java.util.Optional;

/**
 * Detects a write-ahead tool intent whose external outcome cannot be replayed or inferred.
 * Such an effect may be changed only by an exact durable receipt or a future operator
 * adjudication command.
 */
public final class UncertainToolEffectGuard {

    private UncertainToolEffectGuard() {
    }

    public static Optional<Finding> firstFinding(HarnessRunState run) {
        if (run == null) {
            throw new IllegalArgumentException("Harness run is required");
        }
        return run.toolEffects().values().stream()
            .filter(UncertainToolEffectGuard::requiresAdjudication)
            .sorted(Comparator.comparing(HarnessToolEffect::effectId))
            .findFirst()
            .map(effect -> new Finding(effect,
                UncertainToolEffectReason.OPERATOR_ADJUDICATION_REQUIRED));
    }

    public static boolean requiresAdjudication(HarnessToolEffect effect) {
        return effect != null && effect.requiresOperatorAdjudication();
    }

    /**
     * Projects SUSPENDED without ever rewriting the effect. Intermediate transitions exist only
     * in memory so QUEUED/waiting snapshots are persisted atomically as SUSPENDED.
     */
    public static HarnessRunState suspend(HarnessRunState run, Finding finding, long now) {
        if (finding == null || !run.toolEffects().containsValue(finding.effect())) {
            throw new IllegalArgumentException("Uncertain tool-effect finding is required");
        }
        return suspend(run, finding.reason().code(), now);
    }

    /** Isolates a run after ledger reconciliation itself became unavailable. */
    public static HarnessRunState suspend(HarnessRunState run, String reasonCode, long now) {
        if (run == null || reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException("Run and stable suspension reason are required");
        }
        HarnessRunState next = run;
        if (next.status() == HarnessRunStatus.QUEUED) {
            next = next.transition(HarnessRunStatus.RUNNING, null, now);
        } else if (next.status() == HarnessRunStatus.WAITING_FOR_APPROVAL
            || next.status() == HarnessRunStatus.WAITING_FOR_INPUT) {
            next = next.transition(HarnessRunStatus.QUEUED, null, now)
                .transition(HarnessRunStatus.RUNNING, null, now);
        }
        if (next.status() == HarnessRunStatus.RUNNING
            || next.status() == HarnessRunStatus.SUSPENDED) {
            return next.transition(HarnessRunStatus.SUSPENDED, reasonCode, now);
        }
        return next;
    }

    public record Finding(HarnessToolEffect effect, UncertainToolEffectReason reason) {
        public Finding {
            if (effect == null || reason == null || !requiresAdjudication(effect)) {
                throw new IllegalArgumentException("Invalid uncertain tool-effect finding");
            }
        }
    }
}
