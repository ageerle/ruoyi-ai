package org.ruoyi.service.coding.harness.model;

/** Explains why a run uses its selected model without exposing provider credentials. */
public enum HarnessModelRouteSource {
    AUTO_POLICY,
    SESSION_FIXED,
    LEGACY_SESSION_FALLBACK
}
