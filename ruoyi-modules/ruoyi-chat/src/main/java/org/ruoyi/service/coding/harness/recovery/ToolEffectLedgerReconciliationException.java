package org.ruoyi.service.coding.harness.recovery;

/** A single run could not be reconciled without guessing about an external side effect. */
public final class ToolEffectLedgerReconciliationException extends RuntimeException {

    private final ToolEffectLedgerFailureReason reason;

    public ToolEffectLedgerReconciliationException(ToolEffectLedgerFailureReason reason) {
        super(reason.code());
        this.reason = reason;
    }

    public ToolEffectLedgerReconciliationException(ToolEffectLedgerFailureReason reason,
                                                    Throwable cause) {
        super(reason.code(), cause);
        this.reason = reason;
    }

    public ToolEffectLedgerFailureReason reason() {
        return reason;
    }
}
