package org.ruoyi.chat.service.coding.harness.context;

import org.ruoyi.chat.service.coding.harness.model.HarnessPermissionMode;

import java.util.List;

/** Control-plane facts that summaries may repeat but may never rewrite. */
public record ContextPins(
    String originalRequirement,
    String currentPlan,
    HarnessPermissionMode permissionMode,
    List<String> securityConstraints
) {

    public ContextPins {
        if (originalRequirement == null || originalRequirement.isBlank() || permissionMode == null) {
            throw new IllegalArgumentException("Context pins require an objective and permission mode");
        }
        currentPlan = currentPlan == null ? "" : currentPlan;
        securityConstraints = securityConstraints == null
            ? List.of() : List.copyOf(securityConstraints);
        if (securityConstraints.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Security constraints must be non-blank");
        }
    }
}
