package org.ruoyi.chat.service.coding.harness.approval;

/** Authoritative lifecycle of one exact tool invocation approval. */
public enum ApprovalState {
    PENDING,
    APPROVED,
    DENIED,
    EXPIRED,
    CONSUMED;

    public boolean isTerminal() {
        return this == DENIED || this == EXPIRED || this == CONSUMED;
    }
}
