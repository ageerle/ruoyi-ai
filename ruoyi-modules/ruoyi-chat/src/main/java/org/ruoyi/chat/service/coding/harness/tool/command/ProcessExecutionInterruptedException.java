package org.ruoyi.chat.service.coding.harness.tool.command;

/** The calling worker was interrupted after its complete child process tree was terminated. */
public class ProcessExecutionInterruptedException extends CommandToolException {

    public ProcessExecutionInterruptedException(InterruptedException cause) {
        super("PROCESS_INTERRUPTED", "Process execution was interrupted and terminated", cause);
    }
}
