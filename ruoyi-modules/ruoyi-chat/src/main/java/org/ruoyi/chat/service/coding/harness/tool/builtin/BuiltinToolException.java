package org.ruoyi.chat.service.coding.harness.tool.builtin;

/** Stable, user-safe failure raised by a built-in coding tool. */
public final class BuiltinToolException extends RuntimeException {

    private final String code;

    public BuiltinToolException(String code, String message) {
        super(message);
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Error code is required");
        }
        this.code = code;
    }

    public BuiltinToolException(String code, String message, Throwable cause) {
        super(message, cause);
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Error code is required");
        }
        this.code = code;
    }

    public String code() {
        return code;
    }
}
