package org.ruoyi.service.coding.harness.loop.model;

/** Stable terminal failure categories for one provider turn. */
public enum ModelTurnFailureKind {
    CONTEXT_OVERFLOW,
    REQUEST_REJECTED,
    PROVIDER_ERROR,
    LISTENER_ERROR,
    TIMEOUT,
    CANCELLED,
    INTERRUPTED,
    START_FAILURE
}
