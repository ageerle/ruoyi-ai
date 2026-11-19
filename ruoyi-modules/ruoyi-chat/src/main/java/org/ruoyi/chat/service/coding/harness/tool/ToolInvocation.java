package org.ruoyi.chat.service.coding.harness.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One model-issued tool call. The supplied callId is preserved across planning and persistence. */
public record ToolInvocation(
    String callId,
    String toolName,
    String operation,
    Map<String, Object> arguments,
    List<String> mutationTargets
) {

    public ToolInvocation {
        if (callId == null || callId.isBlank() || toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("Tool callId and toolName are required");
        }
        operation = operation == null || operation.isBlank() ? toolName : operation;
        arguments = arguments == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
        mutationTargets = mutationTargets == null ? List.of() : List.copyOf(mutationTargets);
        if (mutationTargets.stream().anyMatch(path -> path == null || path.isBlank())) {
            throw new IllegalArgumentException("Mutation targets cannot contain blank paths");
        }
    }

    public static ToolInvocation of(String callId, String toolName, Map<String, Object> arguments) {
        return new ToolInvocation(callId, toolName, toolName, arguments, List.of());
    }
}
