package org.ruoyi.chat.service.coding.harness.loop.tool;

import java.util.List;

/** Completed ALLOW slots in the assistant's original source order. */
public record HarnessToolBatchExecution(
    List<HarnessToolExecutionResult> results
) {
    public HarnessToolBatchExecution {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
