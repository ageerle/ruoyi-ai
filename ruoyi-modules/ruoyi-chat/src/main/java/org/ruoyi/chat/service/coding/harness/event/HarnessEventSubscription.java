package org.ruoyi.chat.service.coding.harness.event;

/** Handle owned by a transport. Closing it never changes the run lifecycle. */
@FunctionalInterface
public interface HarnessEventSubscription extends AutoCloseable {
    @Override
    void close();
}
