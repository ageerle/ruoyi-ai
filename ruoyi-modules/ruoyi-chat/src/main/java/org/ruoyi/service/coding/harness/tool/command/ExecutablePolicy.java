package org.ruoyi.service.coding.harness.tool.command;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Authorizes a command name inside the pinned container image; no host lookup is performed. */
final class ExecutablePolicy {

    private static final Set<String> SHELL_INTERPRETERS = Set.of(
        "cmd", "command", "powershell", "pwsh", "sh", "bash", "dash", "zsh",
        "fish", "csh", "ksh", "wsl"
    );

    private final CommandToolConfig config;

    ExecutablePolicy(CommandToolConfig config) {
        this.config = config;
    }

    String authorize(String requested) {
        String executable = CommandValidation.requireText(requested, "executable", 4_096);
        final Path supplied;
        try {
            supplied = Path.of(executable);
        } catch (InvalidPathException error) {
            throw new CommandToolException("INVALID_EXECUTABLE", "Executable name is invalid", error);
        }
        String fileName = supplied.getFileName() == null ? executable
            : supplied.getFileName().toString();
        if (isShell(fileName)) {
            throw new CommandToolException("SHELL_INTERPRETER_DENIED",
                "Shell interpreters are never valid execute_process executables");
        }
        if (supplied.isAbsolute() || supplied.getNameCount() != 1
            || executable.contains("/") || executable.contains("\\")) {
            throw new CommandToolException("EXECUTABLE_NOT_ALLOWLISTED",
                "Executable must be a single allowlisted command name inside the pinned image");
        }
        String normalized = normalizedName(executable);
        boolean allowed = config.executableAllowlist().stream()
            .filter(value -> !value.contains("/") && !value.contains("\\"))
            .map(ExecutablePolicy::normalizedName)
            .anyMatch(normalized::equals);
        if (!allowed) {
            throw new CommandToolException("EXECUTABLE_NOT_ALLOWLISTED",
                "Executable is not authorized by the container command allowlist");
        }
        return executable;
    }

    private static boolean isShell(String name) {
        return SHELL_INTERPRETERS.contains(normalizedName(name));
    }

    private static String normalizedName(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String suffix : List.of(".exe", ".cmd", ".bat", ".com")) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }
}
