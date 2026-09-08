package org.ruoyi.service.coding.harness.tool.command;

import java.io.IOException;

interface DockerRuntime {

    void verifyAvailable(DockerSandboxConfig sandbox);

    Process start(DockerSandboxConfig sandbox, DockerSandboxInvocation invocation,
                  long timeoutMs) throws IOException;

    void ensureRemoved(DockerSandboxConfig sandbox, String containerName, long timeoutMs);
}
