package org.ruoyi.service.coding.harness.model;

/** Durable stages of the single provider context-overflow recovery attempt. */
public enum ProviderOverflowRecoveryStage {
    COMPACTION_REQUIRED,
    RETRY_READY,
    RETRY_IN_FLIGHT,
    EXHAUSTED
}
