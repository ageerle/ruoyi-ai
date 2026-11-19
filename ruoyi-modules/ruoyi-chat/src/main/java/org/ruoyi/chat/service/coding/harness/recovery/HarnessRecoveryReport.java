package org.ruoyi.chat.service.coding.harness.recovery;

/** Immutable startup-recovery accounting, useful both for operations and focused tests. */
public record HarnessRecoveryReport(
    int scanned,
    int scheduled,
    int alreadyScheduled,
    int waitingSkipped,
    int terminalSkipped,
    int quarantined,
    boolean truncated,
    boolean idempotentNoop
) {

    public static HarnessRecoveryReport disabledOrAlreadyRun() {
        return new HarnessRecoveryReport(0, 0, 0, 0, 0, 0, false, true);
    }
}
