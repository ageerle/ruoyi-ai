package org.ruoyi.chat.service.coding.harness.context;

import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;

import java.util.List;

public record ContextCompactionResult(
    ContextCompactionStatus status,
    ContextState state,
    List<HarnessMessage> archivedMessages,
    ContextWindow window,
    String detail
) {

    public ContextCompactionResult {
        if (status == null || state == null || window == null) {
            throw new IllegalArgumentException("Invalid compaction result");
        }
        archivedMessages = archivedMessages == null ? List.of() : List.copyOf(archivedMessages);
    }

    public boolean compacted() {
        return status == ContextCompactionStatus.COMPACTED;
    }
}
