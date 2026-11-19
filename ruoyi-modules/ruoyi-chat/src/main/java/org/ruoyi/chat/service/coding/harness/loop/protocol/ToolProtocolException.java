package org.ruoyi.chat.service.coding.harness.loop.protocol;

/** Prevents a malformed or incomplete transcript from reaching the provider. */
public final class ToolProtocolException extends IllegalStateException {

    private final ToolProtocolValidation validation;

    public ToolProtocolException(ToolProtocolValidation validation) {
        super(message(validation));
        this.validation = validation;
    }

    public ToolProtocolValidation validation() {
        return validation;
    }

    private static String message(ToolProtocolValidation validation) {
        if (validation == null) {
            return "Tool protocol validation is unavailable";
        }
        if (!validation.violations().isEmpty()) {
            ToolProtocolViolation first = validation.violations().get(0);
            return "Tool protocol violation " + first.code() + ": " + first.detail();
        }
        return "Transcript is not at a valid next-model-request boundary";
    }
}
