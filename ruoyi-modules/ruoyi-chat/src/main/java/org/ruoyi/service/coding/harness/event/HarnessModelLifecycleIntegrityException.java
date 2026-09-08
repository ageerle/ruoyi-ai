package org.ruoyi.service.coding.harness.event;

/** Indicates durable lifecycle evidence is logically corrupt rather than temporarily unavailable. */
public final class HarnessModelLifecycleIntegrityException extends IllegalStateException {

    public HarnessModelLifecycleIntegrityException(String message) {
        super(message);
    }
}
