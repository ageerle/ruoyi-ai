package org.ruoyi.chat.service.coding.harness.context;

public enum ContextCompactionStatus {
    NOT_NEEDED,
    COMPACTED,
    NO_SAFE_CUTOFF,
    SUMMARY_FAILED,
    VALIDATION_FAILED,
    EMERGENCY_ALREADY_ATTEMPTED,
    CIRCUIT_OPEN
}
