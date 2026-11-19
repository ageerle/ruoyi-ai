package org.ruoyi.chat.service.coding.harness.model;

import java.util.List;

public record HarnessPlan(
    long revision,
    String goal,
    List<HarnessPlanStep> steps,
    String replanReason,
    long createdAt,
    long updatedAt
) {

    public HarnessPlan {
        steps = steps == null ? List.of() : List.copyOf(steps);
        if (revision < 0) {
            throw new IllegalArgumentException("Plan revision cannot be negative");
        }
    }

    public static HarnessPlan empty() {
        return new HarnessPlan(0, null, List.of(), null, 0, 0);
    }
}
