package org.ruoyi.chat.service.coding.harness.model;

import java.util.UUID;

public record HarnessQueuedInput(
    String inputId,
    HarnessInputKind kind,
    String content,
    long createdAt
) {

    public HarnessQueuedInput {
        if (inputId == null || inputId.isBlank() || kind == null || content == null || content.isBlank()) {
            throw new IllegalArgumentException("Invalid queued input");
        }
    }

    public static HarnessQueuedInput create(HarnessInputKind kind, String content, long now) {
        return new HarnessQueuedInput(UUID.randomUUID().toString(), kind, content, now);
    }

    public static HarnessQueuedInput createWithId(String inputId, HarnessInputKind kind,
                                                  String content, long now) {
        return new HarnessQueuedInput(inputId, kind, content, now);
    }
}
