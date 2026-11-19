package org.ruoyi.chat.service.coding.harness.tool;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable operational and security metadata for one registered tool. */
public record ToolDescriptor(
    String toolName,
    Set<ToolCapability> capabilities,
    boolean concurrencySafe,
    long timeoutMillis,
    long maxInputBytes,
    long maxOutputBytes,
    boolean outputOffloadable,
    String riskSummary
) {

    public ToolDescriptor {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool name is required");
        }
        if (capabilities == null || capabilities.isEmpty()) {
            throw new IllegalArgumentException("At least one tool capability is required");
        }
        if (timeoutMillis <= 0 || maxInputBytes <= 0 || maxOutputBytes <= 0) {
            throw new IllegalArgumentException("Tool limits must be positive");
        }
        if (riskSummary == null || riskSummary.isBlank()) {
            throw new IllegalArgumentException("Tool risk summary is required");
        }
        capabilities = Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    public boolean hasCapability(ToolCapability capability) {
        return capabilities.contains(capability);
    }
}
