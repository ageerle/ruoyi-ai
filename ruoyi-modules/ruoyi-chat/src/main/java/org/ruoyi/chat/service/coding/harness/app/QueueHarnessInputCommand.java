package org.ruoyi.chat.service.coding.harness.app;

import org.ruoyi.chat.service.coding.harness.model.HarnessInputKind;

import java.util.UUID;

public record QueueHarnessInputCommand(HarnessInputKind kind, String content,
                                       String idempotencyKey) {
    public QueueHarnessInputCommand {
        if (kind == null || kind == HarnessInputKind.INITIAL || content == null || content.isBlank()
            || idempotencyKey == null || idempotencyKey.isBlank()
            || idempotencyKey.length() > 256) {
            throw new IllegalArgumentException("A queued input must be STEER or FOLLOW_UP and have content");
        }
        content = content.strip();
        idempotencyKey = idempotencyKey.strip();
    }

    public QueueHarnessInputCommand(HarnessInputKind kind, String content) {
        this(kind, content, UUID.randomUUID().toString());
    }
}
