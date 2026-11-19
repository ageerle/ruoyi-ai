package org.ruoyi.chat.service.coding.harness.loop.protocol;

/** One protocol defect tied to the source ledger position and, when known, a tool call. */
public record ToolProtocolViolation(
    ToolProtocolViolationCode code,
    long messageSequence,
    String toolCallId,
    String detail
) {

    public ToolProtocolViolation {
        if (code == null || messageSequence < -1 || detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("Invalid tool protocol violation");
        }
        toolCallId = toolCallId == null || toolCallId.isBlank() ? null : toolCallId;
    }
}
