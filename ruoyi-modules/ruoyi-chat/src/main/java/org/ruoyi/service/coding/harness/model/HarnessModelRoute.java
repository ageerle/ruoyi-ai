package org.ruoyi.service.coding.harness.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable model decision anchored to a run before it enters the scheduler.
 *
 * <p>The route deliberately contains names and policy metadata only. Provider endpoints,
 * credentials and database identifiers must never enter run snapshots or events.</p>
 */
public record HarnessModelRoute(
    int policyVersion,
    String requestedModel,
    String selectedModel,
    HarnessTaskClass taskClass,
    boolean thinkingEnabled,
    HarnessModelRouteSource source
) {

    public HarnessModelRoute {
        requestedModel = normalize("requestedModel", requestedModel);
        selectedModel = normalize("selectedModel", selectedModel);
        if (policyVersion < 1 || taskClass == null || source == null) {
            throw new IllegalArgumentException("Invalid Harness model route");
        }
    }

    /** Stable, explicitly allow-listed event projection. */
    public Map<String, Object> eventMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("policyVersion", policyVersion);
        metadata.put("requestedModel", requestedModel);
        metadata.put("selectedModel", selectedModel);
        metadata.put("taskClass", taskClass.name());
        metadata.put("thinkingEnabled", thinkingEnabled);
        metadata.put("source", source.name());
        return Map.copyOf(metadata);
    }

    private static String normalize(String label, String value) {
        if (value == null || value.isBlank() || value.length() > 200) {
            throw new IllegalArgumentException(label + " is required and must not exceed 200 characters");
        }
        return value.strip();
    }
}
