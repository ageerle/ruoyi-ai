package org.ruoyi.chat.service.coding.harness.loop.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import org.ruoyi.chat.service.coding.harness.tool.ToolDescriptor;

record HarnessToolRegistration(
    ToolSpecification specification,
    ToolDescriptor descriptor,
    ToolExecutor executor
) {
    HarnessToolRegistration {
        if (specification == null || descriptor == null || executor == null
            || !specification.name().equalsIgnoreCase(descriptor.toolName())) {
            throw new IllegalArgumentException("Invalid Harness tool registration");
        }
    }
}
