package org.ruoyi.service.coding.harness.tool.command;

import java.util.List;

record DockerSandboxInvocation(String containerName, List<String> command) {

    DockerSandboxInvocation {
        if (containerName == null || containerName.isBlank() || command == null
            || command.isEmpty()) {
            throw new IllegalArgumentException("Docker sandbox invocation is incomplete");
        }
        command = List.copyOf(command);
    }
}
