package org.ruoyi.chat.service.coding.harness.loop.protocol;

import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;

import java.util.List;
import java.util.Optional;

/** CONTROL-free, provider-ordered transcript plus structural validation evidence. */
public record ToolProtocolValidation(
    List<HarnessMessage> modelMessages,
    List<ToolBatchProjection> batches,
    List<ToolProtocolViolation> violations,
    boolean allowsNextModelRequest
) {

    public ToolProtocolValidation {
        modelMessages = modelMessages == null ? List.of() : List.copyOf(modelMessages);
        batches = batches == null ? List.of() : List.copyOf(batches);
        violations = violations == null ? List.of() : List.copyOf(violations);
        if (allowsNextModelRequest && (!violations.isEmpty()
            || batches.stream().anyMatch(batch -> !batch.complete()))) {
            throw new IllegalArgumentException("Invalid transcript cannot allow a model request");
        }
    }

    public boolean valid() {
        return violations.isEmpty() && batches.stream().allMatch(ToolBatchProjection::complete);
    }

    public Optional<ToolBatchProjection> lastUnclosedBatch() {
        for (int index = batches.size() - 1; index >= 0; index--) {
            ToolBatchProjection batch = batches.get(index);
            if (!batch.missingCalls().isEmpty()) {
                return Optional.of(batch);
            }
        }
        return Optional.empty();
    }
}
