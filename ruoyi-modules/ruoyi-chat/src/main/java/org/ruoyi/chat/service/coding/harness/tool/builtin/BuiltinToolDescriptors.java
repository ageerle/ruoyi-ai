package org.ruoyi.chat.service.coding.harness.tool.builtin;

import org.ruoyi.chat.service.coding.harness.tool.ToolCapability;
import org.ruoyi.chat.service.coding.harness.tool.ToolDescriptor;

import java.util.List;
import java.util.Set;

/** Security and execution metadata for the first-party file/search tool set. */
public final class BuiltinToolDescriptors {

    private BuiltinToolDescriptors() {
    }

    public static List<ToolDescriptor> all(RunContext context) {
        BuiltinToolLimits limits = context.limits();
        long ioTimeout = limits.ioTimeout().toMillis();
        long searchTimeout = limits.searchTimeout().toMillis();
        long readOutput = limits.maxReadOutputBytes() * 2 + 64 * 1024;
        long collectionOutput = limits.maxReadOutputBytes() * 4 + 64 * 1024;
        long writeInput = limits.maxWriteBytes() * 6 + 64 * 1024;
        return List.of(
            new ToolDescriptor("read_file", Set.of(ToolCapability.READ), true, ioTimeout,
                64 * 1024, readOutput, true,
                "Reads one bounded file page inside the immutable workspace lease"),
            new ToolDescriptor("read_source", Set.of(ToolCapability.READ), true, ioTimeout,
                64 * 1024, readOutput, true,
                "Reads literal UTF-8 source text without a JSON-encoded content wrapper"),
            new ToolDescriptor("list_files", Set.of(ToolCapability.READ, ToolCapability.SEARCH), true,
                ioTimeout, 16 * 1024, collectionOutput, true,
                "Enumerates a bounded, ignore-aware workspace subtree without following links"),
            new ToolDescriptor("glob_files", Set.of(ToolCapability.SEARCH), true, ioTimeout,
                16 * 1024, collectionOutput, true,
                "Matches a bounded, ignore-aware workspace subtree without following links"),
            new ToolDescriptor("search_text", Set.of(ToolCapability.SEARCH), true, searchTimeout,
                64 * 1024, collectionOutput, true,
                "Searches bounded workspace text through parameterized ripgrep or a Java fallback"),
            new ToolDescriptor("git_status", Set.of(ToolCapability.READ), true, searchTimeout,
                16 * 1024, collectionOutput, true,
                "Reads bounded porcelain status only when the lease root is the repository root"),
            new ToolDescriptor("git_diff", Set.of(ToolCapability.READ), true, searchTimeout,
                16 * 1024, collectionOutput, true,
                "Reads a bounded unified diff with external drivers disabled and no shell invocation"),
            new ToolDescriptor("write_file", Set.of(ToolCapability.WRITE), false, ioTimeout,
                writeInput, 16 * 1024, false,
                "Atomically creates or replaces one lease-bound file; existing files require a SHA precondition"),
            new ToolDescriptor("replace_text", Set.of(ToolCapability.WRITE), false, ioTimeout,
                writeInput, 16 * 1024, false,
                "Atomically replaces exactly one literal match under a SHA precondition")
        );
    }
}
