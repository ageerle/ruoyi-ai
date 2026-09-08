package org.ruoyi.service.coding.harness.recovery;

/** Stable, argument-free reason codes for fail-closed tool-ledger recovery. */
public enum ToolEffectLedgerFailureReason {
    SCAN_LIMIT_EXCEEDED("tool_effect_ledger_scan_limit_exceeded"),
    NON_MONOTONIC_SEQUENCE("tool_effect_ledger_non_monotonic_sequence"),
    COMMITTED_RECEIPT_MISMATCH("committed_tool_receipt_mismatch"),
    CONCURRENT_APPEND_RETRY_EXHAUSTED("tool_effect_ledger_append_conflict");

    private final String code;

    ToolEffectLedgerFailureReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
