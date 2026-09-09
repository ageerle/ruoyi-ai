package org.ruoyi.service.coding.harness.recovery;

/** Stable, non-sensitive reason codes for fail-closed tool-effect recovery. */
public enum UncertainToolEffectReason {

    OPERATOR_ADJUDICATION_REQUIRED("uncertain_tool_effect_requires_adjudication"),
    LEDGER_RECONCILIATION_UNAVAILABLE("tool_effect_ledger_reconciliation_unavailable");

    private final String code;

    UncertainToolEffectReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
