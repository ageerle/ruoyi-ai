package org.ruoyi.service.coding.harness.model;

/**
 * Stable, non-sensitive reason codes exposed by {@code model.turn.abandoned} events.
 *
 * <p>Provider messages and exception text remain diagnostic state and must never be copied into
 * the event ledger. These codes are deliberately coarse so clients can deterministically close a
 * streamed model effect without learning provider response bodies.</p>
 */
public enum HarnessModelEffectOutcomeCode {
    PROVIDER_ERROR,
    PROVIDER_START_FAILURE,
    PROVIDER_CANCELLED,
    PROVIDER_CONTEXT_OVERFLOW,
    MODEL_DEADLINE_EXCEEDED,
    MODEL_INTERRUPTED,
    PROTOCOL_REJECTED,
    RUN_CANCELLED,
    RUN_FAILED,
    RUN_SUSPENDED,
    RECOVERY_UNCERTAIN,
    RECOVERY_INCONSISTENT
}
