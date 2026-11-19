package org.ruoyi.chat.service.coding.harness.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistable result slot for every tool call, including policy-synthesized results. */
public record ToolOutcome(
    String callId,
    String toolName,
    ToolOutcomeStatus status,
    boolean synthetic,
    String code,
    String message,
    Map<String, Object> details
) {

    public ToolOutcome {
        if (callId == null || callId.isBlank() || toolName == null || toolName.isBlank()
            || status == null || code == null || code.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Invalid tool outcome");
        }
        if ((status == ToolOutcomeStatus.APPROVAL_REQUIRED || status == ToolOutcomeStatus.DENIED)
            && !synthetic) {
            throw new IllegalArgumentException("Policy outcomes must be synthetic");
        }
        details = details == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static ToolOutcome pending(ToolInvocation invocation) {
        return new ToolOutcome(invocation.callId(), invocation.toolName(), ToolOutcomeStatus.PENDING,
            false, "pending_execution", "Tool call is waiting for execution", Map.of());
    }

    public static ToolOutcome approvalRequired(ToolInvocation invocation, ToolPolicyEvaluation evaluation) {
        return new ToolOutcome(invocation.callId(), invocation.toolName(),
            ToolOutcomeStatus.APPROVAL_REQUIRED, true, evaluation.code(), evaluation.reason(),
            Map.of("operation", invocation.operation()));
    }

    public static ToolOutcome denied(ToolInvocation invocation, ToolPolicyEvaluation evaluation) {
        return new ToolOutcome(invocation.callId(), invocation.toolName(), ToolOutcomeStatus.DENIED,
            true, evaluation.code(), evaluation.reason(), Map.of("operation", invocation.operation()));
    }
}
