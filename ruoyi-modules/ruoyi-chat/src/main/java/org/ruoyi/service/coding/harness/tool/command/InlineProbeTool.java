package org.ruoyi.service.coding.harness.tool.command;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Small verification adapter that sends a script over stdin instead of forcing a model to encode
 * a quote-heavy program inside an argv JSON string. It deliberately delegates every workspace,
 * executable, environment, timeout, output, cancellation, and process-tree control to the same
 * {@link ExecuteProcessTool}; this is an ergonomic surface, not additional authority.
 */
public final class InlineProbeTool {

    private final ExecuteProcessTool processTool;
    private final boolean readOnly;

    public InlineProbeTool(ExecuteProcessTool processTool) {
        this(processTool, false);
    }

    /**
     * @param readOnly when true, Node runs under its permission model with workspace reads only;
     *                 Python is rejected because this adapter cannot enforce an equivalent
     *                 filesystem boundary for it.
     */
    public InlineProbeTool(ExecuteProcessTool processTool, boolean readOnly) {
        this.processTool = Objects.requireNonNull(processTool, "processTool");
        this.readOnly = readOnly;
    }

    @Tool(name = "run_inline_probe", value = {
        "Run one bounded assertion script from UTF-8 stdin inside the workspace lease. Use this for "
            + "multi-line falsification probes instead of placing source in argv or creating a test file. "
            + "The script must terminate, assert/throw or exit non-zero on failure, and explicitly release "
            + "every deferred promise/latch after observing the intended interleaving. Supported runtimes "
            + "are node and python. Node executes as an ES module: use import, never require. Never call "
            + "process.exit(); await server.close and every other handle, then let the event loop finish. "
            + "Raw Node has no browser document/window, so do not eval browser-only scripts. Test the "
            + "actual repository artifact instead of copying its algorithm into the probe. The working "
            + "directory is the workspace root: import repository modules "
            + "with ./relative/path, never ../ or a raw Windows drive path. This delegates to execute_process "
            + "In read-only VERIFY, Node child_process and filesystem writes are intentionally denied. "
            + "Do not try to start a web server or connect to a fixed localhost port: it may belong to an "
            + "unrelated process and create false counterevidence. Runtime server behavior must come from "
            + "the approved BUILD test evidence; use this probe for pure read-only assertions over current "
            + "source or imported side-effect-free modules. "
            + "and has exactly the same approval, "
            + "allowlist, cancellation, timeout, and Docker sandbox boundary."
    })
    public ProcessExecutionResult runInlineProbe(
        @P(name = "runtime", value = "Exact runtime: node or python", required = true)
        String runtime,
        @P(name = "script",
            value = "Complete bounded UTF-8 assertion program sent literally on stdin", required = true)
        String script,
        @P(name = "timeoutMs", value = "Optional bounded timeout", required = false)
        Long timeoutMs
    ) {
        if (runtime == null || script == null || script.isBlank()) {
            throw new CommandToolException("INVALID_INLINE_PROBE",
                "runtime and a non-blank script are required");
        }
        return switch (runtime.toLowerCase(Locale.ROOT)) {
            case "node" -> processTool.executeInlineProbe("node", readOnly
                    ? List.of("--permission", "--allow-fs-read=.", "--input-type=module")
                    : List.of("--input-type=module"),
                ".", timeoutMs, script, readOnly);
            case "python" -> {
                if (readOnly) {
                    throw new CommandToolException("READ_ONLY_PROBE_RUNTIME",
                        "VERIFY permits only the Node read-only inline probe");
                }
                yield processTool.executeInlineProbe("python", List.of("-"), ".", timeoutMs,
                    script, false);
            }
            default -> throw new CommandToolException("INVALID_INLINE_PROBE_RUNTIME",
                "runtime must be exactly node or python");
        };
    }
}
