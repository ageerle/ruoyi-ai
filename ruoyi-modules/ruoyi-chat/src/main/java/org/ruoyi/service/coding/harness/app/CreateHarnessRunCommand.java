package org.ruoyi.service.coding.harness.app;

import org.ruoyi.service.coding.harness.model.HarnessBudget;

import java.util.List;
import java.util.UUID;

public record CreateHarnessRunCommand(String requirement, HarnessBudget budget,
                                      String idempotencyKey,
                                      List<HarnessImageInput> images) {

    public CreateHarnessRunCommand(String requirement, HarnessBudget budget) {
        this(requirement, budget, UUID.randomUUID().toString(), List.of());
    }

    public CreateHarnessRunCommand(String requirement, HarnessBudget budget,
                                   String idempotencyKey) {
        this(requirement, budget, idempotencyKey, List.of());
    }

    public CreateHarnessRunCommand {
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
            || idempotencyKey.length() > 256) {
            throw new IllegalArgumentException("idempotencyKey is required and must not exceed 256 characters");
        }
        idempotencyKey = idempotencyKey.strip();
        images = images == null ? List.of() : List.copyOf(images);
        if (images.size() > 5) {
            throw new IllegalArgumentException("A run accepts at most 5 images");
        }
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }
}
