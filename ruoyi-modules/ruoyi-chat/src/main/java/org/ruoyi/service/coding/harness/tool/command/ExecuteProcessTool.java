package org.ruoyi.service.coding.harness.tool.command;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.service.coding.harness.tool.builtin.RunContext;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Run-bound argv process tool forced through a pinned, networkless Docker OS sandbox.
 *
 * <p>The target executable is a container entrypoint, never a host path. Only the configured
 * absolute Docker CLI starts on the host. The image/toolchain itself remains a trusted premise.</p>
 */
public final class ExecuteProcessTool {

    /** JVM-wide admission boundary; per-run tool instances must not multiply host capacity. */
    private static final Semaphore GLOBAL_PROCESS_SLOTS = new Semaphore(
        CommandToolConfig.DEFAULT.maxConcurrentProcesses(), true);

    private final RunContext context;
    private final CommandToolConfig config;
    private final CommandWorkspaceGuard workspaceGuard;
    private final ExecutablePolicy executablePolicy;
    private final DockerSandboxConfig sandbox;
    private final DockerRuntime dockerRuntime;
    private final DockerSandboxCommandBuilder commandBuilder;
    private final Semaphore processSlots;

    public ExecuteProcessTool(RunContext context) {
        this(context, CommandToolConfig.DEFAULT);
    }

    public ExecuteProcessTool(RunContext context, CommandToolConfig config) {
        this(context, config, new DockerCliRuntime());
    }

    ExecuteProcessTool(RunContext context, CommandToolConfig config,
                       DockerRuntime dockerRuntime) {
        this.context = Objects.requireNonNull(context, "context");
        this.config = Objects.requireNonNull(config, "config");
        this.workspaceGuard = new CommandWorkspaceGuard(context);
        this.executablePolicy = new ExecutablePolicy(config);
        this.sandbox = config.dockerSandbox();
        this.dockerRuntime = Objects.requireNonNull(dockerRuntime, "dockerRuntime");
        sandbox.requireUsableDockerExecutable();
        dockerRuntime.verifyAvailable(sandbox);
        this.commandBuilder = new DockerSandboxCommandBuilder(sandbox, context.leaseRoot());
        if (config.maxConcurrentProcesses() > CommandToolConfig.DEFAULT.maxConcurrentProcesses()) {
            throw new IllegalArgumentException(
                "Per-tool process capacity cannot exceed the JVM-wide sandbox limit");
        }
        this.processSlots = GLOBAL_PROCESS_SLOTS;
    }

    public RunContext context() {
        return context;
    }

    public CommandToolConfig config() {
        return config;
    }

    @Tool(name = "execute_process", value = {
        "Execute one allowlisted container program with a literal argv array inside the immutable workspace lease. "
            + "No shell parses the arguments. A pinned Docker image runs read-only, networkless, "
            + "non-root, resource bounded, and receives only the exact workspace bind mount. "
            + "Maven/npm/pnpm are pinned to a credential-free workspace .harness-deps cache; "
            + "dependency installation must use offline mode and may fail if it was not provisioned. "
            + "The command must terminate: never start a dev/static server, watch mode, or another "
            + "long-lived process, especially as exitCode=0 plan evidence. Inline Node/Python source "
            + "in argv or stdin is forbidden: do not use node -e/-p or python -c/-. Use a finite "
            + "file-based command such as node --check, repository read tools for inspection, or "
            + "run_inline_probe when it is advertised."
    })
    public ProcessExecutionResult executeProcess(
        @P(name = "executable",
            value = "Single allowlisted executable name supplied as the container entrypoint",
            required = true)
        String executable,
        @P(name = "argv", value = "Argument array passed literally; never a shell command string",
            required = true)
        List<String> argv,
        @P(name = "cwd", value = "Optional existing workspace-relative working directory",
            required = false)
        String cwd,
        @P(name = "timeoutMs", value = "Optional timeout bounded by the run command limit",
            required = false)
        Long timeoutMs,
        @P(name = "stdin",
            value = "Optional bounded UTF-8 input for an existing program; inline interpreter source is forbidden",
            required = false)
        String stdin
    ) {
        return executeProcessInternal(executable, argv, cwd, timeoutMs, stdin, false, false);
    }

    ProcessExecutionResult executeInlineProbe(String executable, List<String> argv,
                                               String cwd, Long timeoutMs, String stdin,
                                               boolean workspaceReadOnly) {
        return executeProcessInternal(executable, argv, cwd, timeoutMs, stdin, true,
            workspaceReadOnly);
    }

    private ProcessExecutionResult executeProcessInternal(String executable, List<String> argv,
                                                           String cwd, Long timeoutMs, String stdin,
                                                           boolean inlineProbe,
                                                           boolean workspaceReadOnly) {
        List<String> arguments = CommandValidation.validateArgv(argv, config);
        rejectDuplicatedExecutableArgument(executable, arguments);
        if (!inlineProbe) {
            rejectInlineInterpreterProgram(executable, arguments, stdin);
        }
        String authorizedExecutable = executablePolicy.authorize(executable);
        Path workingDirectory = workspaceGuard.cwd(cwd);
        long timeout = effectiveTimeout(timeoutMs);
        byte[] standardInput = validateStandardInput(stdin);

        boolean acquired = false;
        long started = System.nanoTime();
        long hardDeadline = deadlineAfter(started, timeout);
        long doubledTerminationGrace = config.terminationGraceMs() > Long.MAX_VALUE / 2
            ? Long.MAX_VALUE : config.terminationGraceMs() * 2;
        long cleanupReserveMs = timeout >= 4_000
            ? Math.min(Math.max(3_000, doubledTerminationGrace), timeout / 3)
            : Math.max(1, timeout / 4);
        long terminationDeadline = subtractMillis(hardDeadline, cleanupReserveMs);
        long runnableBudgetMs = Math.max(1, timeout - cleanupReserveMs);
        long terminationReserveMs = Math.min(config.terminationGraceMs(),
            Math.max(1, runnableBudgetMs / 4));
        long processDeadline = subtractMillis(terminationDeadline, terminationReserveMs);
        Throwable executionFailure = null;
        DockerSandboxInvocation invocation = null;
        try {
            if (!processSlots.tryAcquire(remainingUntil(processDeadline,
                "PROCESS_SLOT_TIMEOUT"), TimeUnit.MILLISECONDS)) {
                throw new CommandToolException("PROCESS_SLOT_TIMEOUT",
                    "Process concurrency limit remained saturated until timeout");
            }
            acquired = true;
            // Re-check cwd immediately before constructing the single bind mount.
            workingDirectory = workspaceGuard.cwd(cwd);
            if (System.nanoTime() >= processDeadline) {
                throw new CommandToolException("PROCESS_START_TIMEOUT",
                    "Docker preflight and process-slot wait exhausted the command wall limit");
            }
            invocation = commandBuilder.build(context.runId(), workingDirectory,
                authorizedExecutable, arguments, workspaceReadOnly);
            return executeContainer(invocation, standardInput, started, processDeadline,
                terminationDeadline);
        } catch (RuntimeException | Error error) {
            executionFailure = error;
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            ProcessExecutionInterruptedException interrupted =
                new ProcessExecutionInterruptedException(error);
            executionFailure = interrupted;
            throw interrupted;
        } finally {
            CommandToolException terminalCleanupFailure = null;
            if (invocation != null) {
                try {
                    dockerRuntime.ensureRemoved(sandbox, invocation.containerName(),
                        remainingUntil(hardDeadline, "CONTAINER_CLEANUP_FAILED"));
                } catch (RuntimeException cleanupError) {
                    CommandToolException cleanupFailure = cleanupError instanceof CommandToolException typed
                        ? typed : new CommandToolException("CONTAINER_CLEANUP_FAILED",
                            "Sandbox container cleanup failed", cleanupError);
                    if (executionFailure != null) {
                        executionFailure.addSuppressed(cleanupFailure);
                    } else {
                        terminalCleanupFailure = cleanupFailure;
                    }
                }
            }
            if (acquired) {
                processSlots.release();
            }
            if (terminalCleanupFailure != null) {
                throw terminalCleanupFailure;
            }
        }
    }

    private ProcessExecutionResult executeContainer(DockerSandboxInvocation invocation,
                                                    byte[] standardInput, long started,
                                                    long processDeadline,
                                                    long terminationDeadline)
        throws InterruptedException {
        Process process = null;
        OutputStream processInput = null;
        Thread stdinThread = null;
        AtomicReference<IOException> stdinFailure = new AtomicReference<>();
        Thread stdoutThread = null;
        Thread stderrThread = null;
        BoundedOutputCollector stdout = null;
        BoundedOutputCollector stderr = null;
        try {
            process = dockerRuntime.start(sandbox, invocation,
                remainingUntil(processDeadline, "PROCESS_START_TIMEOUT"));

            stdout = new BoundedOutputCollector(process.getInputStream(),
                config.maxOutputBytesPerStream());
            stderr = new BoundedOutputCollector(process.getErrorStream(),
                config.maxOutputBytesPerStream());
            stdoutThread = collectorThread(stdout, "stdout", process.pid());
            stderrThread = collectorThread(stderr, "stderr", process.pid());
            stdoutThread.start();
            stderrThread.start();
            processInput = process.getOutputStream();
            stdinThread = standardInputThread(processInput, standardInput, stdinFailure,
                process.pid());
            stdinThread.start();

            long remainingTimeout = remainingUntil(processDeadline, "PROCESS_START_TIMEOUT");
            boolean exited = process.waitFor(remainingTimeout, TimeUnit.MILLISECONDS);
            boolean timedOut = !exited;
            if (timedOut) {
                terminateProcessTree(process, terminationDeadline);
            }
            awaitStandardInput(stdinThread, processInput, terminationDeadline);
            awaitCollectors(stdoutThread, stderrThread, stdout, stderr, terminationDeadline);
            if (!timedOut && stdinFailure.get() != null) {
                throw new CommandToolException("STDIN_WRITE_FAILED",
                    "Bounded process stdin could not be delivered", stdinFailure.get());
            }
            if (!timedOut && (stdout.failure() != null || stderr.failure() != null)) {
                IOException failure = stdout.failure() != null ? stdout.failure() : stderr.failure();
                throw new CommandToolException("OUTPUT_CAPTURE_FAILED",
                    "Process output could not be captured", failure);
            }
            int exitCode = exitValue(process);
            long durationMs = elapsedMillis(started);
            boolean truncated = stdout.truncated() || stderr.truncated();
            return new ProcessExecutionResult(exitCode, timedOut, durationMs,
                stdout.content(), stderr.content(), truncated,
                stdout.truncated(), stderr.truncated());
        } catch (InterruptedException error) {
            if (process != null) {
                terminateProcessTreeUninterruptibly(process, terminationDeadline);
            }
            closeAndJoinStandardInputUninterruptibly(stdinThread, processInput,
                terminationDeadline);
            closeAndJoinUninterruptibly(stdoutThread, stderrThread, stdout, stderr,
                terminationDeadline);
            Thread.currentThread().interrupt();
            throw new ProcessExecutionInterruptedException(error);
        } catch (IOException error) {
            if (process != null) {
                terminateProcessTreeUninterruptibly(process, terminationDeadline);
            }
            closeAndJoinStandardInputUninterruptibly(stdinThread, processInput,
                terminationDeadline);
            throw new CommandToolException("PROCESS_START_FAILED",
                "Authorized process could not be started", error);
        } catch (CommandToolException error) {
            if (process != null && process.isAlive()) {
                terminateProcessTreeUninterruptibly(process, terminationDeadline);
            }
            closeAndJoinStandardInputUninterruptibly(stdinThread, processInput,
                terminationDeadline);
            closeAndJoinUninterruptibly(stdoutThread, stderrThread, stdout, stderr,
                terminationDeadline);
            throw error;
        }
    }

    /** Backward-compatible direct Java API; LangChain4j exposes only the annotated overload. */
    public ProcessExecutionResult executeProcess(String executable, List<String> argv,
                                                 String cwd, Long timeoutMs) {
        return executeProcess(executable, argv, cwd, timeoutMs, null);
    }

    private static void rejectDuplicatedExecutableArgument(String executable,
                                                           List<String> arguments) {
        if (executable == null || arguments.isEmpty()) {
            return;
        }
        String executableName = normalizedExecutableName(executable);
        String firstArgumentName = normalizedExecutableName(arguments.get(0));
        if (!executableName.isEmpty() && executableName.equals(firstArgumentName)) {
            throw new CommandToolException("DUPLICATE_EXECUTABLE_ARGUMENT",
                "argv contains only program arguments; remove argv[0]=\""
                    + arguments.get(0) + "\" because executable=\"" + executable
                    + "\" already starts that program");
        }
    }

    private static void rejectInlineInterpreterProgram(String executable,
                                                       List<String> arguments,
                                                       String stdin) {
        String name = normalizedExecutableName(Objects.toString(executable, ""));
        boolean hasStdin = stdin != null && !stdin.isBlank();
        boolean inline = switch (name) {
            case "node" -> arguments.stream().anyMatch(argument -> Set.of(
                    "-e", "--eval", "-p", "--print").contains(argument))
                || hasStdin && (arguments.isEmpty()
                    || arguments.stream().anyMatch(argument ->
                        argument.equals("--input-type")
                            || argument.startsWith("--input-type=")));
            case "python", "python3", "py" -> arguments.stream().anyMatch(argument ->
                    argument.equals("-c") || argument.equals("-"))
                || hasStdin && arguments.isEmpty();
            default -> false;
        };
        if (inline) {
            throw new CommandToolException("INLINE_INTERPRETER_DENIED",
                "execute_process cannot run inline interpreter source; use read_source/read_file "
                    + "for repository inspection or run_inline_probe during VERIFY");
        }
    }

    private static String normalizedExecutableName(String value) {
        String normalized = value.trim().replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        if (separator >= 0) {
            normalized = normalized.substring(separator + 1);
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        for (String suffix : List.of(".exe", ".cmd", ".bat")) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }

    private byte[] validateStandardInput(String stdin) {
        byte[] bytes = stdin == null ? new byte[0] : stdin.getBytes(StandardCharsets.UTF_8);
        long maximum = Math.max(1L, (long) config.maxArgumentChars() * 4L);
        if (bytes.length > maximum) {
            throw new CommandToolException("STDIN_TOO_LARGE",
                "stdin exceeds the configured UTF-8 byte limit of " + maximum);
        }
        if (stdin != null) {
            for (int index = 0; index < stdin.length(); index++) {
                char character = stdin.charAt(index);
                if ((character < 0x20 && character != '\t' && character != '\n'
                    && character != '\r') || character == 0x7f) {
                    throw new CommandToolException("STDIN_CONTROL_CHARACTER_DENIED",
                        "stdin may contain text, tab, carriage return, and newline only");
                }
            }
        }
        return bytes;
    }

    private static Thread standardInputThread(OutputStream output, byte[] bytes,
                                              AtomicReference<IOException> failure, long pid) {
        Thread thread = new Thread(() -> {
            try (output) {
                output.write(bytes);
            } catch (IOException error) {
                failure.set(error);
            }
        }, "harness-execute-process-" + pid + "-stdin");
        thread.setDaemon(true);
        return thread;
    }

    private static void awaitStandardInput(Thread thread, OutputStream output,
                                           long deadlineNanos)
        throws InterruptedException {
        joinUntil(thread, deadlineNanos);
        if (thread.isAlive()) {
            closeQuietly(output);
            joinUntil(thread, deadlineNanos);
        }
        if (thread.isAlive()) {
            throw new CommandToolException("STDIN_WRITE_STUCK",
                "Bounded process stdin writer did not terminate");
        }
    }

    private static void closeAndJoinStandardInputUninterruptibly(Thread thread,
                                                                 OutputStream output,
                                                                 long deadlineNanos) {
        closeQuietly(output);
        if (thread == null) {
            return;
        }
        boolean interrupted = false;
        while (thread.isAlive() && System.nanoTime() < deadlineNanos) {
            try {
                thread.join(50);
            } catch (InterruptedException error) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeQuietly(OutputStream output) {
        if (output == null) {
            return;
        }
        try {
            output.close();
        } catch (IOException ignored) {
            // Process termination and exact container cleanup remain authoritative.
        }
    }

    private long effectiveTimeout(Long requested) {
        if (requested == null) {
            return config.defaultTimeoutMs();
        }
        if (requested <= 0 || requested > config.maxTimeoutMs()) {
            throw new CommandToolException("INVALID_TIMEOUT",
                "timeoutMs must be positive and no greater than the configured maximum");
        }
        return requested;
    }

    private static long deadlineAfter(long started, long timeoutMs) {
        long duration = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        return started > Long.MAX_VALUE - duration ? Long.MAX_VALUE : started + duration;
    }

    private static long subtractMillis(long deadlineNanos, long millis) {
        long duration = TimeUnit.MILLISECONDS.toNanos(Math.max(0, millis));
        return deadlineNanos < Long.MIN_VALUE + duration
            ? Long.MIN_VALUE : deadlineNanos - duration;
    }

    private static long remainingUntil(long deadlineNanos, String failureCode) {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            throw new CommandToolException(failureCode,
                "Process operation exhausted the shared wall deadline");
        }
        return Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining));
    }

    private static Thread collectorThread(BoundedOutputCollector collector, String stream,
                                          long pid) {
        Thread thread = new Thread(collector,
            "harness-execute-process-" + pid + "-" + stream);
        thread.setDaemon(true);
        return thread;
    }

    private static void awaitCollectors(Thread stdoutThread, Thread stderrThread,
                                        BoundedOutputCollector stdout,
                                        BoundedOutputCollector stderr,
                                        long deadlineNanos) throws InterruptedException {
        joinUntil(stdoutThread, deadlineNanos);
        joinUntil(stderrThread, deadlineNanos);
        if (stdoutThread.isAlive() || stderrThread.isAlive()) {
            stdout.close();
            stderr.close();
            joinUntil(stdoutThread, deadlineNanos);
            joinUntil(stderrThread, deadlineNanos);
        }
        if (stdoutThread.isAlive() || stderrThread.isAlive()) {
            throw new CommandToolException("OUTPUT_CAPTURE_STUCK",
                "Process output collectors did not terminate");
        }
    }

    private static void joinUntil(Thread thread, long deadlineNanos) throws InterruptedException {
        while (thread.isAlive()) {
            long remaining = deadlineNanos - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            thread.join(Math.max(1, Math.min(100,
                TimeUnit.NANOSECONDS.toMillis(remaining))));
        }
    }

    private static void terminateProcessTree(Process process, long deadlineNanos)
        throws InterruptedException {
        ProcessHandle parent = process.toHandle();
        List<ProcessHandle> descendants = descendantsDeepestFirst(parent);
        descendants.forEach(ExecuteProcessTool::destroyQuietly);
        waitForHandles(descendants, deadlineNanos);
        List<ProcessHandle> remaining = new ArrayList<>(descendants);
        for (ProcessHandle discovered : descendantsDeepestFirst(parent)) {
            if (remaining.stream().noneMatch(existing -> existing.pid() == discovered.pid())) {
                remaining.add(discovered);
            }
        }
        remaining.forEach(ExecuteProcessTool::destroyForciblyQuietly);
        waitForHandles(remaining, deadlineNanos);
        destroyQuietly(parent);
        waitForHandles(List.of(parent), deadlineNanos);
        destroyForciblyQuietly(parent);
        List<ProcessHandle> all = new ArrayList<>(remaining);
        all.add(parent);
        waitForHandles(all, deadlineNanos);
        if (all.stream().anyMatch(ProcessHandle::isAlive)) {
            throw new CommandToolException("PROCESS_TERMINATION_FAILED",
                "Timed-out process tree could not be fully terminated");
        }
        process.waitFor(remainingUntil(deadlineNanos, "PROCESS_TERMINATION_FAILED"),
            TimeUnit.MILLISECONDS);
    }

    private static List<ProcessHandle> descendantsDeepestFirst(ProcessHandle parent) {
        List<ProcessHandle> descendants = new ArrayList<>(parent.descendants().toList());
        Collections.reverse(descendants);
        return descendants;
    }

    private static void waitForHandles(List<ProcessHandle> handles, long deadlineNanos)
        throws InterruptedException {
        while (handles.stream().anyMatch(ProcessHandle::isAlive)
            && System.nanoTime() < deadlineNanos) {
            long remaining = deadlineNanos - System.nanoTime();
            TimeUnit.NANOSECONDS.sleep(Math.min(TimeUnit.MILLISECONDS.toNanos(10), remaining));
        }
    }

    private static void destroyQuietly(ProcessHandle handle) {
        try {
            if (handle.isAlive()) {
                handle.destroy();
            }
        } catch (RuntimeException ignored) {
            // The forced pass below is authoritative.
        }
    }

    private static void destroyForciblyQuietly(ProcessHandle handle) {
        try {
            if (handle.isAlive()) {
                handle.destroyForcibly();
            }
        } catch (RuntimeException ignored) {
            // Liveness is checked after every forced termination attempt.
        }
    }

    private static void terminateProcessTreeUninterruptibly(Process process,
                                                            long deadlineNanos) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    terminateProcessTree(process, deadlineNanos);
                    return;
                } catch (InterruptedException error) {
                    interrupted = true;
                } catch (CommandToolException error) {
                    descendantsDeepestFirst(process.toHandle())
                        .forEach(ExecuteProcessTool::destroyForciblyQuietly);
                    destroyForciblyQuietly(process.toHandle());
                    return;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void closeAndJoinUninterruptibly(Thread stdoutThread, Thread stderrThread,
                                                     BoundedOutputCollector stdout,
                                                     BoundedOutputCollector stderr,
                                                     long deadlineNanos) {
        if (stdout != null) {
            stdout.close();
        }
        if (stderr != null) {
            stderr.close();
        }
        Thread[] threads = {stdoutThread, stderrThread};
        for (Thread thread : threads) {
            if (thread == null) {
                continue;
            }
            while (thread.isAlive() && System.nanoTime() < deadlineNanos) {
                try {
                    thread.join(25);
                } catch (InterruptedException ignored) {
                    // Original interrupt status is restored by the caller.
                }
            }
        }
    }

    private static int exitValue(Process process) {
        try {
            return process.exitValue();
        } catch (IllegalThreadStateException error) {
            return -1;
        }
    }

    private static long elapsedMillis(long started) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
    }
}
