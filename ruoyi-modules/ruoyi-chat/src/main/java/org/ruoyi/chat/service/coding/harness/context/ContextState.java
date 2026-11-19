package org.ruoyi.chat.service.coding.harness.context;

import org.ruoyi.chat.service.coding.harness.model.HarnessContextCheckpoint;
import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;

import java.util.ArrayList;
import java.util.List;

/** Immutable model working set. Raw messages at or before the checkpoint are ledger-only. */
public record ContextState(
    ContextPins pins,
    List<HarnessMessage> workingMessages,
    HarnessContextCheckpoint checkpoint,
    CompactionControl compactionControl
) {

    public ContextState {
        if (pins == null) {
            throw new IllegalArgumentException("Context pins are required");
        }
        workingMessages = workingMessages == null ? List.of() : List.copyOf(workingMessages);
        checkpoint = checkpoint == null ? HarnessContextCheckpoint.empty() : checkpoint;
        compactionControl = compactionControl == null
            ? CompactionControl.initial() : compactionControl;
        long previous = checkpoint.toSequence();
        for (HarnessMessage message : workingMessages) {
            if (message == null || message.sequence() <= previous) {
                throw new IllegalArgumentException(
                    "Working messages must be ordered after the checkpoint");
            }
            previous = message.sequence();
        }
    }

    public static ContextState create(ContextPins pins, List<HarnessMessage> messages) {
        return new ContextState(pins, messages, HarnessContextCheckpoint.empty(),
            CompactionControl.initial());
    }

    public ContextState append(List<HarnessMessage> messages) {
        List<HarnessMessage> next = new ArrayList<>(workingMessages);
        if (messages != null) {
            next.addAll(messages);
        }
        return new ContextState(pins, next, checkpoint, compactionControl);
    }

    ContextState afterCompaction(List<HarnessMessage> retained,
                                 HarnessContextCheckpoint nextCheckpoint,
                                 CompactionControl nextControl) {
        return new ContextState(pins, retained, nextCheckpoint, nextControl);
    }

    ContextState withControl(CompactionControl nextControl) {
        return new ContextState(pins, workingMessages, checkpoint, nextControl);
    }

    public ContextState resetCompactionCircuit() {
        return withControl(compactionControl.resetCircuit());
    }
}
