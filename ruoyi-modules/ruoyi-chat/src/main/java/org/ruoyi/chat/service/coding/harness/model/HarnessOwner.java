package org.ruoyi.chat.service.coding.harness.model;

import java.util.Objects;

/** Authenticated owner of every Harness resource. */
public record HarnessOwner(String tenantId, Long userId) {

    public HarnessOwner {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        Objects.requireNonNull(userId, "userId is required");
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
