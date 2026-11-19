package org.ruoyi.chat.service.coding.harness.model;

public enum HarnessMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL,
    /** Audit/control-plane record that is never sent to the model as a chat message. */
    CONTROL
}
