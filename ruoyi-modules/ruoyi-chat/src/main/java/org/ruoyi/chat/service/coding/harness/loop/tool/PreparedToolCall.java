package org.ruoyi.chat.service.coding.harness.loop.tool;

import org.ruoyi.chat.service.coding.harness.model.HarnessToolCall;
import org.ruoyi.chat.service.coding.harness.tool.ToolDescriptor;
import org.ruoyi.chat.service.coding.harness.tool.ToolInvocation;

/** Validated model call paired with its policy projection and registered descriptor. */
public record PreparedToolCall(
    HarnessToolCall source,
    ToolInvocation invocation,
    ToolDescriptor descriptor
) {
    public PreparedToolCall {
        if (source == null || invocation == null || descriptor == null
            || !source.toolCallId().equals(invocation.callId())
            || !source.toolName().equalsIgnoreCase(descriptor.toolName())) {
            throw new IllegalArgumentException("Invalid prepared tool call");
        }
    }
}
