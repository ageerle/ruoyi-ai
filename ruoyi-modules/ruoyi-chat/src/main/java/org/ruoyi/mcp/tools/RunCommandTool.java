package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 受控命令执行工具
 * 仅允许执行白名单命令，禁止高风险和破坏性命令
 */
@Component
public class RunCommandTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Runs a safe whitelisted command in workspace. " +
        "Only supports non-interactive commands for build/test/git status workflows. " +
        "Blocks destructive and shell-chaining commands. " +
        "Use absolute working directory inside workspace. " +
        "Requires an approval token from task_planner before execution.";

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int MAX_TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_CHARS = 20_000;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "mvn", "./mvnw", "npm", "pnpm", "yarn", "go", "gradle", "./gradlew", "git"
    );

    private static final Map<String, Set<String>> ALLOWED_SUBCOMMANDS = Map.of(
        "git", Set.of("status", "diff", "log", "branch", "checkout", "switch", "add", "commit", "restore"),
        "mvn", Set.of("compile", "test", "package", "verify"),
        "./mvnw", Set.of("compile", "test", "package", "verify"),
        "npm", Set.of("run", "test", "install", "ci"),
        "pnpm", Set.of("run", "test", "install"),
        "yarn", Set.of("run", "test", "install"),
        "go", Set.of("test", "build"),
        "gradle", Set.of("test", "build"),
        "./gradlew", Set.of("test", "build")
    );

    private static final Set<String> BLOCKED_EXACT_TOKENS = Set.of(
        "rm", "rmdir", "unlink", "shutdown", "reboot", "poweroff", "sudo", "su", "mkfs", "fdisk",
        "mount", "umount", "iptables", "nft", "useradd", "userdel"
    );

    private static final List<String> BLOCKED_PHRASES = List.of(
        "git reset --hard", "git clean -f", "git clean -fd", "git clean -xdf"
    );

    private final String rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public RunCommandTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
    }

    @Tool(DESCRIPTION)
    public String runCommand(String command, String workingDirectory, Integer timeoutSeconds, String approvalToken,
                             String approvalScope) {
        try {
            if (!ApprovalTokenStore.consume(approvalToken, approvalScope)) {
                return "Error: Invalid or expired approval token. Please generate a new plan token via task_planner and confirm before execution.";
            }

            if (command == null || command.trim().isEmpty()) {
                return "Error: Command cannot be empty";
            }
            String normalized = command.trim();

            if (containsShellChaining(normalized)) {
                return "Error: Shell chaining operators are not allowed";
            }

            String lower = normalized.toLowerCase();
            for (String blocked : BLOCKED_PHRASES) {
                if (lower.contains(blocked)) {
                    return "Error: Command contains blocked token: " + blocked;
                }
            }

            List<String> parts = List.of(normalized.split("\\s+"));
            List<String> lowerParts = parts.stream().map(String::toLowerCase).collect(Collectors.toList());
            for (String token : lowerParts) {
                if (BLOCKED_EXACT_TOKENS.contains(token)) {
                    return "Error: Command contains blocked token: " + token;
                }
            }

            String binary = parts.get(0);
            if (!ALLOWED_COMMANDS.contains(binary)) {
                return "Error: Command is not in allowed list: " + binary;
            }
            String subcommandError = validateSubcommand(binary, parts);
            if (subcommandError != null) {
                return "Error: " + subcommandError;
            }

            Path workdir = resolveWorkdir(workingDirectory);
            if (!Files.exists(workdir) || !Files.isDirectory(workdir)) {
                return "Error: Working directory not found: " + workdir;
            }

            int timeout = timeoutSeconds == null ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
            if (timeout < 1 || timeout > MAX_TIMEOUT_SECONDS) {
                return "Error: timeoutSeconds must be between 1 and " + MAX_TIMEOUT_SECONDS;
            }

            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.directory(workdir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> outputFuture = executor.submit(() -> readProcessOutput(process, MAX_OUTPUT_CHARS));

            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                executor.shutdownNow();
                return "Error: Command timeout after " + timeout + " seconds";
            }

            String output = getOutputSafely(outputFuture, executor);

            int code = process.exitValue();
            String relativeWd = getRelativePath(workdir);
            String summary = "Command: " + normalized + "\nWorkingDirectory: " + relativeWd + "\nExitCode: " + code + "\n\n";
            return summary + output;
        } catch (IOException e) {
            logger.error("Error running command: {}", command, e);
            return "Error: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Command execution interrupted";
        } catch (Exception e) {
            logger.error("Unexpected error running command: {}", command, e);
            return "Error: Unexpected error: " + e.getMessage();
        }
    }

    private Path resolveWorkdir(String workingDirectory) throws IOException {
        Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
        if (workingDirectory == null || workingDirectory.isBlank()) {
            return workspaceRoot;
        }

        Path path = Paths.get(workingDirectory);
        Path resolved;
        if (!path.isAbsolute()) {
            resolved = workspaceRoot.resolve(path).normalize();
        } else {
            resolved = path.normalize();
        }

        if (!resolved.startsWith(workspaceRoot)) {
            throw new IOException("Working directory must be within workspace: " + workingDirectory);
        }
        return resolved;
    }

    private boolean containsShellChaining(String command) {
        return command.contains("&&") || command.contains("||") || command.contains(";") || command.contains("|");
    }

    private String validateSubcommand(String binary, List<String> parts) {
        Set<String> allowedSubs = ALLOWED_SUBCOMMANDS.get(binary);
        if (allowedSubs == null) {
            return null;
        }

        if (parts.size() < 2) {
            return "Missing subcommand for " + binary;
        }

        String sub = parts.get(1);
        if (sub.startsWith("-")) {
            return "Flag-only command is not allowed for " + binary;
        }

        if (!allowedSubs.contains(sub)) {
            return "Subcommand is not allowed for " + binary + ": " + sub;
        }

        if (("npm".equals(binary) || "pnpm".equals(binary) || "yarn".equals(binary))
            && "install".equals(sub) && !parts.contains("-g") && !parts.contains("--global")) {
            return "Package install must be global (-g/--global)";
        }

        return null;
    }

    private String readProcessOutput(Process process, int maxChars) throws IOException {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() + line.length() + 1 <= maxChars) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }

    private String getOutputSafely(Future<String> outputFuture, ExecutorService executor)
        throws InterruptedException, ExecutionException {
        try {
            return outputFuture.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return "";
        } finally {
            executor.shutdown();
            executor.awaitTermination(Duration.ofSeconds(2).toSeconds(), TimeUnit.SECONDS);
        }
    }

    private String getRelativePath(Path filePath) {
        try {
            Path workspaceRoot = Paths.get(rootDirectory).toRealPath();
            return workspaceRoot.relativize(filePath.toRealPath()).toString();
        } catch (Exception e) {
            return filePath.toString();
        }
    }

    @Override
    public String getToolName() {
        return "run_command";
    }

    @Override
    public String getDisplayName() {
        return "执行命令";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
