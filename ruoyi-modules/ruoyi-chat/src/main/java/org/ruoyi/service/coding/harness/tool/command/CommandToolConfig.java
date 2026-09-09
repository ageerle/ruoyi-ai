package org.ruoyi.service.coding.harness.tool.command;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource and authorization limits for execute_process.
 *
 * <p>The executable allowlist is authorization metadata inside a mandatory Docker OS sandbox.
 * The pinned image and the tools/environment baked into it are explicit trusted prerequisites;
 * no host executable, cache, home directory, or environment is delegated to repository code.</p>
 */
public record CommandToolConfig(
    Set<String> executableAllowlist,
    Set<String> inheritedEnvironmentKeys,
    Map<String, String> additionalEnvironment,
    int maxConcurrentProcesses,
    long defaultTimeoutMs,
    long maxTimeoutMs,
    int maxOutputBytesPerStream,
    int maxArgvEntries,
    int maxArgumentChars,
    long terminationGraceMs,
    DockerSandboxConfig dockerSandbox
) {

    public static final Set<String> DEFAULT_EXECUTABLE_ALLOWLIST = Set.of(
        "git", "git.exe", "java", "java.exe", "javac", "javac.exe",
        "mvn", "mvn.cmd", "mvn.exe", "gradle", "gradle.bat",
        "node", "node.exe", "npm", "npm.cmd", "npx", "npx.cmd",
        "pnpm", "pnpm.cmd", "pnpm.exe",
        "python", "python.exe", "python3", "python3.exe", "rg", "rg.exe"
    );

    public static final Set<String> DEFAULT_INHERITED_ENVIRONMENT = Set.of();

    public static final CommandToolConfig DEFAULT = new CommandToolConfig(
        DEFAULT_EXECUTABLE_ALLOWLIST,
        DEFAULT_INHERITED_ENVIRONMENT,
        Map.of(),
        2,
        30_000,
        120_000,
        256 * 1024,
        256,
        16 * 1024,
        2_000,
        DockerSandboxConfig.UNCONFIGURED
    );

    /** Source-compatible constructor; without Docker policy execution remains fail-closed. */
    public CommandToolConfig(Set<String> executableAllowlist,
                             Set<String> inheritedEnvironmentKeys,
                             Map<String, String> additionalEnvironment,
                             int maxConcurrentProcesses,
                             long defaultTimeoutMs,
                             long maxTimeoutMs,
                             int maxOutputBytesPerStream,
                             int maxArgvEntries,
                             int maxArgumentChars,
                             long terminationGraceMs) {
        this(executableAllowlist, inheritedEnvironmentKeys, additionalEnvironment,
            maxConcurrentProcesses, defaultTimeoutMs, maxTimeoutMs, maxOutputBytesPerStream,
            maxArgvEntries, maxArgumentChars, terminationGraceMs,
            DockerSandboxConfig.UNCONFIGURED);
    }

    public CommandToolConfig {
        if (executableAllowlist == null || executableAllowlist.isEmpty()) {
            throw new IllegalArgumentException("Executable allowlist must not be empty");
        }
        LinkedHashSet<String> executables = new LinkedHashSet<>();
        for (String executable : executableAllowlist) {
            executables.add(CommandValidation.requireText(executable, "allowlisted executable", 4_096));
        }
        executableAllowlist = Set.copyOf(executables);

        inheritedEnvironmentKeys = inheritedEnvironmentKeys == null ? Set.of()
            : Set.copyOf(inheritedEnvironmentKeys);
        if (!inheritedEnvironmentKeys.isEmpty()) {
            throw new IllegalArgumentException(
                "Host environment inheritance is forbidden for sandboxed commands");
        }
        additionalEnvironment = additionalEnvironment == null
            ? Map.of() : Map.copyOf(additionalEnvironment);
        if (!additionalEnvironment.isEmpty()) {
            throw new IllegalArgumentException(
                "Host-provided container environment injection is forbidden");
        }
        dockerSandbox = dockerSandbox == null
            ? DockerSandboxConfig.UNCONFIGURED : dockerSandbox;

        if (maxConcurrentProcesses <= 0 || defaultTimeoutMs <= 0 || maxTimeoutMs <= 0
            || defaultTimeoutMs > maxTimeoutMs || maxOutputBytesPerStream <= 0
            || maxArgvEntries <= 0 || maxArgumentChars <= 0 || terminationGraceMs <= 0) {
            throw new IllegalArgumentException("Command tool limits must be positive and ordered");
        }
    }

    public CommandToolConfig withDockerSandbox(DockerSandboxConfig sandbox) {
        return new CommandToolConfig(executableAllowlist, Set.of(), Map.of(),
            maxConcurrentProcesses, defaultTimeoutMs, maxTimeoutMs, maxOutputBytesPerStream,
            maxArgvEntries, maxArgumentChars, terminationGraceMs, sandbox);
    }
}
