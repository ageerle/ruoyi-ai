package org.ruoyi.chat.service.coding.harness.loop.tool;

/** A cancelled tool ignored interruption and may still own external side effects. */
public final class ToolBatchCancellationTimeoutException extends RuntimeException {

    public ToolBatchCancellationTimeoutException(String message) {
        super(message);
    }
}
