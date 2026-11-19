package org.ruoyi.chat.service.coding.harness.runtime;

import org.ruoyi.chat.service.coding.harness.model.HarnessOwner;

public record HarnessRunRequest(HarnessOwner owner, String sessionId, String runId) {
    public HarnessRunRequest {
        if (owner == null || sessionId == null || sessionId.isBlank()
            || runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Invalid Harness run request");
        }
    }
}
