package org.ruoyi.chat.service.coding.harness.tool.builtin;

import java.time.Duration;

/** Resource ceilings applied to every built-in tool invocation in one run. */
public record BuiltinToolLimits(
    long maxReadFileBytes,
    int maxReadLines,
    long maxReadOutputBytes,
    int maxTraversalDepth,
    int maxListEntries,
    int maxSearchFiles,
    long maxSearchFileBytes,
    int maxSearchResults,
    int maxSearchLineChars,
    long maxWriteBytes,
    Duration ioTimeout,
    Duration searchTimeout
) {

    public static final BuiltinToolLimits DEFAULT = new BuiltinToolLimits(
        4L * 1024 * 1024,
        1_000,
        256L * 1024,
        20,
        2_000,
        10_000,
        2L * 1024 * 1024,
        500,
        1_000,
        4L * 1024 * 1024,
        Duration.ofSeconds(10),
        Duration.ofSeconds(20)
    );

    public BuiltinToolLimits {
        if (maxReadFileBytes <= 0 || maxReadFileBytes >= Integer.MAX_VALUE
            || maxReadLines <= 0 || maxReadOutputBytes <= 0 || maxReadOutputBytes >= Integer.MAX_VALUE
            || maxTraversalDepth <= 0 || maxListEntries <= 0 || maxSearchFiles <= 0
            || maxSearchFileBytes <= 0 || maxSearchFileBytes >= Integer.MAX_VALUE
            || maxSearchResults <= 0 || maxSearchLineChars <= 0
            || maxWriteBytes <= 0 || maxWriteBytes >= Integer.MAX_VALUE
            || ioTimeout == null || ioTimeout.isZero() || ioTimeout.isNegative()
            || searchTimeout == null || searchTimeout.isZero() || searchTimeout.isNegative()) {
            throw new IllegalArgumentException("Built-in tool limits must be positive");
        }
    }
}
