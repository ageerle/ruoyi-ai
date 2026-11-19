package org.ruoyi.chat.service.coding.harness.runtime;

/** Executes one queued run. Implementations reload durable state instead of trusting queue payloads. */
@FunctionalInterface
public interface HarnessRunProcessor {
    void process(HarnessRunRequest request);
}
