package org.ruoyi.chat.service.coding.harness.app;

public class HarnessConflictException extends RuntimeException {
    public HarnessConflictException(String message) {
        super(message);
    }

    public HarnessConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
