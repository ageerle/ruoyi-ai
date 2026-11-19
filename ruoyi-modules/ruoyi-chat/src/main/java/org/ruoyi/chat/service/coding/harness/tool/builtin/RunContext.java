package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/** Immutable workspace lease and resource policy bound to a set of tool objects. */
public record RunContext(
    String runId,
    Path leaseRoot,
    BuiltinToolLimits limits,
    boolean preferRipgrep
) {

    public RunContext {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("Run id is required");
        }
        Objects.requireNonNull(leaseRoot, "leaseRoot");
        limits = Objects.requireNonNullElse(limits, BuiltinToolLimits.DEFAULT);
        try {
            leaseRoot = leaseRoot.toRealPath().normalize();
        } catch (IOException error) {
            throw new IllegalArgumentException("Workspace lease root cannot be resolved: " + leaseRoot, error);
        }
        if (!Files.isDirectory(leaseRoot)) {
            throw new IllegalArgumentException("Workspace lease root is not a directory: " + leaseRoot);
        }
    }

    public static RunContext forWorkspace(Path leaseRoot) {
        return new RunContext(UUID.randomUUID().toString(), leaseRoot, BuiltinToolLimits.DEFAULT, true);
    }
}
