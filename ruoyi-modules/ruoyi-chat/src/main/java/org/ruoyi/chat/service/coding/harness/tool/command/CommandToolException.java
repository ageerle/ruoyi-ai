package org.ruoyi.chat.service.coding.harness.tool.command;

/** Fail-closed validation, startup, or lifecycle error for execute_process. */
public class CommandToolException extends RuntimeException {

    private final String code;

    public CommandToolException(String code, String message) {
        super(message);
        this.code = requireCode(code);
    }

    public CommandToolException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = requireCode(code);
    }

    public String code() {
        return code;
    }

    private static String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Command error code is required");
        }
        return code;
    }
}
