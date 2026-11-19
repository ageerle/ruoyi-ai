package org.ruoyi.chat.service.coding.harness.tool.command;

/** Bounded process outcome. A non-zero exit code is a normal result, not a tool exception. */
public record ProcessExecutionResult(
    int exitCode,
    boolean timedOut,
    long durationMs,
    String stdout,
    String stderr,
    boolean truncated,
    boolean stdoutTruncated,
    boolean stderrTruncated
) {

    public ProcessExecutionResult {
        if (durationMs < 0 || stdout == null || stderr == null
            || truncated != (stdoutTruncated || stderrTruncated)) {
            throw new IllegalArgumentException("Invalid process execution result");
        }
    }
}
