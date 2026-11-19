package org.ruoyi.chat.service.coding.harness.loop.protocol;

/** Stable machine-readable reasons why a ledger cannot be submitted to a model. */
public enum ToolProtocolViolationCode {
    NULL_MESSAGE,
    INVALID_MESSAGE_CONTENT,
    EMPTY_ASSISTANT_MESSAGE,
    INVALID_TOOL_CALL_ARGUMENTS,
    DUPLICATE_CALL_ID,
    ORPHAN_RESULT,
    DUPLICATE_RESULT,
    USER_DURING_OPEN_BATCH,
    ASSISTANT_DURING_OPEN_BATCH,
    SYSTEM_DURING_OPEN_BATCH,
    TOOL_NAME_MISMATCH,
    INVALID_TOOL_RESULT
}
